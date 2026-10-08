# Security Policy

Karnak is a DICOM gateway used to de-identify and forward medical-imaging data.
Because it handles sensitive health information (PHI/PII) and runs on hospital
networks, we take security issues seriously and appreciate responsible
disclosure.

## Supported Versions

Security fixes are provided for the latest released minor version. We recommend
always running the most recent release.

| Version | Supported          |
| ------- | ------------------ |
| 1.2.x   | :white_check_mark: |
| < 1.2   | :x:                |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues,
discussions, or pull requests.**

Instead, report them privately using one of the following channels:

- **Preferred:** Open a [private security advisory](https://github.com/nroduit/karnak/security/advisories/new)
  via GitHub's "Report a vulnerability" feature.
- Alternatively, email the maintainers at **dicom@hcuge.ch**.

Please include as much of the following as you can to help us triage quickly:

- The type of issue (e.g. authentication bypass, injection, exposure of PHI,
  insecure de-identification, SSRF, etc.).
- The affected component(s) and version (DICOM listener, DICOMWeb/STOW-RS,
  Vaadin UI, REST API, profile/de-identification pipeline, etc.).
- Step-by-step instructions to reproduce the issue.
- Proof-of-concept or exploit code, if available.
- The impact, including how an attacker might exploit it.

**Do not include real patient data** in your report. Use synthetic or fully
anonymized DICOM data only.

## Disclosure Process

- We will acknowledge receipt of your report within **5 business days**.
- We will investigate and provide an initial assessment within **10 business
  days**, and keep you informed of progress toward a fix.
- Once a fix is available, we will coordinate a release and a public advisory.
  We are happy to credit you in the advisory unless you prefer to remain
  anonymous.

We ask that you give us a reasonable amount of time to address the issue before
any public disclosure.

## Scope

This policy covers the Karnak application and its source code in this
repository. Vulnerabilities in third-party dependencies should be reported to
the respective upstream projects; if a dependency issue affects Karnak, feel
free to let us know so we can update.

Thank you for helping keep Karnak and its users safe.
