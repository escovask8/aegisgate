# Plano de Testes (Test Plan): AEGIS-1001

## 1. Escopo de Testes
Testar as camadas de decisão determinística (Python Strands), a biblioteca de integração (Spring Boot Starter) e o orquestrador de gates (AegisGate Engine) em cenários de fast-path, fallback e tolerância a falhas.

## 2. Cenários de Teste

### Cenário 1: Fast-Path UAT (Dev -> UAT)
- **Entrada:** Métricas com 100% testes passando, zero vulnerabilidades e sem breaking changes em API.
- **Resultado Esperado:** Strands Decider retorna `selectedOption: PROMOTE_UAT`, `confidence >= 0.85`, HTTP 200 `allowed: true`.

### Cenário 2: Fast-Path Produção (UAT -> Prod)
- **Entrada:** Métricas saudáveis com 0 CVEs críticas, SLAs < 200ms em testes de carga, janela de deploy válida (terça-feira 14h).
- **Resultado Esperado:** Strands Decider retorna `selectedOption: ZERO_TOUCH_DEPLOY`, `confidence >= 0.93`, HTTP 200 `allowed: true`.

### Cenário 3: Smart-Fallback por Risco Crítico
- **Entrada:** Relatório contendo vulnerabilidade crítica CVE-2026-9999 ou quebra de SLA.
- **Resultado Esperado:** Strands Decider retorna `selectedOption: BLOCK_PRODUCTION`, Engine invoca o Bedrock para produzir os 3 pontos de bloqueio técnico, HTTP 422 `allowed: false`.

### Cenário 4: Smart-Fallback por Baixa Confiança (Ambiguidade)
- **Entrada:** Relatório ambíguo com métricas conflitantes onde a confiança calculada é inferior ao limiar ($\tau = 0.93$).
- **Resultado Esperado:** Fallback para o Bedrock para avaliação aprofundada, HTTP 422 `allowed: false`.

### Cenário 5: Resiliência Fail-Closed
- **Entrada:** Sidecar Python desligado ou inacessível.
- **Resultado Esperado:** O sistema não deixa o release passar; captura a falha de comunicação e responde `allowed: false` com motivo `"FAIL_CLOSED: Decider engine unavailable"`.

## 3. Matriz de Verificação de Ferramentas
- Testes Unitários Python: `unittest` / `pytest`
- Testes de Integração Java: `JUnit 5` + `Spring Boot Test` + `MockMvc`
- Teste Ponta a Ponta: Script de demonstração executando sidecar e engine simultaneamente

