# ExpansionAE — desenvolvimento para Minecraft 1.16.5

**Addon experimental para Applied Energistics 2. O objetivo é reimplementar e consolidar funcionalidades selecionadas de vários addons AE2 em uma única base coerente para 1.16.5.**

Alvo: Minecraft 1.16.5, Forge 36.2.42 e Applied Energistics 2 8.4.7. O mod usa o ID `expansionae`. Não substitui nem incorpora o AE2.

## Estado do projeto

O scaffold Forge/Gradle está presente e o primeiro objetivo de CI é comprovar que ExpansionAE compila em Java 8 e recebe corretamente a API do AE2 8.4.7 por meio de `@AEAddon`.

- [PORT_STATUS.md](PORT_STATUS.md): matriz de funcionalidades, dificuldade e fases propostas.
- [ARCHITECTURE.md](ARCHITECTURE.md): arquitetura do core, módulos opcionais e storage channels.
- [NOTICE.md](NOTICE.md): projetos de referência, créditos e licenças observadas.

## Compilar no Windows

1. Instale um **JDK 8 de 64 bits**, como Eclipse Temurin 8. Configure `JAVA_HOME` para a pasta do JDK, não a pasta `bin`.
2. Clone ou extraia o projeto e abra um terminal nessa pasta.
3. Execute `gradlew.bat --no-daemon clean build`.
4. Se a compilação concluir com `BUILD SUCCESSFUL`, o JAR reobfuscado ficará em `build\libs`.

No Linux/macOS: `./gradlew --no-daemon clean build`.

A primeira execução precisa de internet para obter Gradle, Forge, Minecraft e AE2. Não é necessário instalar Gradle separadamente.

## Execução e validação

Use `gradlew.bat runClient` para abrir o ambiente de desenvolvimento. Para servidor de desenvolvimento, use `gradlew.bat runServer`.

Em uma instalação normal, a base é Forge 36.2.42 + AE2 8.4.7 + ExpansionAE.

Uma compilação aprovada não comprova paridade funcional. Use um mundo de teste até concluir testes de inventário, autocrafting, persistência, reload de chunks e servidor dedicado.
