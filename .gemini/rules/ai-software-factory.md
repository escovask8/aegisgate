# AI Software Factory - Regras Operacionais e de Governança

Ao executar tarefas sob o workflow **AI Software Factory** (Jira / Tarefa para PR):

## 1. Princípio Fundamental
A autonomia deve significar que os agentes conseguem investigar, implementar, testar, revisar e corrigir uma tarefa sem intervenção humana a cada passo — não que eles tenham permissão irrestrita. O Orchestrator controla estado, escopo, evidências, limites e pontos de escalonamento.

## 2. Escopo e Limites de Autonomia
* **max_iterations = 5:** Máximo de 5 ciclos de correção entre Developer e QA/Reviewer por tarefa.
* **max_unrelated_files = 0:** Trabalhe exclusivamente dentro do escopo da tarefa recebida. Não altere arquivos não relacionados sem justificativa.
* **production_deploy = forbidden:** Nunca fazer deploy em produção automaticamente na primeira versão.
* Não executar comandos destrutivos sem aprovação explícita.
* Não alterar secrets, credenciais, permissões ou infraestrutura de produção.

## 3. Gatilhos Obrigatórios para HUMAN_REVIEW
Interromper imediatamente e solicitar validação humana diante de:
1. Requisito ambíguo ou decisão que altere comportamento de negócio;
2. Migration destrutiva de banco de dados;
3. Breaking change em contratos de API ou APIs públicas;
4. Mudança de autenticação, autorização ou criptografia;
5. Manipulação de secrets, credenciais ou permissões;
6. Infraestrutura de produção ou deploy de produção;
7. Acesso ou alteração de dados sensíveis;
8. Ciclo de correções excedido (> 5 iterações).

## 4. Persistência de Estado (.ai/tasks/JIRA-XXXX/)
* Toda tarefa registra seus artefatos em `.ai/tasks/JIRA-XXXX/`:
  `task.json`, `context.md`, `requirements.md`, `plan.md`, `architecture.md`, `implementation.md`, `test-plan.md`, `qa-report.md`, `review.md`, `status.json`.
* O estado deve ser legível por máquina e por humanos.
* Cada agente deve atualizar somente os artefatos sob sua responsabilidade e nunca apagar evidências produzidas por outro agente.

## 5. Convenção de Git
* **Branch:** `feature/JIRA-XXXX`
* **Commits:**
  * `feat(JIRA-XXXX): implement <feature>`
  * `test(JIRA-XXXX): add <tests>`
  * `fix(JIRA-XXXX): correct <issue>`
* Nunca fazer merge direto em `main`/`develop`. Criar PR/MR e deixar a política de aprovação controlar o merge.

