# Contexto da Tarefa: AEGIS-1001

## 1. Origem
- Documento de Requisitos: `spec.md` (AegisGate: Autonomous Zero-Touch Promotion Engine)
- Hackathon: AWS Builder Center - Agent Experience Hackathon: Strands Decider
- Padrão de Decisão Primário: `#policy` | Secundário: `#guardrails`

## 2. Tecnologias e Dependências de Runtime
- Java 21 (Temurin LTS)
- Maven 3.9.9
- Python 3.14 (compatível com 3.11+)
- Spring Boot 3.3.x / AWS SDK for Java v2 (Bedrock Runtime)
- FastAPI / Uvicorn para o sidecar local do Strands Decider

## 3. Objetivos
Implementar a solução integral do AegisGate com:
1. `aegis-policy-intervention/`: Reusable Asset 1 (Python)
2. `aegis-gatekeeper-spring-boot-starter/`: Reusable Asset 2 (Java)
3. `aegisgate-engine/`: Aplicação central Spring Boot 3 com endpoints REST de avaliação de Gates (UAT e Produção)
4. Modos de execução híbridos:
   - Modo Local (Offline/Dev): Simulação determinística rápida do Decider 2B e Bedrock Mock / fallback local.
   - Modo Nuvem AWS: Conexão real com Amazon Bedrock (Claude 3.5 Sonnet) via AWS SDK v2 com suporte a AWS Default Credentials Chain e IAM Roles for Service Accounts (IRSA).

