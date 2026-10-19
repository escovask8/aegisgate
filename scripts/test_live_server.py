import urllib.request
import json
import time

def call_api(endpoint, method="GET", text_body=None):
    url = f"http://localhost:8080/api/v1/gates/{endpoint}"
    req = urllib.request.Request(url, method=method)
    if text_body:
        payload = text_body.encode("utf-8")
        req.add_header("Content-Type", "text/plain")
    else:
        payload = None

    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req, data=payload, timeout=5) as res:
            elapsed = int((time.perf_counter() - start) * 1000)
            return res.status, json.loads(res.read().decode("utf-8")), elapsed
    except urllib.error.HTTPError as e:
        elapsed = int((time.perf_counter() - start) * 1000)
        return e.code, json.loads(e.read().decode("utf-8")), elapsed
    except Exception as ex:
        return 0, {"error": str(ex)}, 0

def main():
    print("=" * 80)
    print("  AEGISGATE LIVE INTEGRATION TEST SUITE (Localhost:8080 + 8000)")
    print("=" * 80)

    # 1. Health check
    status, body, ms = call_api("health")
    print(f"\n[1. Health Check] Status: {status} ({ms}ms)")
    print(json.dumps(body, indent=2))
    assert status == 200, f"Expected 200, got {status}"

    # 2. Gate 2 - Production Fast-Path (Healthy)
    healthy_prod = """
    Build: #842
    JUnit: 350/350 passed (100%)
    Security Scan: 0 critical vulnerabilities, 0 high vulnerabilities
    Load Test SLA: p99 latency 78ms (threshold: 200ms)
    Deploy Window: Tuesday 15:00 UTC (Approved)
    Rollback Plan: Automated canary rollback validated
    """
    status, body, ms = call_api("production/evaluate", method="POST", text_body=healthy_prod)
    print(f"\n[2. Gate 2: UAT -> Prod (Fast-Path Zero-Touch)] Status: {status} ({ms}ms total)")
    print(f"   Allowed:          {body.get('allowed')}")
    print(f"   Reason:           {body.get('reason')}")
    decider = body.get("deciderMetrics", {})
    print(f"   Decider Option:   {decider.get('selectedOption')}")
    print(f"   Confidence:       {decider.get('confidence')}")
    print(f"   Decider Latency:  {decider.get('latencyMs')}ms")
    assert status == 200, f"Expected 200, got {status}"
    assert body.get("allowed") is True
    assert decider.get("selectedOption") == "ZERO_TOUCH_DEPLOY"
    assert decider.get("confidence") >= 0.93

    # 3. Gate 2 - Production Blocked (CVE Risk)
    cve_prod = """
    Build: #843
    JUnit: 350/350 passed
    Security Scan: 1 Critical Vulnerability detected: CVE-2026-3841 in authentication module
    Load Test SLA: p99 latency 85ms
    """
    status, body, ms = call_api("production/evaluate", method="POST", text_body=cve_prod)
    print(f"\n[3. Gate 2: UAT -> Prod (Smart-Fallback: Security Block)] Status: {status} ({ms}ms total)")
    print(f"   Allowed:          {body.get('allowed')}")
    print(f"   Reason:           {body.get('reason')}")
    print(f"   Bedrock 3-Bullet Analysis:\n{body.get('detailedExplanation')}")
    assert status == 422, f"Expected 422, got {status}"
    assert body.get("allowed") is False
    assert body.get("deciderMetrics", {}).get("selectedOption") == "BLOCK_PRODUCTION"

    # 4. Gate 1 - Dev -> UAT (Healthy)
    healthy_uat = """
    Build: #844
    Unit & Mutation Tests: 110/110 passed (Stryker Score: 92%)
    Schema Migration: V3__create_audit_log.sql (Additive, backward-compatible)
    OpenAPI Specification: Zero breaking changes detected
    """
    status, body, ms = call_api("uat/evaluate", method="POST", text_body=healthy_uat)
    print(f"\n[4. Gate 1: Dev -> UAT (Healthy Promotion)] Status: {status} ({ms}ms total)")
    print(f"   Allowed:          {body.get('allowed')}")
    print(f"   Reason:           {body.get('reason')}")
    print(f"   Decider Option:   {body.get('deciderMetrics', {}).get('selectedOption')}")
    assert status == 200, f"Expected 200, got {status}"
    assert body.get("allowed") is True
    assert body.get("deciderMetrics", {}).get("selectedOption") == "PROMOTE_UAT"

    # 5. Gate 1 - Dev -> UAT (Breaking Change Rejection)
    breaking_uat = """
    Build: #845
    Unit Tests: 108/110 passed, 2 failures
    Schema Migration: Destructive migration detected: dropping column 'user_uuid'
    """
    status, body, ms = call_api("uat/evaluate", method="POST", text_body=breaking_uat)
    print(f"\n[5. Gate 1: Dev -> UAT (Breaking Change Block)] Status: {status} ({ms}ms total)")
    print(f"   Allowed:          {body.get('allowed')}")
    print(f"   Reason:           {body.get('reason')}")
    print(f"   Bedrock 3-Bullet Analysis:\n{body.get('detailedExplanation')}")
    assert status == 422, f"Expected 422, got {status}"
    assert body.get("allowed") is False

    print("\n" + "=" * 80)
    print("  ALL 5 LIVE INTEGRATION TESTS PASSED WITH 100% SUCCESS!")
    print("=" * 80)

if __name__ == "__main__":
    main()

