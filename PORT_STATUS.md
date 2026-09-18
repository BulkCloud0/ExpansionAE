# Estado do port

Este arquivo descreve o estado real do branch `port/1.16.5`. A meta continua sendo portar o conteúdo dos branches Forge 1.20.1 do ExtendedAE e AdvancedAE para Minecraft 1.16.5 / Forge 36.2.42 / AE2 8.4.7, adaptando recursos que dependem de APIs inexistentes nessa versão.

## Implementado / adaptado

### Base e terminais
- Expanded Pattern Provider em bloco e multipart, com 36 padrões.
- Expanded Interface em bloco e multipart, com 36 posições.
- Advanced Pattern Provider e Small Advanced Pattern Provider, incluindo roteamento por face para padrões de processamento.
- Expanded Pattern Access Terminal próprio para inventários maiores que os nove slots assumidos pelo terminal original do AE2 8.4.
- Advanced Pattern Encoder portátil.
- Pattern Modifier.
- Persistência dos buffers de roteamento e devolução segura de itens pendentes.

### Armazenamento, máquinas e utilitários do ExtendedAE
- Expanded Drive.
- Expanded IO Port.
- Expanded Charger.
- Expanded Inscriber.
- Expanded Molecular Assembler.
- Active Formation Plane.
- Infinity Cobblestone Cell e Infinity Water Cell.
- ME Packing Tape e Packed Device.
- Upgrade items para interface, pattern provider, IO bus, pattern terminal e drive.

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

### Recursos
- Telas próprias para os containers já adaptados.
- Modelos, blockstates, loot tables, traduções en_US/pt_BR e receitas dos recursos já registrados.
- NOTICE/LGPL preservando créditos e origem das adaptações.

## Ainda pendente ou sem paridade completa

### ExtendedAE
- Wireless Connector, Wireless Hub e ferramentas/terminais wireless estendidos.
- Ingredient Buffer. O original moderno usa GenericStackInv; AE2 8.4 não possui a mesma abstração, portanto exige uma implementação equivalente para itens/fluidos.
- Crystal Fixer. O original depende do sistema moderno de budding quartz, inexistente no AE2 8.4; requer redesign/backport da mecânica.
- Caner.
- Circuit Cutter e seu sistema de receitas.
- Assembler Matrix multiblock.
- Expanded Crafting Terminal e variantes wireless.
- Paridade específica do Oversize Interface além da adaptação item-only já coberta pelo Expanded Interface.
- Integrações opcionais que existam e sejam viáveis no ecossistema 1.16.5.

### AdvancedAE
- Advanced Crafting CPU / componentes e lógica de cluster.
- Quantum Computer.
- Quantum Crafter, terminal e terminal wireless.
- Quantum Armor, upgrades, energia, filtros e telas de configuração.
- Portable Workbench e utilitários associados.
- Formato moderno de Advanced Processing Pattern com stacks genéricos/fluidos.
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
