# CLAUDE.md

## Projeto
Intruphon — app Android leve e sem rastreamento para baixar mídia de links, para conteúdo próprio, de licença livre ou com permissão.
- Tipo: público · Público-alvo: global
- Nome nos arquivos: `intruphon` (ex: intruphon_v0.1.0.apk; título da Release: "Intruphon v0.1.0")

## Mantenedor
- Nome: rilclesaraiva · Contato: saraivarilcle@gmail.com
- Apoio: https://buymeacoffee.com/saraivarilcle · Licença: GPL-3.0
- País (assinatura): BR
- Segredos ficam em: Google Drive (pasta privada Intruphon/)
- Repositório GitHub: https://github.com/rilclesaraiva/Intruphon

## Stack
- Stack: Kotlin + Jetpack Compose (Material 3)
- Build: `gradle assembleRelease` · Testes: `gradle testDebugUnitTest`
  (sem wrapper por enquanto: o CI usa `gradle/actions/setup-gradle` com Gradle 8.9;
  adicionar o wrapper quando a sessão ou o mantenedor puder gerá-lo)
- Mapa: core → `app/src/main/java/com/intruphon/app/core` · data → `.../data` · features → `.../features`
- Versão: `app/build.gradle.kts` (`versionName`) · lida no CI por `VERSION_CMD`
- Versionamento: SemVer (regra; o número é calculado pelo [Unreleased], nunca escolhido)
- Release: `gradle assembleRelease` → `app/build/outputs/apk/release/*.apk`
- Pacote (Android): `com.intruphon.app`, fixo · R8 ativo no release (sem ofuscação), nunca desligar
- Ícone: `app/src/main/res/drawable/ic_launcher_foreground.xml` + `values/ic_launcher_background.xml`, só muda se o mantenedor pedir
- minSdk 26 (ícone vetorial adaptativo sem PNG de fallback)
- Versões de AGP/Kotlin/Compose foram escolhidas sem poder consultar os repositórios
  (rede bloqueada na sessão); o CI é a primeira prova de que compilam.

## Arquitetura
Três papéis: core (config, tema), data (banco, rede, arquivos — vazio por enquanto), features (home, about).
A entrada (`MainActivity`) registra as telas. Plano de extração: NewPipeExtractor (GPL-3.0) numa feature/`data`
própria quando a primeira versão com download for planejada; sem SDKs de anúncio ou analytics.

## Regras de estrutura
- Feature não importa outra feature; o compartilhado sobe para core/data.
- Navegação entre features passa pela entrada (rotas registradas lá).
- Banco, APIs e arquivos só via data. Core sem regra de negócio.
- Seguir as convenções da stack. Nenhuma dependência nova sem justificar.

## Fluxo de trabalho
- Uma tarefa por vez, mudanças pequenas. Não refatorar fora do escopo.
- Tudo direto na `main`: sem branches (`wip/`, `feature/`…) e sem PR. Cada push
  gera o build para vistoria. Se o ambiente exigir commit ao parar, é na `main`.
- Rodar build e testes antes do push quando o ambiente permitir. Se a sessão não
  consegue compilar (ex: nuvem sem SDK ou sem acesso às dependências), o CI é a
  única prova, e a IA diz isso em vez de afirmar que testou.
- Só dizer que terminou quando o CI daquele commit passar no GitHub.
  CI vermelho é corrigido antes de qualquer tarefa nova.
- Registrar toda mudança em [Unreleased] no CHANGELOG.
- Preferir remover a acumular: o que não é usado sai.
- Na dúvida, perguntar. Nunca inventar API.
- Bloqueio de segurança do ambiente (proxy, permissão, leitura de credencial) nunca
  é contornado: o passo vai para o CI ou para o mantenedor.

## Release (regra absoluta)
- Só com confirmação explícita do mantenedor citando a versão
  ("confirmo release v1.2.0"), na conversa.
  "Ok", "beleza" ou silêncio não contam.
- A versão é calculada pela regra de versionamento. Número pedido fora da regra:
  explicar o número certo e esperar a confirmação com ele.
- Sem confirmação: nada de bump de versão ou mover [Unreleased].
- A IA nunca cria tag nem Release: o commit `chore(release): vX.Y.Z` na main basta,
  e o CI cria a tag e publica quando vê versão nova com seção no CHANGELOG.
- Toda atualização vai para os artefatos do Actions (build para vistoria) e a IA
  avisa com o link. O mantenedor vistoria e só então pede o release público.
- Entre a vistoria e o commit de release, só mudam versão e CHANGELOG; mudou código, nova vistoria.

## Segredos
- Nunca commitar chaves, keystores, senhas ou .env.
- Secrets de assinatura: a IA gera e envia os arquivos direto ao mantenedor, sem
  nunca ler ou exibir o conteúdo. O mantenedor guarda no Google Drive (pasta privada).
  Nunca no repo, nunca escritos no texto do chat.
- Cadastrar secrets no GitHub é sempre tarefa manual do mantenedor.

## Idioma
- README e CHANGELOG: inglês (global).

## Projeto público
- README com a seção de transparência sobre IA (nunca remover) e, no fim, a de
  apoio e contato com os dados de *Mantenedor*.
- No app: nunca citar doação ou pagamento. Só "Quer apoiar o projeto? Entre no
  repositório do projeto" com link para o repo.
- Páginas legais em `docs/` (privacidade, termos), publicadas no GitHub Pages e
  linkadas no README e no app.
- Coleta nova (SDK, permissão, chamada de rede, cookie) → atualizar a política
  no mesmo commit.
- Nenhuma afirmação sem respaldo: o que README, loja e páginas legais dizem
  precisa ser verdade no código. Na dúvida, não afirmar.
- Uso do app: sempre descrito como para conteúdo próprio, de licença livre ou com
  permissão; nunca prometer ou sugerir contornar proteções de terceiros.

## Contexto atual
- Foco: primeiro CI verde; depois planejar a versão com download (NewPipeExtractor).
- Problemas conhecidos: sem wrapper do Gradle; versões de AGP/Kotlin/Compose e das actions não verificadas (CI é a prova).
