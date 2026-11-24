# CLAUDE.md

Lido no início de cada sessão. Em conflito entre uma instrução antiga do chat e este arquivo, vale este arquivo. Decisão nova que mude algo aqui entra neste arquivo no mesmo commit. Segue o documento de estrutura de 20/11/2026.

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
- Build: `./gradlew assembleRelease` · Testes: `./gradlew testDebugUnitTest`
  (wrapper no repositório; a versão do Gradle, 8.9, mora só em `gradle/wrapper/gradle-wrapper.properties`;
  os arquivos do wrapper vieram do repositório do BoomListener, do mesmo mantenedor, porque o proxy da
  sessão bloqueia `services.gradle.org`)
- Mapa: core → `app/src/main/java/com/intruphon/app/core` · data → `.../data` · features → `.../features`
- Versão: `gradle.properties` (`APP_VERSION`, único lugar; `versionName` lê dali) · lida no CI por `VERSION_CMD` · versões das libs só em `gradle/libs.versions.toml` · `versionCode` = número da execução do CI (`GITHUB_RUN_NUMBER`) e `BuildConfig.BUILD` mostra o build na tela Sobre e nas notas de download
- Versionamento: SemVer (regra; o número é calculado pelo [Unreleased], nunca escolhido)
- Release: `./gradlew assembleRelease` → `app/build/outputs/apk/release/*.apk`
- Pacote (Android): `com.intruphon.app`, fixo · R8 ativo no release (sem ofuscação), nunca desligar
- Ícone: `app/src/main/res/drawable/ic_launcher_foreground.xml` (+ `ic_launcher_monochrome.xml` para o ícone temático) + `values/ic_launcher_background.xml`, só muda se o mantenedor pedir. Desenho: intruso espiando atrás de um funil de 3 barras, uma cor (#C7DDE8) sobre #0C1620, um pouco maior que a zona segura de propósito. O Sobre desenha o mesmo ícone em Compose (`features/about/AboutIcon.kt`, formas em coordenadas da arte + `ART_K/ART_TX/ART_TY`) e o anima: olha esquerda, direita, para o usuário e assenta no funil; toca ao abrir o Sobre e a cada toque. Mudou o desenho, atualizar os dois
- minSdk 29 (ícone vetorial adaptativo sem PNG de fallback; pasta pública sem permissão de armazenamento)
- Sobre: entrada própria nos ajustes (versão e build lidos do build, código-fonte, novidades,
  issues, privacidade, termos, licenças (as três abrem no navegador, nunca viram tela do app) e convite de apoio ao repositório), conforme a seção 9.1
  do estrutura-vibe-coding
- Atualizações: sim, via Releases públicas (`GET /repos/rilclesaraiva/Intruphon/releases/latest`).
  Consulta ao abrir e no máximo a cada 10 min ao voltar, com ETag; "Agora não" só na
  sessão; sino adia 1 semana / 1 mês ou pula a versão; controles em Ajustes → Sistema
- Versões de AGP/Kotlin/Compose foram escolhidas sem poder consultar os repositórios
  (rede bloqueada na sessão); o CI é a primeira prova de que compilam.

## Arquitetura
Três papéis: core (config, tema M3 Expressive, LinkExtractor, `ImageMemory` (tamanho dos caches de imagem em KB), `core/ui`: PageHeader, botão flutuante, cartões de seção, `Motion` (molas nomeadas), `AppSheet` (única forma de abrir sheet), `OptionRow` (opção que abre na própria linha), `Selection` (estado de seleção testado), `Keyboard` (campo perde o foco quando o teclado fecha)), data (net: OkHttpDownloader; media: MediaRepository com NewPipeExtractor; download: DownloadEngine (fila, StateFlow), HttpFileDownloader (OkHttp em partes de 10 MiB + MediaStore), MediaDownloader (executa o Plan: direto, M4A limpo ou junção vídeo+áudio no cache), MediaMuxing (MediaMuxer), Mp4Tags (tags iTunes em M4A/MP4), Id3v2 + Mp3Encoder (MediaCodec decodifica, LAME em Java codifica), CoverArt (capa quadrada) e MediaPreview (capa/duração da aba Downloads), `data/settings` (AppSettings: só Wi-Fi, downloads simultâneos), `data/stats` (SpeedLog: velocidade por formato/qualidade, só no aparelho + logcat `IntruphonSpeed`), `data/net/NetworkKind`, `data/update` (UpdateRepository: última Release pública do GitHub com ETag, escolhas do sino; SemVer e política de quando avisar), DownloadService (primeiro plano, dataSync), DownloadNotifications e DownloadStore com a lista em SharedPreferences), features (home, search = busca por nome, about = Sobre, settings (início só com categorias + páginas Aparência, Downloads e Sistema), update = painel de atualização, download = sheet de formatos com mínima/máxima, downloads = biblioteca).
Entradas: `IntruphonApp` (inicia o DownloadEngine), `MainActivity` (app: pilha de páginas abertas a partir da Home (Downloads, Ajustes e suas categorias, Sobre), cada uma em `SaveableStateProvider`, com o voltar preditivo; monta o aviso de atualização) e `ShareActivity` (destino do compartilhar; tema translúcido; link: só mostra a sheet de formatos por cima do app de origem; texto sem link: abre a página Busca da `MainActivity`). A sheet de formatos vive em `features/download` e é usada pelas duas entradas; `home` e `downloads` só recebem callbacks (a entrada abre a sheet, também para "tentar de novo").
Pastas no aparelho: `Download/Intruphon/Audio` e `Download/Intruphon/Video`, gravadas via MediaStore pelo próprio app (sem permissão de armazenamento no Android 10+). Em Ajustes o usuário pode escolher outra pasta (seletor de pastas do Android, `ACTION_OPEN_DOCUMENT_TREE`, permissão persistente; `data/download/SaveLocation` cria Audio/Video lá, apaga e indexa); se a permissão se perder, volta ao padrão. Downloads simultâneos: um limite para Wi-Fi e outro para dados móveis (1 a 10), vale o da conexão atual. Permissões: INTERNET, ACCESS_NETWORK_STATE (Wi-Fi e log de velocidade), POST_NOTIFICATIONS (pedida ao tocar em Download no Android 13+; o download segue se negar), FOREGROUND_SERVICE e FOREGROUND_SERVICE_DATA_SYNC. O DownloadManager do sistema foi abandonado (ficava em "Waiting").
Dependências justificadas: java-lame vendorizado em `app/src/main/java/net/sourceforge/lame` (LGPL, port Java do LAME; o Android não tem encoder MP3 e assim não há biblioteca nativa; licença em `third_party/java-lame`), NewPipeExtractor (extração, GPL-3.0 como o projeto), OkHttp (o extrator exige um cliente HTTP do app), coroutines (carga fora da thread principal), desugar_jdk_libs_nio (o extrator exige abaixo do Android 13).
R8: as classes do extrator e do Rhino ficam com `-keep` em `proguard-rules.pro` (reflexão e JS embutido); o R8 continua ligado.
Sem SDKs de anúncio ou analytics. Chamadas de rede do app além do link do usuário: Releases do GitHub (atualização) e a foto de perfil do GitHub (tela Sobre).

## Interface (regras do mantenedor)
- Material 3 Expressive: formas grandes e arredondadas, tipografia marcante, movimento com mola.
- Nunca barra superior fixa (nem TopAppBar, nem abas no topo) e nunca barra inferior. O título de cada
  página rola com o conteúdo e fica sempre ao lado do botão flutuante, nunca por baixo dele.
  Exceção: barras de busca podem ser fixas no topo, como no YouTube (2026-11-24); a regra vale para barras de título. Em dúvida se algo é barra de busca ou de título, perguntar ao mantenedor.
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
  lugar. O botão de voltar toca a mesma saída. Toque durante uma animação nunca é descartado: conclui a anterior na hora e começa a nova (abrir depressa uma categoria logo após Ajustes, ou voltar duas vezes seguidas, funciona sem esperar); só o toque na mesma página que já está abrindo é ignorado, e nada abre enquanto o dedo arrasta o gesto. Com a opção desligada,
  o voltar comum faz a mesma saída sem acompanhar o dedo. A tela de aviso de atualização trata o voltar
  à parte ("Agora não"). A duração ignora a escala de animação do sistema.
- Excluir um download pergunta: remover só da lista ou apagar também do aparelho. Apagar do aparelho
  usa o pedido de exclusão do próprio sistema (sem permissão de armazenamento).
- Toda página lembra a posição em que foi deixada (rolagem e campos) ao sair e voltar, em qualquer
  lugar do app. A entrada envolve cada página em `SaveableStateProvider`; a página guarda rolagem e
  campos com `rememberScrollState`, `rememberLazyListState` ou `rememberSaveable`. Página nova só
  entra se seguir isso.
- Busca por nome: texto sem link na Home (o botão vira "Buscar") abre a página Busca (`features/search/SearchScreen`, aberta pela entrada como as outras páginas); o texto compartilhado sem link vai pela `ShareActivity`, que abre a `MainActivity` (`launchMode="singleTask"`, extra `EXTRA_SEARCH`, `onNewIntent`) em vez de uma sheet. A página é como a do YouTube: campo fixo no topo, ao lado do botão de voltar (exceção da regra de barra fixa: é barra de busca); com o campo em edição lista pesquisas parecidas (`MediaRepository.suggestions`, só YouTube); ao enviar a busca lista os resultados em abas em pílula, Todos (padrão), Vídeos e Músicas (`MediaRepository.search`, primeira página); Todos junta as duas buscas (`data/media/SearchMerge`: um vídeo, uma música, alternando; faixa nas duas listas aparece uma vez, como música) e só nele cada item leva a pílula Áudio ou Vídeo; deslizar o dedo para os lados troca de aba. Tocar num resultado abre a `DownloadSheet` pela entrada; link digitado na busca vai direto à sheet. Só compilado pelo CI; não provado no aparelho.
- Home: o campo tem sempre a mesma altura, com ou sem foco; o rótulo ("Cole o link aqui ou o nome de uma música") é uma linha pequena que nunca quebra; a tecla Enter do teclado faz o mesmo que o botão (buscar ou baixar), e na página Busca ela envia a busca.
- Playlist: link de playlist (`/playlist?list=`) abre a sheet de playlist (itens com miniatura e duração, selecionar todos, um formato para a lista toda); cada item é lido e enfileirado em ordem pelo `DownloadEngine.enqueueAll`.
- Downloads: abas em pílula no topo da lista (Todos, padrão; Áudio; Vídeo), e deslizar o dedo para os lados troca de aba (`core/ui/TabSwipe`, o mesmo da Busca); um cartão "Baixando (N)" agrupa os ativos e abre a página Baixando (cancelar um ou todos); falhas ficam numa seção no topo; capa aparece já durante o download. Download concluído mostra só "Concluído" (nunca "Salvo em…" nem aviso de tags e capa). Detalhes (⋮ → Detalhes) é um passo do painel (ações → Detalhes, `features/downloads/DetailsStep`, lido do arquivo por `data/download/FileDetails`): local, formato, tamanho, duração, taxa de bits, taxa de amostragem, resolução e quadros (vídeo), modificado em, link de origem.
- Tela cheia: as páginas desenham atrás das barras do sistema (sem barra sólida); cabeçalho e rodapé respeitam os insets.
- Ajustes: a página inicial tem só as categorias (Aparência, Downloads, Sistema, cada uma com ícone de cor
  própria e uma linha-resumo) e, embaixo, "Sobre o Intruphon"; nada solto. Cada categoria é uma página aberta
  a partir dela (pela entrada). Toda opção mora numa categoria; se nenhuma serve, perguntar. Aparência: tema,
  cores do sistema, voltar preditivo, idioma. Downloads: só Wi-Fi, limites simultâneos, pasta. Sistema:
  Atualizações (verificar, mostrando a versão; avisos: normal, 1 semana, 1 mês) e registro de velocidade.
  Opção com lista de escolhas abre na própria linha (`ExpandableOptionRow` + `ChoiceRow`), começa fechada
  e com mola; opção que precisa de explicação ou de uma janela do sistema (pasta) usa o painel de baixo.
- Idiomas: inglês (padrão), espanhol e português do Brasil, seguindo o idioma do Android. Em Ajustes há
  um botão de idioma que abre a tela de idioma do app do próprio Android (Android 13+; antes disso, a
  tela de idioma do sistema). Todo texto novo entra nos três arquivos de strings (`values`, `values-es`,
  `values-pt-rBR`).
- Sobre: ícones das linhas como no outro app do mantenedor (código, selo, inseto, cadeado, artigo,
  coração) e a foto do GitHub do desenvolvedor (baixada e guardada no cache; sem foto, um ícone).
- Pop-up de download como o do SnapTube: duas opções de áudio e duas de vídeo (a mínima e a máxima);
  o resto só em "Mais formatos". O botão Download fica sempre visível.

- Memória de posição: página que sai da pilha de vez (voltar) esquece rolagem e campos (`pages.removeState`); só lembra enquanto outra página aberta a partir dela está por cima.
- Downloads: toque na linha abre (falha tenta de novo); segurar ou ⋮ abre o painel inferior; toque só na capa inicia a seleção (pílulas no topo: fechar+contagem, selecionar todos+mais; o botão de voltar some); sem botões "Abrir"/"Excluir arquivo". Excluir: pergunta remover da lista ou apagar do aparelho, depois janela própria de confirmação (o Android não mostra a dele para arquivos do próprio app).
- Painel em passos (`core/ui/BottomPanelFlow`): o passo atual desliza para a direita e some, o próximo chega do mesmo lado com mola leve; do 2º passo em diante há um mini botão voltar no canto superior esquerdo (e o voltar do sistema) que desfaz o passo do mesmo jeito. O painel tem a altura do passo mostrado (no máx. 70% da tela) e cresce ou encolhe com mola ao trocar de passo, sem espaço vazio embaixo. Downloads usa: ações → excluir? → confirmação.
- Janelas: nunca no meio da tela (sem AlertDialog); tudo sobe de baixo com `core/ui/BottomPanel` + `PanelAction`. Toda sheet usa `core/ui/AppSheet`, nunca `ModalBottomSheet` direto: o voltar desce o painel com a animação normal (sem a de encolher do sistema) e `dragOnlyFromTop` deixa só a faixa do puxador arrastar (usado na playlist, para não perder a seleção). Arquivo já apagado por fora: só "remover da lista" (checado de novo ao abrir o painel/fluxo de exclusão).
- Movimento: tudo que se move usa as molas nomeadas de `AppMotion` (`spatial`, `fastSpatial`, `effects`), com os valores do M3 Expressive escritos ali porque o Material 3 fixado não tem `MotionScheme`; nenhum número solto nas telas. Única exceção: a saída de página (`AppMotion.SHORT`, tween fixo, para o gesto, o botão e o voltar terminarem iguais).
- Seleção em lista: estado em `core/ui/Selection` (testado); a tela só liga as ações. Campo de texto leva `clearFocusWhenKeyboardCloses()`.
- Imagens: os caches (miniaturas e capas) são medidos em KB de imagem, 1/16 da memória do app (`ImageMemory`), não por número de itens.
- Playlist: itens indisponíveis são sondados antes, aparecem escurecidos com "!" e são ignorados em silêncio.

## Regras de estrutura
- Feature não importa outra feature; o compartilhado sobe para core/data.
- Navegação entre features passa pela entrada (rotas registradas lá).
- Banco, APIs e arquivos só via data. Core sem regra de negócio.
- Seguir as convenções da stack. Nenhuma dependência nova sem justificar; antes de usar uma versão nova, conferir na documentação oficial que ela existe e o que exige.
- Dados da pessoa já guardados (lista de downloads, ajustes): mudança de formato sempre com migração escrita e testada; nunca apagar dados para "começar limpo".

## Fluxo de trabalho
- Uma tarefa por vez, mudanças pequenas. Não refatorar fora do escopo.
- Tudo direto na `main`: sem branches (`wip/`, `feature/`…) e sem PR. Cada push
  gera o build para vistoria. Se o ambiente exigir commit ao parar, é na `main`.
- Rodar build e testes antes do push quando o ambiente permitir. Se a sessão não
  consegue compilar (ex: nuvem sem SDK ou sem acesso às dependências), o CI é a
  única prova, e a IA diz isso em vez de afirmar que testou.
- Só dizer que terminou quando o CI daquele commit passar no GitHub.
  CI vermelho é corrigido antes de qualquer tarefa nova. Quando falha, o CI posta o erro como
  comentário no commit: `gh api repos/rilclesaraiva/Intruphon/commits/<sha>/comments`.
- Todo pedido vira tarefa na lista de tarefas da sessão (a ferramenta de tarefas, não a memória), antes
  de começar: um item por pedido, mesmo pequeno. Antes de dizer que terminou, reler a mensagem original e
  conferir item por item. O que ficar pendente vai para *Contexto atual* (ou vira issue).
- Registrar toda mudança em [Unreleased] no CHANGELOG. Ler só o topo dele (até a primeira versão
  publicada); arquivos de `changelog/` só se o mantenedor perguntar sobre uma versão antiga.
- Bug que a IA não consegue reproduzir: log primeiro, correção depois. Pedir o registro (ou criar um modo
  de registrar) antes de mexer no código; só chamar de causa o que o log sustentar, o resto é hipótese e é
  dito como hipótese. Perguntar se outro app tem o mesmo sintoma (pode ser do sistema).
- Achados de revisão ficam em `ACHADOS.md` (fora deste arquivo), com o resultado ao lado quando provados
  ("provado / refutado / corrigido", data e commit), sem apagar. Não agir sobre eles sem o mantenedor pedir.
- Lógica de `data/` e estados de tela com regra própria ganham teste unitário; o que só se prova no aparelho
  é dito como não testado.
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
- Passos após a confirmação: (0) revisão com contexto limpo: um subagente lê só este arquivo e o diff desde o
  último release e aponta falhas de segurança, quebra das regras de estrutura e itens do CHANGELOG que não
  estão no código; o mantenedor decide entre lançar assim ou corrigir antes (o que exige novo build e nova
  vistoria); o que ficar sem correção e sem prova vai para `ACHADOS.md`. (1) mover [Unreleased] para `## [X.Y.Z] - data`. (2) se o CHANGELOG.md passar de 300 linhas,
  mover as versões anteriores (menos a nova) para `changelog/vA-vB.md`, que nunca mais muda, e terminar o CHANGELOG.md com `## [Earlier versions]` e o link para `changelog/` (o título impede o link de entrar nas notas da Release); deixar um `[Unreleased]` vazio. (3) subir
  `APP_VERSION` em `gradle.properties` (o `versionCode` vem do número da execução do CI e só sobe; não passar
  a calculá-lo da versão, porque o v0.1.0 já saiu com o número da execução e um valor menor impediria a
  atualização). (4) commit `chore(release): vX.Y.Z` na main.
- Sem confirmação: nada de bump de versão ou mover [Unreleased].
- A IA nunca cria tag nem Release: o commit `chore(release): vX.Y.Z` na main basta,
  e o CI cria a tag e publica quando vê versão nova com seção no CHANGELOG.
- Toda atualização vai para os artefatos do Actions (build para vistoria) e a IA
  avisa com o link da execução do workflow `CI` **daquele commit**: conferir o nome e o commit antes de mandar, nunca a do "pages build and deployment", que também aparece na lista quando `docs/` muda. O mantenedor vistoria e só então pede o release público.
- Ferramenta temporária de teste ou diagnóstico (registro de log, botão de teste) nunca sai em release pública: antes do commit de release ela é removida por inteiro, o que gera nova build e nova vistoria.
- Entre a vistoria e o commit de release, só mudam versão e CHANGELOG; mudou código, nova vistoria.

## Segredos
- Nunca commitar chaves, keystores, senhas ou .env.
- Secrets de assinatura: a IA gera e envia os arquivos direto ao mantenedor, sem
  nunca ler ou exibir o conteúdo. O mantenedor guarda no Google Drive (pasta privada).
  Nunca no repo, nunca escritos no texto do chat.
- Cadastrar secrets no GitHub é sempre tarefa manual do mantenedor.

## Idioma
- README e CHANGELOG: inglês (global).
- Textos visíveis do app sempre nos arquivos de idioma, nunca escritos no código.

## Projeto público
- README com a seção de transparência sobre IA (nunca remover) e, no fim, a de
  apoio e contato com os dados de *Mantenedor*.
- No app: nunca citar doação ou pagamento. Só "Quer apoiar o projeto? Entre no
  repositório do projeto" com link para o repo.
- Páginas legais em `docs/` (privacidade, termos, licenças), publicadas no GitHub Pages e
  linkadas no README e no app.
- Coleta nova (SDK, permissão, chamada de rede, cookie) → atualizar a política
  no mesmo commit. Biblioteca nova → `docs/licenses.md` no mesmo commit.
- Nenhuma afirmação sem respaldo: o que README, loja e páginas legais dizem
  precisa ser verdade no código. Na dúvida, não afirmar.
- Uso do app: o mantenedor não definiu restrição de direitos autorais; README, loja e app não afirmam
  que o uso é só para conteúdo próprio, livre ou com permissão. Nunca prometer ou sugerir contornar
  proteções de terceiros.

## Limites do produto (não mudam)
- Sem SDKs de anúncio ou analytics. Sem permissão de armazenamento (pasta pública via MediaStore ou seletor de pastas). Chamadas de rede além do link do usuário: só as Releases do GitHub (atualização) e a foto de perfil do GitHub (Sobre). Mudar algo aqui só com o mantenedor.

## Decisões mantidas (não propor mudar sem o mantenedor pedir)
- `versionCode` = número da execução do CI (2026-11-15): o v0.1.0 já saiu assim, e um valor menor impediria a atualização; não passar a derivá-lo da versão.

## Decisões recusadas (não propor de novo sem o mantenedor pedir)
Toda ideia recusada (da IA ou de usuário) entra aqui no mesmo commit, com data e motivo. Antes de propor algo, conferir esta lista.
- Cabeçalho fixo de lista e barra de rolagem rápida, do BoomListener (2026-11-20): o mantenedor não os escolheu entre os itens de UI.
- Backup e restauração via SAF, do BoomListener (2026-11-20): o mantenedor não o escolheu entre os itens de estrutura.
- Estrutura multimódulo do Gradle com `build-logic` (2026-11-20): não adotada; peso demais para um app deste tamanho.

- Download paralelo por arquivo e aria2 (2026-11-24): o registro de velocidade mostrou 106 de 111 downloads em áudio de uma parte só, e o tempo pesado está na conversão e na junção, não no download. Só interessa melhorar a conversão.

## Limitações desta sessão
- A rede da sessão bloqueia `services.gradle.org`: nada que baixe o Gradle roda aqui (o wrapper só foi provado pelo CI).
- Sem acesso às dependências, a sessão não compila: o CI é a única prova. Nada novo foi testado no aparelho pela IA.

## Contexto atual
- Foco: v0.1.0 publicada (Release no GitHub). Próximos: novas fontes além do YouTube. Cores: o app usa as próprias, tiradas do ícone; "Usar cores do sistema" (Android 12+) vem desligado por padrão.
- Problemas conhecidos: Ajustes em categorias, painel recolhível, `AppSheet`, `Selection`, caches por KB e o quadro puro do ícone só compilados pelo CI; testado no aparelho só com YouTube (áudio M4A/MP3, vídeo 1080p, tags, capa, biblioteca); a interface nova vem sendo vistoriada pelo mantenedor no aparelho a cada build (o que ele não comentou, a IA não dá como testado); exclusão pelo sistema, Sobre e aviso de atualização só compilados pelo CI; a v0.1.0 é a última Release pública, então o aviso de atualização ainda não teve versão nova para mostrar; a conversão MP3 em Java pode ser lenta em músicas longas.
