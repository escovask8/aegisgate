package com.aegisgate.controller;

import com.aegisgate.model.DecisionContracts.GateEvaluationResult;
import com.aegisgate.service.AegisGatekeeperService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/gates")
public class GatekeeperController {

    private final AegisGatekeeperService gatekeeperService;

    public GatekeeperController(AegisGatekeeperService gatekeeperService) {
        this.gatekeeperService = gatekeeperService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "AegisGate Autonomous Zero-Touch Promotion Engine",
                "version", "1.0.0"
        ));
    }

    @PostMapping("/production/evaluate")
    public ResponseEntity<GateEvaluationResult> evaluateProduction(@RequestBody String metricsSnapshot) {
        GateEvaluationResult result = gatekeeperService.evaluateProductionGate(metricsSnapshot);
        if (result.allowed()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result);
    }

    @PostMapping("/uat/evaluate")
    public ResponseEntity<GateEvaluationResult> evaluateUat(@RequestBody String metricsSnapshot) {
        GateEvaluationResult result = gatekeeperService.evaluateUatGate(metricsSnapshot);
        if (result.allowed()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result);
    }
}

