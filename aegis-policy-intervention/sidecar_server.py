"""
AegisGate: Strands Decider 2B Sidecar Server
Exposes HTTP endpoints (/decide, /health) matching the AegisGate Decision Contracts.
Supports both FastAPI (if installed) and fallback Python standard library HTTP server.
"""

import sys
import json
import logging
import argparse
from typing import List, Optional
from http.server import HTTPServer, BaseHTTPRequestHandler
from aegis_intervention_handler import AegisPolicyInterventionHandler, EvaluationDecision

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s - %(message)s")
logger = logging.getLogger("aegisgate.sidecar")

handler = AegisPolicyInterventionHandler()


def handle_decision_request(payload: dict) -> dict:
    context = payload.get("context", "")
    question = payload.get("question", "")
    options = payload.get("options", ["ZERO_TOUCH_DEPLOY", "BLOCK_PRODUCTION"])

    decision = handler.intercept_and_decide(context, question, options)
    return {
        "selectedOption": decision.selected_option,
        "confidence": decision.confidence,
        "latencyMs": decision.latency_ms,
        "rationale": decision.rationale
    }


# Standard Library Fallback HTTP Server for zero-dependency local execution
class StandardHttpHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path in ("/health", "/api/v1/health"):
            response = {
                "status": "UP",
                "engine": "Strands Decider 2B (AegisGate Sidecar)",
                "checkpoint": handler.model_checkpoint
            }
            self._send_json(200, response)
        else:
            self._send_json(404, {"error": "Not Found"})

    def do_POST(self):
        if self.path in ("/decide", "/api/v1/decide"):
            content_length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_length)
            try:
                data = json.loads(body.decode("utf-8"))
                result = handle_decision_request(data)
                self._send_json(200, result)
            except Exception as e:
                logger.error("Error processing request: %s", e)
                self._send_json(400, {"error": str(e)})
        else:
            self._send_json(404, {"error": "Not Found"})

    def _send_json(self, status_code: int, data: dict):
        response_bytes = json.dumps(data).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(response_bytes)))
        self.end_headers()
        self.wfile.write(response_bytes)

    def log_message(self, format, *args):
        logger.info("%s - - [%s] %s", self.client_address[0], self.log_date_time_string(), format % args)


def run_server(host: str = "127.0.0.1", port: int = 8000):
    # Try FastAPI first if installed
    try:
        import uvicorn
        from fastapi import FastAPI, HTTPException, Request
        from pydantic import BaseModel

        app = FastAPI(title="AegisGate Strands Decider Sidecar", version="1.0.0")

        class DecisionReq(BaseModel):
            context: str
            question: str
            options: List[str]

        class DecisionResp(BaseModel):
            selectedOption: str
            confidence: float
            latencyMs: int
            rationale: Optional[str] = None

        @app.get("/health")
        def health():
            return {
                "status": "UP",
                "engine": "Strands Decider 2B (AegisGate Sidecar)",
                "checkpoint": handler.model_checkpoint
            }

        @app.post("/decide")
        async def decide(request: Request):
            try:
                data = await request.json()
            except Exception:
                data = {}
            return handle_decision_request(data)

        logger.info("Starting sidecar using FastAPI on http://%s:%d", host, port)
        uvicorn.run(app, host=host, port=port, log_level="info")
        return
    except ImportError:
        logger.info("FastAPI/uvicorn not installed; running lightweight standard HTTP server on http://%s:%d", host, port)

    httpd = HTTPServer((host, port), StandardHttpHandler)
    logger.info("AegisGate Sidecar listening on http://%s:%d", host, port)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        logger.info("Stopping sidecar server...")
        httpd.server_close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="AegisGate Decider Sidecar Server")
    parser.add_argument("--host", default="127.0.0.1", help="Host to bind (default: 127.0.0.1)")
    parser.add_argument("--port", type=int, default=8000, help="Port to bind (default: 8000)")
    args = parser.parse_args()

    run_server(host=args.host, port=args.port)

