# ExpansionAE — desenvolvimento para Minecraft 1.16.5

**Addon experimental para Applied Energistics 2. O objetivo é reimplementar e consolidar funcionalidades selecionadas de vários addons AE2 em uma única base coerente para 1.16.5.**

Alvo: Minecraft 1.16.5, Forge 36.2.42 e Applied Energistics 2 8.4.7. O mod usa o ID `expansionae`. Não substitui nem incorpora o AE2.

## Estado do projeto

O repositório está na fase de arquitetura e seleção de funcionalidades. O plano não é portar integralmente nenhum dos projetos de referência, mas adaptar apenas os sistemas escolhidos para a API do AE2 8.4.x.

- [PORT_STATUS.md](PORT_STATUS.md): matriz de funcionalidades, dificuldade e fases propostas.
- [ARCHITECTURE.md](ARCHITECTURE.md): arquitetura do core, módulos opcionais e storage channels.
- [NOTICE.md](NOTICE.md): projetos de referência, créditos e licenças observadas.

## Compilar no Windows

1. Instale um **JDK 8 de 64 bits**, como Eclipse Temurin 8. Configure `JAVA_HOME` para a pasta do JDK, não a pasta `bin`.
2. Extraia o projeto e abra um terminal nessa pasta.
3. Instale **Gradle 6.8.3** ou use a mesma versão configurada pela GitHub Actions.
4. Execute `gradle --no-daemon clean build`.
5. Se a compilação concluir com `BUILD SUCCESSFUL`, o JAR reobfuscado ficará em `build\\libs`.

A GitHub Actions configura Java 8 e Gradle 6.8.3 automaticamente. O Gradle Wrapper será adicionado assim que o binário `gradle-wrapper.jar` for incorporado ao repositório; até lá, o build local requer Gradle 6.8.3 instalado.

A primeira execução precisa de internet para obter Forge, Minecraft e AE2.

## Execução e validação

Use `gradle runClient` para abrir o ambiente de desenvolvimento. Em uma instalação normal, a base será Forge 36.2.42 + AE2 8.4.7 + ExpansionAE.

Uma compilação aprovada não comprova paridade funcional. Use um mundo de teste até concluir testes de inventário, autocrafting, persistência, reload de chunks e servidor dedicado.
