# Arquitetura Técnica: AegisGate (AEGIS-1001)

## 1. Visão Geral da Arquitetura
O AegisGate opera como um interceptor e avaliador de risco para esteiras de CI/CD corporativas (GitHub Actions, AWS CodePipeline, GitLab CI). Ele combina a velocidade e custo irrisório de inferência de um modelo ultra-compacto (**Strands Decider 2B**, ~115ms) para 90% dos fluxos de rotina com o poder analítico de um LLM de fronteira (**Amazon Bedrock / Anthropic Claude 3.5 Sonnet**) apenas quando anomalias, riscos ou baixa confiança estatística são detectados.

```
                          ┌───────────────────────────┐
                          │ CI/CD Pipeline Promotion  │
                          └─────────────┬─────────────┘
                                        │ POST /api/v1/gates/{gate}/evaluate
                                        ▼
    ┌────────────────────────────────────────────────────────────────────────┐
    │                     AegisGate Engine (Spring Boot 3)                   │
    │                                                                        │
    │  ┌────────────────────────┐         ┌───────────────────────────────┐  │
    │  │  GatekeeperController  │         │  AegisGatekeeperService       │  │
    │  └───────────┬────────────┘         └───────────────┬───────────────┘  │
    └──────────────┼──────────────────────────────────────┼──────────────────┘
                   │                                      │
                   │                                      │ 1. Avaliação Rápida (/decide)
                   │                                      ▼
                   │                      ┌───────────────────────────────┐
                   │                      │   Aegis Decider 2B Sidecar    │
                   │                      │      (Python / FastAPI)       │
                   │                      │     ~115ms / Determinístico   │
                   │                      └───────────────┬───────────────┘
                   │                                      │
                   │                 ┌────────────────────┴───────────────────┐
                   │                 ▼                                        ▼
                   │        [GO & Confiança >= tau]               [NO_GO ou Confiança < tau]
                   │                 │                                        │
                   │                 │ Fast-Path                              │ Fallback
                   │                 │                                        ▼
                   │                 │                        ┌───────────────────────────────┐
                   │                 │                        │   BedrockExplainerClient      │
                   │                 │                        │ ├─ AwsBedrockExplainer (AWS)  │
                   │                 │                        │ └─ LocalBedrockExplainer(Mock)│
                   │                 │                        └───────────────┬───────────────┘
                   │                 ▼                                        ▼
                   │          HTTP 200 (Allowed)                     HTTP 422 (Blocked + 3 Bullets)
                   │
                   ▼
    ┌───────────────────────────┐
    │ Notificação Slack / PR    │
    └───────────────────────────┘
```

## 2. Decisões Arquiteturais e Padrões de Projeto

### DA01: Desacoplamento através de Reusable Assets
- **Asset 1 (Python):** `aegis-policy-intervention`. Um pacote Python contendo a classe `AegisPolicyInterventionHandler` compatível com o ecossistema Strands Agents da AWS, além de um servidor HTTP leve (`sidecar_server.py`) baseado em FastAPI para hospedar o Decider 2B em contêineres sidecar (ECS, EKS, Kubernetes ou localmente).
- **Asset 2 (Java):** `aegis-gatekeeper-spring-boot-starter`. Uma biblioteca Spring Boot com auto-configuração condicional (`@AutoConfiguration`, `@ConditionalOnMissingBean`), permitindo que qualquer microsserviço ou portal de plataforma Java integre o AegisGate com dependência Maven e configuração no `application.yml`.

### DA02: Estratégia de Fallback e Chaveamento Local vs AWS
Para viabilizar testes 100% locais sem depender de conexão ou credenciais da AWS, a camada do Bedrock é desacoplada por uma interface:
```java
public interface BedrockExplainerClient {
    String generateExplanation(String metricsReport, DecisionResponse decision);
}
```
Duas implementações são fornecidas:
1. `AwsBedrockExplainer`: Utiliza o AWS SDK for Java v2 (`BedrockRuntimeClient`) para invocar `anthropic.claude-3-5-sonnet-20240620-v1:0` via JSON padronizado Messages API do Bedrock.
2. `LocalBedrockExplainer`: Gera pareceres técnicos determinísticos em 3 bullet points baseados na análise sintática dos dados submetidos, permitindo testes unitários e de integração herméticos sem custo de rede.
O chaveamento é controlado por `aegisgate.bedrock.mode`: `aws` ou `local` (default em profile local).

### DA03: Política Fail-Closed para Segurança de Plataforma
Se a chamada ao Strands Decider 2B falhar por timeout, desconexão de rede ou erro 5xx:
- O AegisGate **NUNCA** libera o deploy.
- O resultado é imediatamente marcado como `allowed: false`.
- O motivo registra o incidente de infraestrutura e encaminha para intervenção manual (`HUMAN_REVIEW`).

### DA04: Modelagem dos Gates e Limiares
- **Gate UAT:** `PROMOTE_UAT` vs `REJECT_DEV`, $\tau = 0.85$.
- **Gate Produção:** `ZERO_TOUCH_DEPLOY` vs `BLOCK_PRODUCTION`, $\tau = 0.93$.

## 3. Contratos de Dados (DTOs)
- `DecisionRequest(String context, String question, List<String> options)`
- `DecisionResponse(String selectedOption, double confidence, long latencyMs)`
- `GateEvaluationResult(boolean allowed, String reason, DecisionResponse deciderMetrics)`

## 4. Avaliação do Arquiteto
- **Status:** `PASS`.
- **Justificativa:** O desenho atende com precisão aos requisitos do hackathon da AWS (Fast-Path com Strands Decider 2B, Smart-Fallback com Amazon Bedrock, os 2 Reusable Assets, medição de latência e compatibilidade multi-ambiente). O plano é tecnicamente executável.

