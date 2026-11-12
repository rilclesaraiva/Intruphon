# CLAUDE.md

## Projeto
Intruphon — app Android leve e sem rastreamento para baixar mídia de links. O app é livre: o usuário decide o que baixa.
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
- Versão: `app/build.gradle.kts` (`versionName`) · lida no CI por `VERSION_CMD` · `versionCode` = número da execução do CI (`GITHUB_RUN_NUMBER`) e `BuildConfig.BUILD` mostra o build na tela Sobre e nas notas de download
- Versionamento: SemVer (regra; o número é calculado pelo [Unreleased], nunca escolhido)
- Release: `gradle assembleRelease` → `app/build/outputs/apk/release/*.apk`
- Pacote (Android): `com.intruphon.app`, fixo · R8 ativo no release (sem ofuscação), nunca desligar
- Ícone: `app/src/main/res/drawable/ic_launcher_foreground.xml` + `values/ic_launcher_background.xml`, só muda se o mantenedor pedir
- minSdk 29 (ícone vetorial adaptativo sem PNG de fallback; pasta pública sem permissão de armazenamento)
- Sobre: entrada própria nos ajustes (versão e build lidos do build, código-fonte, novidades,
  issues, privacidade, termos, licenças e convite de apoio ao repositório), conforme a seção 9.1
  do estrutura-vibe-coding
- Atualizações: sim, via Releases públicas (`GET /repos/rilclesaraiva/Intruphon/releases/latest`).
  Consulta ao abrir e no máximo a cada 10 min ao voltar, com ETag; "Agora não" só na
  sessão; sino adia 1 semana / 1 mês ou pula a versão; controles em Ajustes → Sistema
- Versões de AGP/Kotlin/Compose foram escolhidas sem poder consultar os repositórios
  (rede bloqueada na sessão); o CI é a primeira prova de que compilam.

## Arquitetura
Três papéis: core (config, tema M3 Expressive, LinkExtractor, `core/ui`: PageHeader, botão flutuante, cartões de seção), data (net: OkHttpDownloader; media: MediaRepository com NewPipeExtractor; download: DownloadEngine (fila, StateFlow), HttpFileDownloader (OkHttp em partes de 10 MiB + MediaStore), MediaDownloader (executa o Plan: direto, M4A limpo ou junção vídeo+áudio no cache), MediaMuxing (MediaMuxer), Mp4Tags (tags iTunes em M4A/MP4), Id3v2 + Mp3Encoder (MediaCodec decodifica, LAME em Java codifica), CoverArt (capa quadrada) e MediaPreview (capa/duração da aba Downloads), `data/settings` (AppSettings: só Wi-Fi, downloads simultâneos), `data/stats` (SpeedLog: velocidade por formato/qualidade, só no aparelho + logcat `IntruphonSpeed`), `data/net/NetworkKind`, `data/update` (UpdateRepository: última Release pública do GitHub com ETag, escolhas do sino; SemVer e política de quando avisar), DownloadService (primeiro plano, dataSync), DownloadNotifications e DownloadStore com a lista em SharedPreferences), features (home, about = Sobre, settings, update = painel de atualização, download = sheet de formatos com mínima/máxima, downloads = biblioteca).
Entradas: `IntruphonApp` (inicia o DownloadEngine), `MainActivity` (app: pilha de páginas abertas a partir da Home (Downloads, Ajustes, Sobre), cada uma em `SaveableStateProvider`, com o voltar preditivo; monta o aviso de atualização) e `ShareActivity` (destino do compartilhar; tema translúcido, só mostra a sheet de formatos por cima do app de origem). A sheet de formatos vive em `features/download` e é usada pelas duas entradas; `home` e `downloads` só recebem callbacks (a entrada abre a sheet, também para "tentar de novo").
Pastas no aparelho: `Download/Intruphon/Audio` e `Download/Intruphon/Video`, gravadas via MediaStore pelo próprio app (sem permissão de armazenamento no Android 10+). Permissões: INTERNET, ACCESS_NETWORK_STATE (Wi-Fi e log de velocidade), POST_NOTIFICATIONS (pedida ao tocar em Download no Android 13+; o download segue se negar), FOREGROUND_SERVICE e FOREGROUND_SERVICE_DATA_SYNC. O DownloadManager do sistema foi abandonado (ficava em "Waiting").
Dependências justificadas: java-lame vendorizado em `app/src/main/java/net/sourceforge/lame` (LGPL, port Java do LAME; o Android não tem encoder MP3 e assim não há biblioteca nativa; licença em `third_party/java-lame`), NewPipeExtractor (extração, GPL-3.0 como o projeto), OkHttp (o extrator exige um cliente HTTP do app), coroutines (carga fora da thread principal), desugar_jdk_libs_nio (o extrator exige abaixo do Android 13).
R8: as classes do extrator e do Rhino ficam com `-keep` em `proguard-rules.pro` (reflexão e JS embutido); o R8 continua ligado.
Sem SDKs de anúncio ou analytics. Chamadas de rede do app além do link do usuário: Releases do GitHub (atualização) e a foto de perfil do GitHub (tela Sobre).

## Interface (regras do mantenedor)
- Material 3 Expressive: formas grandes e arredondadas, tipografia marcante, movimento com mola.
- Nunca barra superior fixa (nem TopAppBar, nem abas no topo) e nunca barra inferior. O título de cada
  página rola com o conteúdo e fica sempre ao lado do botão flutuante, nunca por baixo dele.
- A Home é única: link, botão Download e, abaixo, o histórico breve dos downloads recentes, com "Ver
  todos" para a página Downloads. Ajustes, Downloads e Sobre são páginas abertas a partir da Home.
- Botão de ajustes flutuante no canto superior direito da Home; nas outras páginas, no canto
  superior esquerdo fica o botão flutuante de voltar. Os dois são levemente translúcidos e pertencem à
  própria página (deslizam com ela, nunca ficam fixos sobre a tela inteira). Todas as páginas da pilha
  ficam compostas com `key(página)`, para não perder rolagem nem miniaturas.
- Home: os recentes enchem a tela até o fim (a quantidade que cabe no aparelho, mínimo 3); tocar fora do campo tira o foco; sem texto de dica; o campo do link tem botão de colar à direita, que vira X (limpar tudo) quando há texto.
- Voltar preditivo (opção em Ajustes → Aparência, ligada por padrão; `enableOnBackInvokedCallback` no
  manifesto): só em páginas abertas a partir da Home. No gesto, a página atual acompanha o dedo para a
  direita com sombra e cantos que arredondam até 28 dp logo no começo; a anterior aparece por baixo,
  deslocada 20% para a esquerda, com véu escuro de 32% que clareia ao arrastar. Ao soltar, a página
  termina de sair (`AppMotion.SHORT`) e a troca acontece sem nova animação; ao cancelar, volta ao
  lugar. O botão de voltar toca a mesma saída e ignora toque durante a animação. Com a opção desligada,
  o voltar comum faz a mesma saída sem acompanhar o dedo. A tela de aviso de atualização trata o voltar
  à parte ("Agora não"). A duração ignora a escala de animação do sistema.
- Excluir um download pergunta: remover só da lista ou apagar também do aparelho. Apagar do aparelho
  usa o pedido de exclusão do próprio sistema (sem permissão de armazenamento).
- Toda página lembra a posição em que foi deixada (rolagem e campos) ao sair e voltar, em qualquer
  lugar do app. A entrada envolve cada página em `SaveableStateProvider`; a página guarda rolagem e
  campos com `rememberScrollState`, `rememberLazyListState` ou `rememberSaveable`. Página nova só
  entra se seguir isso.
- Playlist: link de playlist (`/playlist?list=`) abre a sheet de playlist (itens com miniatura e duração, selecionar todos, um formato para a lista toda); cada item é lido e enfileirado em ordem pelo `DownloadEngine.enqueueAll`.
- Downloads: abas em pílula no topo da lista (Todos, padrão; Áudio; Vídeo).
- Ajustes: seção Atualizações com duas linhas (verificar, mostrando a versão; avisos, com diálogo: normal, 1 semana, 1 mês).
- Idiomas: inglês (padrão), espanhol e português do Brasil, seguindo o idioma do Android. Em Ajustes há
  um botão de idioma que abre a tela de idioma do app do próprio Android (Android 13+; antes disso, a
  tela de idioma do sistema). Todo texto novo entra nos três arquivos de strings (`values`, `values-es`,
  `values-pt-rBR`).
- Sobre: ícones das linhas como no outro app do mantenedor (código, selo, inseto, cadeado, artigo,
  coração) e a foto do GitHub do desenvolvedor (baixada e guardada no cache; sem foto, um ícone).
- Pop-up de download como o do SnapTube: duas opções de áudio e duas de vídeo (a mínima e a máxima);
  o resto só em "Mais formatos". O botão Download fica sempre visível.

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
- Uso do app: o mantenedor não definiu restrição de direitos autorais; README, loja e app não afirmam
  que o uso é só para conteúdo próprio, livre ou com permissão. Nunca prometer ou sugerir contornar
  proteções de terceiros.

## Contexto atual
- Foco: vistoria da nova interface (Home com histórico, voltar preditivo, exclusão, pop-up mínima/máxima, Sobre, aviso de atualização); o release v0.2.0 espera a confirmação do mantenedor. Depois novas fontes além do YouTube e wrapper do Gradle.
- Problemas conhecidos: sem wrapper do Gradle; testado no aparelho só com YouTube (áudio M4A/MP3, vídeo 1080p, tags, capa, biblioteca); nova interface, voltar preditivo, exclusão pelo sistema, Sobre e aviso de atualização só compilados pelo CI; sem Release pública ainda, então o aviso de atualização não achou nada para mostrar; a conversão MP3 em Java pode ser lenta em músicas longas.
