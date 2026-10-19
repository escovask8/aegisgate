# Plano de Implementação (Plan): AEGIS-1001

## 1. Objetivo
Desenvolver de ponta a ponta a solução AegisGate com capacidade de teste local (FastAPI sidecar + Spring Boot Engine) e suporte nativo a AWS Bedrock na nuvem.

## 2. Estrutura do Repositório a ser criada
```
aegisgate/
├── pom.xml                                      # Root aggregator Maven POM
├── aegis-policy-intervention/                   # Reusable Asset 1 (Python)
│   ├── pyproject.toml / requirements.txt
│   ├── aegis_intervention_handler.py            # Reusable Strands Decision Handler
│   ├── sidecar_server.py                        # FastAPI /decide and /health endpoint
│   └── tests/
│       └── test_handler.py                      # Testes unitários do handler Python
├── aegis-gatekeeper-spring-boot-starter/         # Reusable Asset 2 (Java Starter)
│   ├── pom.xml
│   └── src/main/java/com/aegisgate/
│       ├── model/DecisionContracts.java         # Records de Request/Response
│       ├── service/AegisGatekeeperService.java  # Engine de decisão + Bedrock
│       ├── service/BedrockExplainerClient.java  # Interface abstrata p/ Bedrock
│       ├── service/AwsBedrockExplainer.java     # Implementação real AWS SDK
│       ├── service/LocalBedrockExplainer.java   # Implementação Mock local
│       └── autoconfigure/                       # AutoConfiguration & Properties
└── aegisgate-engine/                            # Servidor central Spring Boot 3
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/aegisgate/
        │   │   ├── AegisGateApplication.java
        │   │   └── controller/GatekeeperController.java
        │   └── resources/
        │       ├── application.yml
        │       └── application-local.yml
        └── test/
            └── java/com/aegisgate/
                └── GatekeeperControllerIntegrationTest.java
```

## 3. Sequência de Execução
1. **Configuração Maven Raiz**: Criar `pom.xml` pai agrupando `aegis-gatekeeper-spring-boot-starter` e `aegisgate-engine` com Java 21, Spring Boot 3.3.4 e AWS SDK Bedrock v2.
2. **Implementação Python (Reusable Asset 1)**:
   - Implementar `AegisPolicyInterventionHandler` com validação de regras de inferência do Decider 2B.
   - Implementar `sidecar_server.py` com FastAPI, endpoints `/decide` e `/health`, suporte a parâmetros e latência calibrada (~115ms).
   - Testes unitários em Python com `unittest`.
3. **Implementação Java Starter (Reusable Asset 2)**:
   - `DecisionContracts` (Records: `DecisionRequest`, `DecisionResponse`, `GateEvaluationResult`).
   - Abstração e suporte a Bedrock (`BedrockExplainerClient`), permitindo chavear via propriedade `aegisgate.bedrock.mode=aws` ou `local`.
   - `AegisGatekeeperService` com suporte a Gate UAT e Gate Produção, `CONFIDENCE_THRESHOLD`, fail-closed seguro.
   - Autoconfiguração do Spring Boot (`AegisGatekeeperAutoConfiguration`).
4. **Implementação do AegisGate Engine**:
   - `GatekeeperController` expondo `/api/v1/gates/production/evaluate` e `/api/v1/gates/uat/evaluate`.
   - Testes de integração Spring Boot (`@SpringBootTest`, `@AutoConfigureMockMvc`).
5. **Scripts e Ferramental de Teste Local e Nuvem**:
   - Script python/powershell de execução de demo ponta a ponta (`scripts/run_demo.py` ou similar).
   - Validação de compilação Maven (`mvn clean install`).
   - Execução de testes automatizados e geração de evidências.

## 4. Riscos e Mitigações
- **Risco 1:** Falta de credenciais AWS em ambiente local.
  * *Mitigação:* `LocalBedrockExplainer` ativado por padrão em profile `local` ou quando AWS credentials não estiverem configuradas, garantindo que `mvn test` passe sem depender de credenciais externas.
- **Risco 2:** Sidecar indisponível.
  * *Mitigação:* Tratamento de exceção `RestClientException` disparando `FAIL_CLOSED` imediato sem travar o servidor.

