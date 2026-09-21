# Estado do port

Este arquivo descreve o estado real do branch `port/1.16.5`. A meta continua sendo portar o conteúdo dos branches Forge 1.20.1 do ExtendedAE e AdvancedAE para Minecraft 1.16.5 / Forge 36.2.42 / AE2 8.4.7, adaptando recursos que dependem de APIs inexistentes nessa versão.

## Implementado / adaptado

### Base e terminais
- Expanded Pattern Provider em bloco e multipart, com 36 padrões.
- Expanded Interface em bloco e multipart, com 36 posições.
- Oversize Interface em bloco e multipart, com 36 posições e capacidade 16× (até 1024 itens por posição), com persistência própria para contagens acima do limite vanilla do ItemStack 1.16.5.
- Advanced Pattern Provider e Small Advanced Pattern Provider, incluindo roteamento por face para padrões de processamento.
- Expanded Pattern Access Terminal próprio para inventários maiores que os nove slots assumidos pelo terminal original do AE2 8.4.
- Expanded Crafting Terminal em multipart com Crafting 3×3, Stonecutter, Smithing e Anvil, com inventários persistentes por modo, armazenamento ME e segurança do AE2 8.4.
- Advanced Pattern Encoder portátil.
- Pattern Modifier.
- Persistência dos buffers de roteamento e devolução segura de itens pendentes.

### Armazenamento, máquinas e utilitários do ExtendedAE
- Expanded Drive.
- Expanded IO Port.
- Expanded Charger.
- ME Crystal Fixer adaptado ao sistema antigo de Crystal Seeds do AE2 8.4: cresce seeds diretamente, consumindo energia ME e Charged Certus como catalisador (1 cristal por 100 passos de crescimento).
- Assembler Matrix multiblock via MBCalculator do AE2 8.4: Frame nas arestas, Wall/Glass nas faces, Pattern/Crafter/Speed no interior, 36 padrões por Pattern Matrix, 8 lanes por Crafter Matrix e até 5 Speed Matrix por cluster.
- Expanded Inscriber.
- Expanded Molecular Assembler.
- Circuit Slicer / Circuit Cutter, com receitas de blocos para prints, Speed Cards, tanque opcional, energia AE e auto-export.
- ME Ingredient Buffer adaptado para 1.16.5 com 36 posições compartilhadas entre itens/fluidos e 64.000 mB por posição de fluido.
- ME Caner/Canner para fluidos Forge: modos Fill/Empty, tanque de 64.000 mB, custo de 80 AE por operação e automação por capabilities.
- Block of Silicon / Bloco de Silício, incluindo compressão e descompressão.
- Active Formation Plane.
- Infinity Cobblestone Cell e Infinity Water Cell.
- ME Packing Tape e Packed Device.
- Upgrade items para interface, pattern provider, IO bus, pattern terminal e drive.
- Wireless Connector 1:1 e Wireless Hub de 8 portas com ferramenta básica/avançada de pareamento, fila de até 32 links, persistência de frequência, limite de 1000 blocos e ponte real entre nós AE2 carregados na mesma dimensão.

### Buses e controle
- Fast Import Bus e Fast Export Bus.
- Mod Export Bus e Mod Storage Bus.
- Tag Export Bus e Tag Storage Bus.
- Precise Export Bus e Precise Storage Bus.
- Threshold Export Bus.
- Threshold Level Emitter com histerese.
- AdvancedAE Import/Export Bus.
- AdvancedAE Stock Export Bus com quantidade-alvo configurável dentro das limitações do ItemStack do AE2 8.4.
- AdvancedAE Advanced IO Bus, incluindo regulação de estoque e importação filtrada.
- Throughput Monitor do AdvancedAE, com histórico de vazão e configurador de janela de medição.

### AdvancedAE — materiais e Reaction Chamber
- Reaction Chamber funcional com 9 entradas, saída de item, entrada/saída de fluido, energia AE e quatro Speed Cards.
- Curva de aceleração adaptada do upstream (2/3/5/10/50 passos por ciclo para 0–4 Speed Cards).
- Quantum Infusion, Shattered Singularity, Quantum Alloy, Quantum Alloy Plate, Quantum Processor e Quantum Storage Component.
- Receitas de Reaction Chamber para singularidade, cristais Certus/Fluix e produção em lote de processadores.
- A receita do Quantum Alloy aceita cobre via tag quando disponível e ferro como fallback de compatibilidade para modpacks 1.16.5 sem cobre.
- Quantum Computer / Advanced Crafting CPU com Core, 128M/256M Storage, Accelerator, Data Entangler, Multi-Threader, Unit e Structural Glass.
- Regras de formação 7×7×7 adaptadas do upstream: vidro estrutural na borda, um Core interno, no máximo um Data Entangler e um Multi-Threader.
- O cluster continua usando a engine de jobs do AE2 8.4; os contadores de storage/threads são escalados para os valores padrão do AdvancedAE (8 threads, multiplicadores 4×).
- Quantum Crafter funcional no núcleo item-only do AE2 8.4: 9 padrões de crafting, buffer de 18 saídas, consumo direto do armazenamento ME, retorno automático para a rede e aceleração 1×/8×/16×/32×/64× com Speed Cards. O buffer local garante fallback sem perda quando a inserção na rede falha.
- Quantum Crafter Terminal cabeado e wireless para gerenciar remotamente os Quantum Crafters ativos da mesma rede, com paginação por máquina, edição dos 9 padrões, enable/disable, mínimos de entrada, máximo de saída, ME export e faces de saída.
- Quantum Armor energizada: helmet/chestplate/leggings/boots com capacidades 200M/300M/250M/200M AE, material Quantum Alloy, instalação por cards e menu de configuração para selecionar a peça equipada, instalar, ativar/desativar e desinstalar upgrades. O núcleo 1.16.5 cobre voo, respiração aquática, visão noturna, magnet, auto-feed local, regeneração, força, attack-speed, HP buffer, walk/sprint/swim, jump, step assist, evasion, flight drift e proteção contra queda. Todos os 23 cards estão registrados.
- Portable Workbench funcional no capacete com card instalado: host persistente em NBT, 1 célula editável, 63 filtros, até 24 upgrades da célula, fuzzy mode, partition, clear e copy mode. Pode abrir ao segurar o capacete ou pelo keybind configurável diretamente enquanto ele está equipado.
- Wireless Expanded Pattern Access Terminal e Wireless Expanded Crafting Terminal usando alcance, bateria, segurança e registro wireless nativos do AE2 8.4.
- Quantum Armor Auto Stock adaptado para 1.16.5: Shift no botão do upgrade captura um snapshot de itens/quantidades do inventário e o capacete mantém essas metas extraindo faltas/devolvendo excedentes pela rede ME.
- Recharging Card ligado à rede ME: recarrega a própria armadura e, no peitoral, itens Forge Energy carregados no inventário principal e offhand.
- Pick-Craft no peitoral com keybind configurável (V por padrão): resolve o bloco mirado, valida crafting na rede ME e submete um job de 1 unidade ao crafting grid do AE2 8.4.

### Recursos
- Telas próprias para os containers já adaptados.
- Modelos, blockstates, loot tables, traduções en_US/pt_BR e receitas dos recursos já registrados.
- NOTICE/LGPL preservando créditos e origem das adaptações.

## Ainda pendente ou sem paridade completa

### ExtendedAE
- Integrações opcionais que existam e sejam viáveis no ecossistema 1.16.5.

### AdvancedAE
- Quantum Armor: ainda faltam a paridade dos submenus modernos de valores/filtros/tint/style e a configuração fina de alguns upgrades; Auto Stock, Recharging, Pick-Craft, menu de cards e Portable Workbench já possuem implementação 1.16.5.
- Auto Feed ainda usa alimento local do inventário; falta a seleção por filtro e fallback completo via armazenamento ME do upstream moderno.
- Formato moderno de Advanced Processing Pattern com stacks genéricos/fluidos; o AE2 8.4 não possui AEKey/GenericStack e exige uma representação própria para fluidos em padrões.
- Integrações opcionais.
- Auto-export direcional e configuração visual completa do Reaction Chamber, além da auditoria fina de opções de GUI, lock reasons e comportamentos modernos.

## Diferenças arquiteturais inevitáveis

AE2 8.4.7 antecede várias APIs usadas pelos mods modernos: Pattern Provider separado de Interface, AEKey/GenericStack, GenericStackInv, estratégias genéricas de import/export e o sistema moderno de fluidos/padrões. O port usa equivalentes semânticos de 1.16.5 em vez de copiar classes literalmente.

## Validação

- `clean build` e reobfuscação funcionam com Forge 36.2.42.
- Uma tentativa de alinhar o userdev a Forge 36.1.10 falhou em compilação por diferenças de generics no Forge/AE2 usado pelo código atual; o branch permanece em 36.2.42.
- O smoke test de cliente chega ao carregamento de modelos, mas o userdev Forge 36.2.42 atualmente cai em um `IllegalAccessError` interno envolvendo `TransformationMatrix.inverseVanilla()`. Isso ocorre fora do código compilado do mod e continua sendo investigado separadamente.
- O smoke test automatizado verifica registros, tamanhos de inventário, variantes de provider e células infinitas; deve continuar crescendo junto com o port.

Nenhum subsistema pendente deve ser considerado concluído somente por possuir registro, modelo ou receita; a implementação funcional e a compilação precisam estar presentes.
