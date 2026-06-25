# To build, run the following command from the top level project directory:
#
# docker build -t osirixfoundation/karnak:latest -f src/main/docker/Dockerfile .
#
# For a local multi-arch build:
# docker buildx build --platform linux/amd64,linux/arm64 -f src/main/docker/Dockerfile .

# Unpacking the Spring Boot layered jar is architecture-independent, so this stage is
# pinned to the build host's platform: it runs once, natively, even for multi-arch
# builds (no emulation), and its output is shared across every target platform.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk-noble AS builder
ARG JAR_FILE=target/karnak*.jar
WORKDIR /app
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# Final deployment image, built per target architecture. Layers are copied least- to
# most-frequently changing so Docker can cache the dependency layers across builds.
# No RUN step executes here, so cross-building needs no QEMU emulation.
FROM eclipse-temurin:25-jdk-noble
WORKDIR /app

COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./
COPY tools/docker-entrypoint.sh .

# Run as a non-root uid. Everything under /app is world-readable and the entrypoint is
# world-executable; the app writes only to /tmp at runtime, so no writable app dir is needed.
USER 1001:1001

EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]