# AegisGate: Autonomous Zero-Touch Promotion Engine

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.11+-blue.svg)](https://www.python.org/)
[![Strands Decider 2B](https://img.shields.io/badge/Model-Strands%20Decider%202B-purple.svg)](https://strandsagents.com)
[![Amazon Bedrock](https://img.shields.io/badge/AWS-Amazon%20Bedrock-232F3E.svg)](https://aws.amazon.com/bedrock/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> **AWS Builder Center Hackathon Entry**: *Agent Experience Hackathon: Strands Decider*  
> **Tags**: `#strands-decider-2b`, `#policy`, `#guardrails`

AegisGate is an enterprise-grade autonomous promotion gate engine designed to eliminate manual deploy approval bottlenecks in CI/CD pipelines ($\text{Dev} \rightarrow \text{UAT} \rightarrow \text{Prod}$). 

By combining the ultra-low latency ($\approx 115\text{ms}$) deterministic decision-making of **Strands Decider 2B** with the deep risk analysis of **Amazon Bedrock (Anthropic Claude 3.5 Sonnet)**, AegisGate achieves:
- **Zero-Touch Fast-Path:** Releases with high statistical confidence are auto-promoted in $< 150\text{ms}$.
- **Smart-Fallback:** Ambiguities or policy violations automatically trigger Claude 3.5 Sonnet to produce a concise 3-bullet technical remediation summary on Slack and GitHub PRs.
- **Fail-Closed Safety:** Infrastructure or network issues immediately default to a safe blocked state.
- **90% Token & Cost Reduction:** Routine builds bypass expensive frontier LLM calls.

---

## Architecture Overview

```
[CI/CD Event: Promotion Request]
           │
           ▼
┌──────────────────────────────────────────────┐
│       AegisGate Engine (Java / Spring)       │
│  Aggregates: JUnit, Trivy CVEs, Git Diff     │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────┐
│  Strands Decider 2B (Local Engine / Sidecar) │  <-- ~115ms Latency
│  Executes deterministic policy decision      │
└──────────────────────┬───────────────────────┘
                       │
        ┌───────────────┴───────────────┐
        ▼                               ▼
[GO & Confidence >= 0.93]       [NO_GO or Confidence < 0.93]
        │                               │
        │ Fast-Path                     │ Fallback
        │                               ▼
        │               ┌───────────────────────────────┐
        │               │ Amazon Bedrock (Claude 3.5)   │
        │               │ Generates risk analysis report│
        │               └───────────────┬───────────────┘
        │                               │
        ▼                               ▼
[HTTP 200: Auto-Approved]       [HTTP 422: Block & Alert Slack/PR]
```

---

## Evidence-Based Automated Acceptance in CI/CD

When a developer opens a Merge Request (e.g. `feature/JIRA-XXXX` &rarr; `develop`), traditional pipelines either require tedious manual approvals or rely on blind boolean rules.

AegisGate acts as the final autonomous quality gate evaluating **holistic proofs**:
1. Preliminary jobs run (Unit Tests, SonarQube, Trivy CVE Scanner, AI Code Review).
2. AegisGate aggregates all evidence snapshots and queries **Strands Decider 2B** (`#policy`).
3. **If all proofs pass ($\tau \ge 0.93$):** Returns `HTTP 200`, automatically approving/merging the MR (Zero-Touch).
4. **If any risk or ambiguity exists:** Returns `HTTP 422`, blocks the pipeline and prompts **Amazon Bedrock (Claude 3.5 Sonnet)** to generate a 3-bullet technical breakdown posted directly on the GitLab MR / GitHub PR discussion and Slack.

### Pipeline Integrations Included
- **GitLab CI/CD:** Complete ready-to-use template in [`.gitlab-ci.example.yml`](.gitlab-ci.example.yml) with automatic MR notes.
- **GitHub Actions:** Complete workflow in [`.github/workflows/aegisgate-gate.example.yml`](.github/workflows/aegisgate-gate.example.yml) with PR comments.

---

## Interactive Visual Dashboard

AegisGate includes a real-time interactive simulator and dashboard accessible at **`http://localhost:8080/`** (or via the static artifact):
- **Live Pipeline Simulator:** Test Fast-Path (115ms), Security Block (CVE-2026), and Ambiguity Fallback with interactive glowing status flows.
- **High-Contrast Theme Switcher:** Instant toggle between Dark Mode and Light Mode.
- **Real-Time KPI Cards:** Latency, 90% cost reduction, and Fail-Closed status.

---

## Repository Structure

```
aegisgate/
├── pom.xml                                      # Root aggregator Maven POM
├── aegis-policy-intervention/                   # Reusable Asset 1 (Python / Strands)
│   ├── aegis_intervention_handler.py            # Reusable Strands Decision Handler
│   ├── sidecar_server.py                        # FastAPI /decide and /health endpoint
│   ├── tests/test_handler.py                    # Python unit tests
│   └── README.md
├── aegis-gatekeeper-spring-boot-starter/         # Reusable Asset 2 (Java / Spring Boot Starter)
│   ├── pom.xml                                  # Turnkey starter module
│   ├── src/main/java/com/aegisgate/
│   │   ├── model/DecisionContracts.java         # Immutable Java Records
│   │   ├── service/AegisGatekeeperService.java  # Fast-Path & Bedrock engine
│   │   ├── service/BedrockExplainerClient.java  # Dual-mode (Local / AWS Bedrock)
│   │   └── autoconfigure/                       # AutoConfiguration & Properties
│   └── src/test/java/                           # Starter test suite
├── aegisgate-engine/                            # Production Spring Boot REST Application
│   ├── pom.xml
│   ├── src/main/java/com/aegisgate/             # Application & GatekeeperController
│   ├── src/main/resources/application.yml       # Local and AWS configuration profiles
│   └── src/test/java/                           # Integration MockMvc tests
├── scripts/
│   └── run_demo.py                              # Live end-to-end demonstration script
└── .ai/tasks/AEGIS-1001/                        # AI Software Factory execution trail
```

---

## Quickstart: Testing Locally (100% Offline)

AegisGate provides a turnkey local mode that requires zero external cloud connections:

### 1. Run the End-to-End Live Demonstration Script
```bash
python scripts/run_demo.py
```
This automatically boots the Strands Decider sidecar and runs tests for:
- Fast-Path Zero-Touch promotion (`ZERO_TOUCH_DEPLOY`, ~115ms, confidence $\ge 0.93$).
- Security Risk detection (`BLOCK_PRODUCTION` triggered by CVEs).
- Gate 1 Dev -> UAT promotion (`PROMOTE_UAT`).

### 2. Run the Java Test Suite
```bash
mvn test
```
All modules (starter, engine, controller integration tests) compile and pass in Java 21.

### 3. Start the Server Locally
In Terminal 1 (Start the Strands Decider Sidecar):
```bash
python aegis-policy-intervention/sidecar_server.py --port 8000
```

In Terminal 2 (Start the AegisGate Engine):
```bash
mvn spring-boot:run -pl aegisgate-engine
```

In Terminal 3 (Test with curl):
```bash
# Test Fast-Path Promotion Gate (Returns HTTP 200)
curl -X POST http://localhost:8080/api/v1/gates/production/evaluate \
  -H "Content-Type: text/plain" \
  -d "Build #100. JUnit: 100% passed. CVEs: 0. SLAs: 80ms."

# Test Blocked Gate (Returns HTTP 422 with Bedrock Risk Summary)
curl -X POST http://localhost:8080/api/v1/gates/production/evaluate \
  -H "Content-Type: text/plain" \
  -d "Build #101. Critical Vulnerability: CVE-2026-9999 detected in auth."
```

---

## Running on AWS (Cloud Mode)

When deploying to AWS (e.g. AWS ECS, EKS, EC2, or AWS Lambda), AegisGate connects to **Amazon Bedrock** using Anthropic Claude 3.5 Sonnet:

### 1. AWS Prerequisites
- AWS Account with Bedrock model access enabled for `anthropic.claude-3-5-sonnet-20240620-v1:0`.
- IAM Role or AWS Credentials configured with the `bedrock:InvokeModel` permission.

### 2. Configure Environment Variables
```bash
export BEDROCK_MODE=aws
export AWS_REGION=us-east-1
export DECIDER_SERVICE_URL=http://localhost:8000
```

### 3. Launch with AWS Profile
```bash
java -jar aegisgate-engine/target/aegisgate-engine-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=aws
```

---

## Reusable Decision Assets

### Asset 1: `AegisPolicyInterventionHandler` (Python)
Drop-in intervention handler for Python agent architectures:
```python
from aegis_intervention_handler import AegisPolicyInterventionHandler

handler = AegisPolicyInterventionHandler()
decision = handler.intercept_and_decide(context, question, options)
```

### Asset 2: `aegis-gatekeeper-spring-boot-starter` (Java)
Add the dependency to your `pom.xml`:
```xml
<dependency>
    <groupId>com.aegisgate</groupId>
    <artifactId>aegis-gatekeeper-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

And configure your `application.yml`:
```yaml
aegisgate:
  decider:
    service-url: http://localhost:8000
  bedrock:
    mode: aws
    region: us-east-1
```
Spring Boot automatically injects `AegisGatekeeperService` into your services.

---

## License
Apache License 2.0. Built for the AWS Agent Experience Hackathon.

