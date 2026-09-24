# ExpansionAE — desenvolvimento para Minecraft 1.16.5

**Addon experimental para Applied Energistics 2. O objetivo é reimplementar e consolidar funcionalidades selecionadas de vários addons AE2 em uma única base coerente para 1.16.5.**

Alvo: Minecraft 1.16.5, Forge 36.2.42 e Applied Energistics 2 8.4.7. O mod usa o ID `expansionae`. Não substitui nem incorpora o AE2.

## Estado do projeto

O repositório está na fase de arquitetura e implementação incremental. O plano não é portar integralmente nenhum dos projetos de referência, mas adaptar apenas os sistemas escolhidos para a API do AE2 8.4.x.

- [PORT_STATUS.md](PORT_STATUS.md): matriz de funcionalidades, dificuldade e fases propostas.
- [ARCHITECTURE.md](ARCHITECTURE.md): arquitetura do core, módulos opcionais e storage channels.
- [NOTICE.md](NOTICE.md): projetos de referência, créditos e licenças observadas.
- [DISK_RECOVERY.md](DISK_RECOVERY.md): diagnóstico read-only, export e procedimento seguro de backup para DISKs em quarentena.

### Vertical slice atual: DISK

O primeiro módulo funcional em validação é a família de armazenamento DISK inspirada no comportamento do AE2Things e reimplementada para a API do AE2 8.4.x:

- tiers de itens 1k / 4k / 16k / 64k;
- capacidades de 1.000 / 4.000 / 16.000 / 64.000 itens, com 1 item = 1 unidade;
- sem limite artificial de tipos de item;
- persistência externa por UUID via `WorldSavedData`;
- aliases do mesmo UUID representam o mesmo armazenamento lógico;
- integração com ME Drive, ME Chest, storage grid e Cell Workbench;
- suporte a FUZZY e INVERTER; CAPACITY não é suportado;
- validação automatizada de restart, chunk/Drive lifecycle, aliases entre grids, receitas, modelos e tooltips.

Para este primeiro backport, recipe/progressão e identidade visual técnica dos quatro tiers já estão definidas e implementadas. A PR permanece em validação apenas pela passagem manual de UX/visual no cliente. Consulte `PORT_STATUS.md` e `DEVELOPMENT.md` para o estado técnico detalhado.

## Compilar no Windows

1. Instale um **JDK 8 de 64 bits**, como Eclipse Temurin 8. Configure `JAVA_HOME` para a pasta do JDK, não a pasta `bin`.
2. Extraia/clone o projeto e abra um terminal nessa pasta.
3. Execute `gradlew.bat --no-daemon clean build`.
4. Se a compilação concluir com `BUILD SUCCESSFUL`, o JAR reobfuscado ficará em `build\\libs`.

No Linux/macOS: `./gradlew --no-daemon clean build`.

O repositório inclui o Gradle Wrapper 7.3.3; não é necessário instalar Gradle separadamente. A GitHub Actions usa o mesmo wrapper para evitar diferenças entre CI e desenvolvimento local.

A primeira execução precisa de internet para obter Gradle, Forge, Minecraft e AE2.

## Execução e validação

Use `gradlew.bat runClient` no Windows ou `./gradlew runClient` no Linux/macOS para abrir o ambiente de desenvolvimento. Em uma instalação normal, a base será Forge 36.2.42 + AE2 8.4.7 + ExpansionAE.

Uma compilação aprovada não comprova paridade funcional. Use um mundo de teste até concluir testes de inventário, autocrafting, persistência, reload de chunks e servidor dedicado.
