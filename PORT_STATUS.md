# Estado do port

Este arquivo descreve o código, não uma certificação de funcionamento. A tarefa de portar os dois mods integralmente permanece aberta.

## Código adaptado; validação em andamento

- Fornecedor de padrões em bloco com 36 posições, persistência NBT e interface de configuração usando a API de autocrafting do AE2 8.4.
- Interface em bloco com 36 posições de configuração/armazenamento.
- Buses de importação/exportação de itens com orçamento de transferência multiplicado por 8, mantendo regras de energia, canal, upgrades e redstone do AE2.
- Células de água e pedregulho infinitos, com canais separados de fluidos e itens. São somente de leitura e recusam depósitos.
- Recursos, traduções pt_BR/en_US e receitas próprias para esses seis itens/blocos.

## Ainda não portado

ExtendedAE: variantes multipart de interfaces/fornecedores; terminal de padrões ampliado; ferramentas de upgrade; fita e pacotes; conexões sem fio; ingredient buffer; drive ampliado; modificador de padrões; assembler, inscriber e charger ampliados; crystal fixer; buses por tag/mod/precisão/limiar; formation plane; caner; IO port; oversize interface; assembler matrix; circuit cutter; terminais de crafting; integrações opcionais.

AdvancedAE: fornecedores avançados e pequenos; encoder com roteamento por face; padrões avançados; computador quântico e seus componentes; reaction chamber; quantum crafter e terminais; buses avançados; throughput monitor; armadura quântica, upgrades, energia e configurações; materiais, fluidos, receitas e integrações.

Também não foram auditadas para paridade as adições exclusivas das branches 1.21/26.x. As referências fixadas deste trabalho são as branches Forge 1.20.1 dos dois projetos.

## Validação necessária

1. Compilação e reobfuscação contra as dependências reais.
2. Inicialização de cliente e servidor dedicado; registro de modelos e carregamento de receitas.
3. Exposição dos 36 padrões ao autocrafting, incluindo as posições 9–35.
4. Inserção/extração, shift-click, drops e reload de chunk sem perdas ou duplicação.
5. Inventários cheios, máquina ausente, canais/energia desligados e cancelamento de receitas.
6. Compatibilidade do terminal de interfaces do AE2 com mais de nove padrões. O terminal padrão tem suposições de nove posições; precisa de adaptação antes de declarar suporte.
7. Interface com todos os 36 filtros, salvamento e cartão de crafting.
8. Extração das células nos canais corretos e rejeição de depósitos.
9. Buses com todos os upgrades, redstone e disponibilidade limitada de energia.

Nenhum item deste checklist deve ser marcado como aprovado sem evidência.
