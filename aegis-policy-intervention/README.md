# Reusable Asset 1: AegisPolicyInterventionHandler (Python)

A drop-in decision pattern that intercepts deployment promotion events, evaluates multi-source compliance context, and makes a sub-150ms deterministic decision using Strands Decider 2B.

## Usage in Python / Strands Agents

```python
from aegis_intervention_handler import AegisPolicyInterventionHandler

handler = AegisPolicyInterventionHandler()
context = "JUnit: 100% passed. CVEs: 0. SLAs: 85ms."
question = "Does this release candidate satisfy zero critical CVEs, pass all integration SLA requirements, and conform to production deployment safety windows?"
options = ["ZERO_TOUCH_DEPLOY", "BLOCK_PRODUCTION"]

decision = handler.intercept_and_decide(context, question, options)
print(f"Selected: {decision.selected_option}, Confidence: {decision.confidence:.2f}, Latency: {decision.latency_ms}ms")
```

## Running the Sidecar Server

```bash
# Starts HTTP server listening on http://127.0.0.1:8000
python sidecar_server.py --port 8000
```

