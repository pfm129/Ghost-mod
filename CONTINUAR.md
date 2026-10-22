# Estado final do Ghost

Projeto: mod cliente Fabric em `outputs/Ghost`, Minecraft Java 26.3.

## Entregue

- Projeto Gradle com Java 25, Fabric Loader 0.19.5, Fabric API 0.163.0+26.3 e Loom 1.18.4 (a versão resolvida pelo seletor `1.18-SNAPSHOT`).
- Código em Java: interceptação local do prefixo `ghost`, OpenAI Responses API, tela de configuração em pt_BR, comandos locais, contexto minimizado, histórico efêmero, fila/rate limit e armazenamento DPAPI no Windows com fallback apenas em memória.
- Testes unitários para prefixo, sanitização de contexto, rate limit e parsing da resposta.
- JAR instalado/entregável em `build/libs/ghost-1.0.0.jar`, com código, recursos, licença e JNA/JNA-platform aninhados para DPAPI.
- README e exemplo sem segredos. Nenhuma chave real foi usada.

## Verificações feitas

- Compilação manual Java 25 de todo o código contra o JAR cliente Minecraft 26.3 e os 160 JARs de dependências já disponíveis: passou.
- Seis testes JUnit executados manualmente fora da tarefa Gradle: passaram.
- O arquivo final é um ZIP/JAR válido; `fabric.mod.json`, recursos, classes e os dois JARs aninhados foram conferidos. A metadata inclui `fabric.mod.json` próprio para cada dependência aninhada.
- O JAR ainda não foi aberto em uma instância real de Minecraft; a validação de execução visual e de carregamento Mixin/Fabric continua pendente.

## Limitação do build neste ambiente

O comando pedido `gradlew.bat build --no-daemon --console=plain` foi executado e falhou durante a configuração do Loom, antes da compilação Gradle: `Failed to setup Minecraft ... Failed to split minecraft`. O ambiente Codex restringe a escrita/substituição de ZIP via Java NIO. A compilação manual funcionou e permitiu entregar o JAR, mas isso não é o mesmo que um `gradlew build` bem-sucedido. Em uma instalação Windows normal, execute `.\gradlew.bat build` na pasta do projeto para validar o build Gradle convencional e regenerar o artefato.

## Limitações reais do mod

- O contexto estendido de seed/mapa/estruturas aparece como planejado e não coleta esses dados nesta versão.
- Não foi possível testar uma chamada real à OpenAI, pois isso exigiria uma chave e faturamento da API do usuário. Configure uma chave no menu e use **Testar conexão**.
- A compatibilidade de runtime deve ser confirmada em uma instância Fabric 26.3 com o JAR entregue; não houve sessão de jogo disponível para smoke test.
