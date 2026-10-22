# Ghost

Mod cliente para Minecraft Java 26.3 que encaminha perguntas iniciadas por `ghost` diretamente à API Responses da OpenAI e mostra a resposta apenas no chat local. O servidor Minecraft não recebe a pergunta. O Ghost não é afiliado à Mojang, à Microsoft nem à OpenAI.

## Requisitos

- Minecraft Java 26.3 e Fabric Loader 0.19.5.
- Fabric API 0.163.0+26.3.
- Java 25 para executar o jogo e compilar.
- Uma chave da API OpenAI com faturamento e acesso ao modelo escolhido.

As versões do projeto seguem o [template oficial Fabric 26.3](https://github.com/FabricMC/fabric-example-mod/tree/26.3). O modelo padrão é `gpt-6-luna`, listado pela OpenAI como opção econômica para tarefas focadas e de alto volume; confirme disponibilidade e preços no [catálogo de modelos](https://developers.openai.com/api/docs/models/gpt-6-luna) e na [página de preços da API](https://openai.com/api/pricing/). Modelos e preços podem mudar; use um modelo habilitado para sua conta.

A interceptação usa um mixin client-side no método de envio do `ChatScreen`, no ponto anterior à transmissão da mensagem. Esse ponto permite cancelar a entrada `ghost` antes do envio normal; o mixin não altera mensagens sem esse prefixo.

## Build

No Windows, abra PowerShell nesta pasta e execute:

```powershell
.\gradlew.bat build
```

O primeiro build baixa Gradle, Minecraft e dependências. O artefato fica em `build/libs/ghost-1.0.0.jar`. Para IDE, importe o `build.gradle` como projeto Gradle. O projeto usa Java 25, Gradle 9.7.1, Loom 1.18-SNAPSHOT e Fabric API 0.163.0+26.3. Nesta entrega, o código passou por compilação Java direta e o JAR foi empacotado; o Loom não concluiu `build` no ambiente Codex porque a escrita do JAR Minecraft falhou dentro do sandbox. Rode o comando em uma instalação normal do Windows para validar e regenerar pelo Gradle.

## Instalação e uso

1. Instale Fabric Loader 0.19.5 para Minecraft 26.3 e coloque Fabric API e `ghost-1.0.0.jar` na pasta `mods` do perfil Fabric.
2. Abra o jogo. Use a tecla `G` (alterável em Opções → Controles → Ghost) ou `/ghost config`.
3. Crie uma chave na plataforma da API OpenAI e cole-a no campo mascarado. O botão de conexão faz uma chamada curta.
4. Selecione um modelo e as opções de privacidade, então clique em **Salvar e ativar**.
5. No chat, escreva `ghost, sua pergunta`. Exemplos: `ghost, explique redstone` ou `GHOST: qual camada é melhor para diamantes?`.

Somente `ghost` no começo, sem diferenciar maiúsculas/minúsculas, seguido de espaço, vírgula, dois-pontos ou fim de texto é interceptado. Nenhuma mensagem comum é alterada. Os comandos locais são `/ghost config`, `/ghost clear` e `/ghost status`.

## Privacidade e segurança

- A pergunta, o contexto que você habilitou e um resumo curto do histórico da sessão são enviados diretamente por HTTPS a `api.openai.com`. Não há servidor intermediário do Ghost.
- O servidor de Minecraft não recebe perguntas Ghost. O mod é somente cliente e não executa comandos do jogo, automações, movimentação, mineração ou ações no personagem.
- Configurações não sensíveis ficam em `config/ghost/settings.json`. No Windows, a chave é protegida pelo DPAPI vinculada ao usuário local e gravada separadamente como texto cifrado em `config/ghost/secret.json`. Em outros sistemas, ou se DPAPI não funcionar, a chave só fica na memória durante a sessão; o mod avisa e não a salva em texto puro.
- A proteção local não impede que um programa malicioso já executando sob o mesmo usuário leia a chave em memória ou use a sessão. DPAPI evita exposição casual, cópia simples do arquivo ou extração da chave copiando o `.jar`.
- Por padrão, não são enviados nome/UUID da conta, IP, endereço do servidor, chat público, inventário completo, seed ou dados de outros jogadores. Contexto de jogo pode enviar versão, dimensão, Y, bioma, dificuldade, vida/fome, item da mão e alvo mirado. Cada categoria pode ser desativada na tela **Categorias do contexto**.
- X/Z exatos ficam desligados por padrão; ativá-los pode revelar localizações do mundo ao provedor da IA.
- O contexto estendido aparece como planejado e não coleta seed, mapa, estruturas nem chunks. O Ghost nunca afirma ter acesso ao mapa inteiro. Em qualquer modo, só envia os dados locais listados acima.
- O histórico mantém no máximo oito interações em memória e é apagado ao sair do jogo; pode ser limpo com `/ghost clear` ou no menu. Desative **Manter histórico** para não incluí-lo nas chamadas.
- Perguntas são limitadas a 1.200 caracteres, o contexto é limitado e a saída é limitada a 320 tokens e dividida em mensagens de chat.
- Busca web é opcional, desligada por padrão e só é solicitada para perguntas que parecem depender de informação atual, quando ativada. A disponibilidade depende do modelo e da API. Fontes reconhecidas podem aparecer no final da resposta.

## Custos

O Ghost usa a API OpenAI e cobra conforme o faturamento, modelo, tokens e ferramentas da conta do jogador. A opção de busca web pode ter custos próprios. Verifique os valores e limites na [página oficial de preços](https://openai.com/api/pricing/) antes de usar. **Assinatura ChatGPT e créditos da API são produtos separados**: uma assinatura ChatGPT não inclui automaticamente saldo de API. Configure limites de uso no painel OpenAI e no próprio Ghost.

## Limitações

- Respostas dependem do modelo e podem conter erros; confirme informações importantes. Versões, modpacks, regras e configurações de servidores podem alterar a resposta.
- O Ghost não lê chat público, arquivos, inventários completos, IP, dados de outros jogadores, chunks não carregados ou dados privados do servidor.
- Contexto estendido, seed e localização de estruturas não estão implementados. Não são usados comandos `/locate` nem descoberta de seed.
- Teste de conexão valida uma chamada mínima, mas não garante que todos os pedidos futuros terão saldo, limite ou acesso ao modelo.
- Cancelamento ao desconectar, trocar de mundo ou fechar o cliente invalida as respostas antigas; a requisição HTTP em andamento também é interrompida quando o runtime permite.

## Solução de problemas

- **A configuração não abre:** use `G`, `/ghost config` ou confirme que o mod está instalado no perfil Fabric correto.
- **Chave inválida:** gere uma chave na plataforma da API e confirme o projeto/organização e permissões.
- **Modelo inválido:** escolha um modelo acessível à sua conta; confirme o identificador exato no catálogo da API.
- **Limite/saldo/faturamento:** confira limites e método de pagamento no painel da API. Isso não é corrigido por uma assinatura ChatGPT.
- **Timeout ou sem internet:** confira a conexão, firewall, proxy e acesso HTTPS ao host `api.openai.com`.
- **Erro inesperado:** atualize Fabric Loader/API para versões compatíveis com 26.3 e tente o botão **Testar conexão**. Mensagens de erro não incluem a chave.
- **DPAPI indisponível:** a chave funciona apenas até fechar o cliente. Ela não é gravada em texto puro; cole-a novamente na próxima sessão.

## Créditos e avisos

Ghost é um projeto independente da comunidade. Minecraft é uma marca da Mojang Studios/Microsoft; Fabric é um projeto comunitário; OpenAI e seus produtos pertencem à OpenAI. Este mod não é patrocinado nem endossado por essas empresas.
