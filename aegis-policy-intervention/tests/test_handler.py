"""
Unit tests for AegisPolicyInterventionHandler
"""

import unittest
import sys
import os

# Ensure import works
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
from aegis_intervention_handler import AegisPolicyInterventionHandler


class TestAegisPolicyInterventionHandler(unittest.TestCase):

    def setUp(self):
        self.handler = AegisPolicyInterventionHandler(default_latency_ms=10)

    def test_production_gate_healthy_fast_path(self):
        context = """
        Build: #451
        JUnit: 142/142 passed (100%)
        Security Scan: 0 critical vulnerabilities, 0 high vulnerabilities
        Performance SLA: p99 latency 85ms (threshold: 200ms)
        Deploy Window: Tuesday 14:00 (Allowed)
        """
        question = "Does this release candidate satisfy zero critical CVEs, pass all integration SLA requirements, and conform to production deployment safety windows?"
        options = ["ZERO_TOUCH_DEPLOY", "BLOCK_PRODUCTION"]

        decision = self.handler.intercept_and_decide(context, question, options)

        self.assertEqual(decision.selected_option, "ZERO_TOUCH_DEPLOY")
        self.assertGreaterEqual(decision.confidence, 0.93)
        self.assertGreater(decision.latency_ms, 0)

    def test_production_gate_blocked_by_critical_cve(self):
        context = """
        Build: #452
        JUnit: 142/142 passed
        Security Scan: 1 Critical Vulnerability detected: CVE-2026-4421 in spring-web
        Performance SLA: p99 latency 90ms
        """
        question = "Does this release candidate satisfy zero critical CVEs, pass all integration SLA requirements, and conform to production deployment safety windows?"
        options = ["ZERO_TOUCH_DEPLOY", "BLOCK_PRODUCTION"]

        decision = self.handler.intercept_and_decide(context, question, options)

        self.assertEqual(decision.selected_option, "BLOCK_PRODUCTION")
        self.assertGreaterEqual(decision.confidence, 0.95)
        self.assertIn("Critical CVE", decision.rationale)

    def test_uat_gate_healthy(self):
        context = """
        Build: #102
        Unit Tests: 95/95 passed
        Database Migrations: V2__add_index.sql (Additive, non-destructive)
        OpenAPI Contract: Backwards-compatible
        """
        question = "Based on test success rates, static analysis, and database schema compatibility, is this build stable for UAT environment promotion without introducing breaking regressions?"
        options = ["PROMOTE_UAT", "REJECT_DEV"]

        decision = self.handler.intercept_and_decide(context, question, options)

        self.assertEqual(decision.selected_option, "PROMOTE_UAT")
        self.assertGreaterEqual(decision.confidence, 0.85)

    def test_inconclusive_or_ambiguous_metrics(self):
        context = """
        Build: #103
        Test results: partial coverage unverified
        Security scan: pending review
        """
        question = "Based on test success rates, static analysis, and database schema compatibility, is this build stable for UAT environment promotion without introducing breaking regressions?"
        options = ["PROMOTE_UAT", "REJECT_DEV"]

        decision = self.handler.intercept_and_decide(context, question, options)

        self.assertEqual(decision.selected_option, "REJECT_DEV")
        self.assertLess(decision.confidence, 0.85)


if __name__ == "__main__":
    unittest.main()

