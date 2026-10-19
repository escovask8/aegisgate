# Requisitos da Tarefa: AEGIS-1001

## 1. Requisitos Funcionais (RF)
- **RF01 (Gate 1 - Dev -> UAT):**
  - Pergunta tipada: `"Based on test success rates, static analysis, and database schema compatibility, is this build stable for UAT environment promotion without introducing breaking regressions?"`
  - Opções: `["PROMOTE_UAT", "REJECT_DEV"]`
  - Limiar de Confiança ($\tau$): `0.85`
- **RF02 (Gate 2 - UAT -> Produção):**
  - Pergunta tipada: `"Does this release candidate satisfy zero critical CVEs, pass all integration SLA requirements, and conform to production deployment safety windows?"`
  - Opções: `["ZERO_TOUCH_DEPLOY", "BLOCK_PRODUCTION"]`
  - Limiar de Confiança ($\tau$): `0.93`
- **RF03 (Fast-Path Zero-Touch):**
  - Se a opção selecionada for aprovação (`ZERO_TOUCH_DEPLOY` ou `PROMOTE_UAT`) e a confiança for $\ge \tau$, retorna HTTP 200 com status `allowed: true`.
- **RF04 (Smart-Fallback Bedrock):**
  - Se a opção for rejeição ou a confiança for $< \tau$, o AegisGate aciona o Amazon Bedrock (`anthropic.claude-3-5-sonnet-20240620-v1:0` ou fallback local configurável) para redigir um parecer executivo conciso em 3 tópicos, retornando HTTP 422 com `allowed: false` e a análise técnica.
- **RF05 (Fail-Closed):**
  - Se o Decider 2B sidecar estiver inacessível ou falhar, o sistema adota FAIL_CLOSED (bloqueia o release e retorna erro/bloqueio seguro).
- **RF06 (Reusable Asset 1 - Python):**
  - Implementar classe `AegisPolicyInterventionHandler` e servidor FastAPI (`sidecar_server.py`) expondo `/decide` e `/health`.
- **RF07 (Reusable Asset 2 - Java Starter):**
  - Implementar starter autoconfigurável Spring Boot `aegis-gatekeeper-spring-boot-starter` com classes de configuração, cliente REST e propriedades `aegisgate.decider.*` e `aegisgate.bedrock.*`.

## 2. Requisitos Não Funcionais (RNF)
- **RNF01 (Latência do Fast-Path):** O ciclo de decisão do Decider deve ocorrer em $< 150\text{ms}$.
- **RNF02 (Compatibilidade Multi-Ambiente):** A solução deve poder rodar 100% offline (modo local com mock de Bedrock e sidecar simulador do Decider) e em ambiente AWS com Bedrock real.
- **RNF03 (Java 21 e Spring Boot 3):** Uso de Java Records e APIs modernas do Spring Boot 3 (`RestClient`).
- **RNF04 (Auditabilidade):** Logs estruturados registrando timestamp, latência, métricas de entrada e parecer de decisão.

