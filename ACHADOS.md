# ACHADOS.md

Achados de revisões: **hipóteses ou pendências, não fatos provados**, salvo onde está escrito. Fora do CLAUDE.md de propósito. Ao provar, anotar o resultado ao lado do item (provado / refutado / corrigido, com data e commit) em vez de apagar.

## A. Revisão de 15/11/2026 (revisor de contexto limpo, só leitura, sem compilar nem rodar nada)

1. **Permissão da pasta antiga liberada, deixando itens MISSING** (`SaveLocation`). *Corrigido* em 15/11/2026 (as permissões antigas ficam ao escolher outra pasta).
2. **`choose()` liberava antes de tomar a permissão nova** (`SaveLocation`). *Corrigido* em 15/11/2026 (toma primeiro).
3. **Volta silenciosa ao padrão quando a permissão da pasta some**. *Corrigido* em 15/11/2026 (a linha em Ajustes avisa que a pasta não está mais disponível).
4. **MIME no `DocumentsContract.createDocument`** (`SaveLocation.create`): alguns provedores podem renomear o arquivo com extensão própria (ex.: `.m4a.mp4`). Incerto. *Como provar:* salvar um M4A e um MP4 numa pasta escolhida, no aparelho, e olhar os nomes. Não corrigido de propósito até provar.
5. **Nuances de rede e consulta de atualização** (`UpdateRepository`): a consulta usa escopo próprio com `tryLock`; uma consulta manual enquanto uma automática roda responde "falhou" em vez de esperar. Impacto baixo. *Como provar:* tocar em "Verificar atualizações" assim que o app abre.

## B. Diferenças contra o `estrutura-vibe-coding` (20/11/2026), para o mantenedor decidir

6. **Licenças são um painel dentro do app**, mas o documento diz que texto legal nunca vira tela do app: Sobre → Legal abre `docs/licenses.md` no GitHub Pages, e `docs/` deve ter `licenses.md` (licença do app, bibliotecas, serviços). O Intruphon tem só `docs/privacy.md` e `docs/terms.md`, e a linha Licenças abre um painel de baixo com texto. Precisa de decisão do mantenedor (hoje CLAUDE.md e código concordam entre si; o documento não).
7. **Sem wrapper do Gradle**: `services.gradle.org` é bloqueado na sessão da IA. O mantenedor roda `gradle wrapper --gradle-version 8.9` uma vez e comita o resultado (ver CLAUDE.md, "Limitações desta sessão").
8. **`versionCode` é o número da execução do CI**, não calculado da versão. Mantido de propósito: o v0.1.0 saiu assim, e um valor calculado (100 para 0.1.0) seria menor que o número da execução e impediria a atualização.

## C. Não testado no aparelho (só compilado pelo CI)

Categorias de Ajustes e as escolhas na própria linha, o voltar do `AppSheet` e o `dragOnlyFromTop`, a regra de foco ao fechar o teclado na Home, os novos tamanhos de cache de imagem e, de antes, a escolha de pasta e o fluxo de exclusão. Os testes unitários cobrem só as partes puras (`Selection`, `iconFrame`, codificação do `DownloadStore`, tamanhos do `ImageMemory`).
