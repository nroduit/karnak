# Gateway forwarding integration tests

End-to-end tests for the DICOM forwarding path (`ForwardService` + the connection pool in
`DicomForwardDestination`). Each test starts a **real C-STORE SCP** (a `DicomListener` on a
loopback port), forwards objects through `ForwardService`, and reads back what the SCP actually
stored — so associations, transfer-syntax negotiation, the lease/return pool, fan-out and the
image-processing pipeline are exercised over the wire, not mocked.

They live under `src/test/java/org/karnak/backend/service/` and build on a shared base class,
`GatewayItTestSupport`.

## Running

```bash
# Functional integration tests (fast) — runs by default
mvn test

# Just the gateway functional set
mvn test -Dtest='ForwardServiceIntegrationTest,FanOutIntegrationTest,\
TransferSyntaxNegotiationIntegrationTest,ImageProcessingForwardIntegrationTest'

# Performance / saturation suite (skipped by default, see Tagging)
mvn test -Pperformance

# A single performance class
mvn test -Pperformance -Dtest=GatewaySaturationTest
```

> **Native library.** Tests touch the DICOM stack and (for image processing) the native OpenCV
> codec. The Maven build wires `-Djava.library.path` to the bundled `libopencv_java`
> automatically. Running from the IDE requires setting it manually — see the IntelliJ note in
> `CLAUDE.md`. The image-processing tests **hard-fail** (not skip) if the library is missing.

## Tagging

JUnit 5 tags select what runs:

| Tag | Meaning | Default `mvn test` |
| --- | --- | --- |
| `integration` | end-to-end over real associations | runs |
| `image-processing` | also drives the native OpenCV codec path | runs |
| `performance` | throughput / saturation characterisation, slower | **skipped** |

Wiring (in `pom.xml`): surefire reads `${karnak.test.groups}` / `${karnak.test.excludedGroups}`;
the default excludes `performance`, and the `performance` profile flips them on.

```xml
<!-- properties -->
<karnak.test.excludedGroups>performance</karnak.test.excludedGroups>

<!-- profile: mvn test -Pperformance -->
<profile>
  <id>performance</id>
  <properties>
    <karnak.test.groups>performance</karnak.test.groups>
    <karnak.test.excludedGroups/>
  </properties>
</profile>
```

## The test classes

| Class | Tags | What it proves |
| --- | --- | --- |
| `GatewayItTestSupport` | — | shared harness (start SCP, build objects, read back UIDs / transfer syntax / pixels) |
| `ForwardServiceIntegrationTest` | `integration` | sequential & concurrent delivery, pool contention (senders > slots), no lease leaks |
| `FanOutIntegrationTest` | `integration` | one object → many destinations (parallel & sequential); a dead destination doesn't stop the live ones |
| `TransferSyntaxNegotiationIntegrationTest` | `integration` | multiple input transfer syntaxes and several SOP classes negotiate over a pooled association |
| `ImageProcessingForwardIntegrationTest` | `integration`, `image-processing` | masking, transcoding to a compressed syntax, and defacing actually modify the stored object |
| `GatewayThroughputTest` | `performance` | throughput scales with pool size; fan-out degrades gracefully under queue saturation |
| `GatewaySaturationTest` | `performance` | scale-up knee and sustained-overload behaviour (below) |
| `GatewayScaleMatrixTest` | `performance` | sweeps pool size, source count (fan-in) and destination count (fan-out) independently (below) |

### The harness (`GatewayItTestSupport`)

Key helpers a new test reuses:

- `startScp()` — a real Store SCP on a free port; auto-stopped after the test.
- `serialize(iuid[, cuid, tsuid])` — a minimal object as the SCP delivers it (raw dataset).
- `dicomDestination(fwdNode, scp[, poolSize])` — a pooled DICOM destination.
- `receivedSopInstanceUids(dir)` / `receivedTransferSyntaxes(dir)` — read back what was stored.

The image-processing test adds pixel-bearing fixtures and drives transforms through a custom
`AttributeEditor` that sets `context.setMaskArea(...)` or the `Defacer.APPLY_DEFACING` property —
the same hooks the production de-identification profile uses.

> **Note on the defacing fixture.** `ImageProcessingForwardIntegrationTest` currently defaces a
> **synthetic** axial CT (a soft-tissue disc over an air background). It proves the defacing path
> runs end-to-end and changes the stored pixels, but not face-detection quality. Drop a
> de-identified real axial CT into `src/test/resources` to strengthen that case.

## Saturation: the pattern and how the gateway reacts

`GatewaySaturationTest` answers “what happens when Karnak is pushed past capacity?”. Before the
numbers, here are the concepts and the components they map to.

### Concepts and components

- **Source** — a sender of DICOM objects. In production it is a modality / PACS that opens a
  C-STORE association to Karnak; Karnak knows it as a `ForwardDicomNode` (authenticated by AE
  title / hostname). In these tests a *source* is a thread (or group of threads) calling
  `ForwardService.storeMultipleDestination(...)` — it plays the role of the upstream sender.
- **Destination** — where a study is forwarded to: a real PACS (DICOM C-STORE SCP) or a DICOMWeb
  endpoint. Modelled by `DicomForwardDestination` (DICOM) and driven against a real
  `DicomListener` SCP in the tests.
- **Connection pool** — each destination holds a fixed set of **`poolSize` DICOM associations**
  (`DicomForwardDestination`, leased with `acquire()` / `release()`). One association carries one
  C-STORE at a time, so the pool is the destination's parallelism: at most `poolSize` objects are
  in flight to it concurrently. When all are leased, extra transfers **share** an association
  round-robin rather than opening new ones — so connection count is bounded and the overload turns
  into latency, not resource growth.
- **Offered concurrency** — how many sends are attempted at once (the number of busy source
  threads). It is the *load*; the pool is the *capacity*. Saturation is what happens when offered
  concurrency exceeds the pool's capacity.
- **Fan-out** — one incoming object forwarded to several destinations at once. Karnak runs the
  per-destination work on a bounded thread pool (`ForwardService` fan-out executor) with a
  `CallerRunsPolicy`: under overload the submitting thread runs the task itself, degrading to
  sequential delivery instead of growing threads/memory without bound.
- **Fan-in** — several independent sources forwarding to the same destination(s) at once. The
  shared destination (its pool, and the PACS behind it) is the contended resource.
- **Throughput** — objects delivered per second (the gateway's output rate).
- **Latency** — wall-clock time of a single send, measured by the source. Because forwarding is
  synchronous from the source's point of view, **the latency a source observes is the
  back-pressure Karnak applies upstream**: a saturated gateway slows its sources down instead of
  dropping objects or buffering without limit.
- **Saturation knee** — the load level where throughput stops rising (the pool/destination is
  fully busy) and additional load only increases latency. Below the knee you are capacity-bound;
  above it you are back-pressure-bound.

### Pattern

The gateway forwards over a **fixed connection pool** (`DicomForwardDestination`, `poolSize`
associations). The test stands in for the C-STORE **source** with a thread pool of *offered
concurrency* senders, and times every individual send. Because the source is what waits on a busy
gateway, **the latency a sender observes is the back-pressure the gateway applies upstream**. The
ramp increases offered concurrency against a fixed pool and records throughput + latency
percentiles at each level.

```
offered senders (the source)        gateway                 destination SCP
  ─ concurrency 1..64 ─▶   ForwardService + pool(N)   ─▶   real C-STORE SCP
        ▲ latency = back-pressure        │
        └──────── rises once N busy ─────┘
```

Invariants asserted at every level: **every instance is delivered (no drops)** and the run
**completes within a timeout (no deadlock)**. The throughput/latency numbers are *logged for
inspection*, not asserted, so the suite doesn't get flaky on shared CI hardware.

### How it reacts (representative run, `poolSize = 4`, 480 instances/level)

| offered conc | throughput/s | p50 ms | p95 ms | max ms |
| ---: | ---: | ---: | ---: | ---: |
| 1 | 3584 | 0 | 0 | 2 |
| 2 | 5178 | 0 | 1 | 2 |
| 4 | 6835 | 0 | 1 | 2 |
| 8 | 7219 | 1 | 3 | 5 |
| 16 | 8354 | 2 | 4 | 7 |
| 32 | 8226 | 3 | 9 | 21 |
| 64 | 7310 | 4 | 23 | 41 |

Reading the curve:

- **Below capacity (conc 1–4 ≤ pool):** throughput climbs almost linearly, latency stays ~0 ms —
  every send gets its own free association.
- **At the knee (conc ~16–32):** throughput **plateaus** around the pool's ceiling. Extra load no
  longer buys throughput; it starts buying latency (p95 4 → 9 ms).
- **Past the knee (conc 64):** throughput stops improving and slightly **regresses** (contention
  overhead), while latency climbs sharply (p95 23 ms, max 41 ms). This rising latency is the
  gateway **back-pressuring the source** rather than dropping or failing work.

Absolute numbers depend on hardware (loopback here); the **shape** — climb → plateau → latency
grows while throughput holds — is the point.

### Sustained overload against a slow destination

`sustained_overload_against_a_slow_destination_absorbs_the_burst_without_loss` fixes high
concurrency and makes the **destination** the bottleneck (an editor sleeps per send). A
representative run (`poolSize = 4`, concurrency 32):

| level | throughput/s | p50 ms | p95 ms | max ms |
| --- | ---: | ---: | ---: | ---: |
| slow-destination | 2244 | 9 | 38 | 42 |

Throughput is capped at the destination's drain rate (~2244/s vs ~8000/s healthy), latency rises
(the absorbed burst), and **all instances are still delivered**. The gateway absorbs the burst and
slows down, it does not shed load.

### Scale matrix: pool size × sources × destinations

`GatewaySaturationTest` varies one knob (offered concurrency) against a fixed shape.
`GatewayScaleMatrixTest` sweeps the three **structural** dimensions of a deployment, one at a
time, so you can see what each buys (and costs). Each *source* is modelled the way Karnak
configures it: its own `ForwardDicomNode` with its own pooled destinations, all converging on the
shared destination SCPs (true fan-in). `objects = sends × destinations`; latency is per send
(source-observed). Representative runs (loopback):

**Pool size** (1 source, 1 destination, concurrency 16) — more associations = more parallel
in-flight transfers, until the destination itself becomes the bottleneck:

| pool | objs/s | p50 | p95 | max |
| ---: | ---: | ---: | ---: | ---: |
| 1 | 2285 | 6 | 12 | 34 |
| 2 | 5353 | 3 | 7 | 13 |
| 4 | 6570 | 2 | 5 | 14 |
| 8 | 8588 | 1 | 5 | 18 |
| 16 | 7406 | 1 | 4 | 12 |

Throughput climbs steeply 1→8 and **flattens (slightly regresses) by 16** — past the point where
the single destination can absorb more, extra associations only add coordination overhead. The
biggest single win is moving off `poolSize = 1`.

**Sources / fan-in** (pool 4, 1 destination, 8 threads each) — independent senders piling onto a
shared destination:

| sources | conc | objs/s | p50 | p95 | max |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 8 | 7376 | 1 | 2 | 5 |
| 2 | 16 | 7664 | 1 | 6 | 14 |
| 4 | 32 | 7791 | 3 | 10 | 14 |
| 8 | 64 | 8696 | 5 | 17 | 28 |

Aggregate throughput is roughly **flat** (the shared destination is the ceiling), while latency
climbs with each added source. Fan-in saturates at the destination and the back-pressure is shared
out across sources — none is starved, nothing is dropped.

**Destinations / fan-out** (pool 4, 2 sources, 8 threads each) — each send fanned out to N
destinations:

| destinations | objects | objs/s | p50 | p95 | max |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 300 | 7664 | 1 | 6 | 14 |
| 2 | 600 | 8344 | 3 | 7 | 10 |
| 4 | 1200 | 9381 | 6 | 11 | 52 |
| 8 | 2400 | 8857 | 12 | 30 | 64 |

Total **object** throughput rises with fan-out (parallel delivery across destinations), but
**per-send latency grows** because a send only completes once every destination has it — and at
8 destinations the fan-out executor's bounded queue starts applying back-pressure (p95 30 ms).
Fan-out multiplies output at the cost of per-object latency.

Takeaways: raise `poolSize` first (cheapest throughput up to the destination's ceiling); a shared
destination caps fan-in throughput regardless of how many sources push; fan-out scales total
output but lengthens each send. In every cell, zero instances are dropped.

### Why it reacts this way (mechanisms)

- **Bounded pool, no unbounded growth.** A destination keeps exactly `poolSize` associations
  (`DicomForwardDestination.acquire()/release()`). When all are leased, extra transfers share an
  association round-robin instead of opening new ones — so resource use is bounded and latency,
  not connection count, absorbs the overload.
- **Bounded fan-out queue + caller-runs.** Multi-destination fan-out uses a `ThreadPoolExecutor`
  with a bounded queue and `CallerRunsPolicy` (`ForwardService.initFanoutExecutor`). Under
  overload the submitting thread runs the task itself, degrading to sequential delivery rather
  than growing threads/memory without limit. `GatewayThroughputTest`’s
  `fan_out_does_not_drop_instances_under_queue_saturation` exercises this directly.
- **Back-pressure to the source.** Because forwarding is synchronous from the source's point of
  view, a saturated gateway slows the C-STORE source down (higher latency) instead of buffering
  unboundedly or dropping objects.

## Adding a test

1. Extend `GatewayItTestSupport`.
2. `startScp()` for each destination PACS you need.
3. Build objects with `serialize(...)` (or the pixel-bearing fixtures for image processing).
4. Forward via `forwardService.storeMultipleDestination(fwdNode, destinations, params(...))`.
5. Assert with `receivedSopInstanceUids(...)` / `receivedTransferSyntaxes(...)` / pixel read-back.
6. Tag it: `@Tag("integration")`, add `@Tag("image-processing")` if it uses the codec, or
   `@Tag("performance")` for load tests (and log numbers rather than asserting them).