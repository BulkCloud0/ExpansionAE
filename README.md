# ExpansionAE — desenvolvimento para Minecraft 1.16.5

**Port incompleto e experimental. Este repositório ainda não contém o port completo de ExtendedAE e AdvancedAE.**

Alvo: Minecraft 1.16.5, Forge 36.2.42 e Applied Energistics 2 8.4.7. O mod usa o ID `expansionae`. Não substitui nem incorpora o AE2.

## Compilar no Windows

1. Instale um **JDK 8 de 64 bits**, como Eclipse Temurin 8. Configure `JAVA_HOME` para a pasta do JDK, não a pasta `bin`.
2. Extraia o projeto e abra um terminal nessa pasta.
3. Execute `gradlew.bat --no-daemon clean build`.
4. Se a compilação concluir com `BUILD SUCCESSFUL`, o JAR reobfuscado ficará em `build\libs`.

No Linux/macOS: `./gradlew --no-daemon clean build`.

A primeira execução precisa de internet para obter Gradle, Forge, Minecraft e AE2. Não é necessário instalar Gradle separadamente. A workflow de GitHub Actions executa a mesma compilação e, se aprovada, disponibiliza o JAR como artefato de desenvolvimento.

## Execução e validação

Use `gradlew.bat runClient` para abrir o ambiente de desenvolvimento. Em uma instalação normal, instale Forge 36.2.42, AE2 8.4.7 e o JAR de ExpansionAE.

Uma compilação aprovada não comprova paridade funcional. Veja [PORT_STATUS.md](PORT_STATUS.md) para cobertura e testes pendentes. Use um mundo de teste até concluir os testes de inventário, autocrafting, salvamento e servidor dedicado.

Créditos, commits de referência e licenças estão em [NOTICE.md](NOTICE.md).
