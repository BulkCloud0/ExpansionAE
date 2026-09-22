# ExpansionAE — Port Status e Backlog Técnico

> Documento de planejamento. Nenhuma linha de código dos projetos de referência foi incorporada ao ExpansionAE nesta etapa.

## Base alvo

- Minecraft 1.16.5
- Forge 36.2.42
- Applied Energistics 2 8.4.7 / branch 8.4.x-1.16.x
- Java 8
- Mod ID: `expansionae`
- Dependência obrigatória pretendida: AE2
- Integrações com outros mods: opcionais e carregadas somente quando o mod correspondente estiver presente

## Restrição principal do backport

Os projetos de referência são majoritariamente 1.18+ / 1.20+ / 1.21+, então o trabalho deve ser tratado como reimplementação de comportamento e não como merge de código.

O AE2 8.4.x não possui a API moderna de `AEKey`, mas já suporta canais de armazenamento customizados por meio de `IStorageChannel<T extends IAEStack<T>>` e `IStorageHelper#registerStorageChannel`. O grid cria monitores para os canais registrados. Portanto, recursos como mana, experiência, químicos e outros podem ser integrados, porém cada integração precisa implementar sua própria pilha/canal no modelo antigo de AE2.

## Fases propostas

### P0 — Infraestrutura

- Projeto Forge/Gradle 1.16.5.
- Registro de blocos, itens, tile entities, containers, packets e configs.
- Camada de compatibilidade com AE2 8.4.7.
- Carregamento condicional de integrações.
- Convenções de NBT, segurança de servidor dedicado e testes de persistência.
- Política de licenças e créditos.

### P1 — Núcleo AE2 / alto retorno

| Origem | Funcionalidades candidatas | Dependência extra | Complexidade | Situação |
| --- | --- | --- | --- | --- |
| ExtendedAE | Pattern Provider 36 slots; Interface 36 slots; buses rápidos; melhorias de Pattern Access | Não | Média | Candidato P1 |
| AE2Things | DISK sem limite de tipos, com modelo próprio de capacidade | Não | Média | Em validação — 1k slice com runtime e persistência automatizados |
| ME Requester | Requester de estoque e terminal de gerenciamento | Não | Média/Alta | Candidato P1 |
| AdvancedAE | Stock Export Bus; Import/Export Bus; Advanced IO Bus | Não | Média/Alta | Candidato P1 |
| Create: AE2 Recipes | Receitas Create para componentes AE2 | Create | Baixa/Média | Candidato P1 opcional |
| Growth Accelerator Tiers | Cranked, Boosted e Directional Growth Accelerator | Não | Média | Candidato P1/P2 |

### P2 — Recursos armazenáveis e utilidades avançadas

| Origem | Funcionalidades candidatas | Dependência extra | Complexidade | Situação |
| --- | --- | --- | --- | --- |
| Applied Experienced | Canal de XP; células; célula portátil; acceptor/converter; P2P de experiência | Não | Alta | Candidato P2 |
| Applied Botanics | Canal de mana; células/portáteis; Fluix Mana Pool; P2P de mana | Botania | Alta | Candidato P2 |
| Ars Energistique | Source cells; portable source cell; ME Source Jar; Source Converter; Source P2P; Spell P2P | Ars Nouveau | Alta | Candidato P2 |
| Applied Mekanistics | Canal químico; chemical cells/portáteis; Chemical P2P | Mekanism | Alta | Candidato P2 |
| AppliedE | EMC como recurso da rede; transmutation module; itens transmutáveis via rede | ProjectE | Alta | Candidato P2 |
| AppliedFlux (branch do ExtendedAE) | Armazenamento de energia/FE em células | Mod de energia aplicável | Alta | Candidato P2 |
| AdvancedAE | ME Throughput Monitor | Não / AppliedFlux para FE | Média | Candidato P2 |

### P3 — Integrações profundas e networking especial

| Origem | Funcionalidades candidatas | Dependência extra | Complexidade | Situação |
| --- | --- | --- | --- | --- |
| Applied Pneumatics | Air cells; Pressure Interface; Temperature Interface; P2P de ar/temperatura; Amadron automation | PneumaticCraft: Repressurized | Muito alta | Candidato P3 |
| Create Applied Kinetics | Energia cinética -> AE; ME Proxy para itens/fluidos; receitas de sequenced assembly | Create | Alta | Candidato P3 |
| AE2 Draconic Fusion Autocrafter | Pattern Provider especializado para Fusion Crafting; roteamento de catalyst/ingredients; retry quando core ocupado | Draconic Evolution | Muito alta | Candidato P3 |
| ME Beam Former | Conexões ME sem cabo; Beam Former/Omni Beam Former; binding tool; wireless energy tower | Não / integração de energia opcional | Muito alta | Candidato P3 |
| AdvancedAE | Advanced Pattern Provider com roteamento por face | Não | Alta | Candidato P3 |

### P4 — Sistemas que devem esperar o núcleo estar estável

- AdvancedAE Quantum Computer.
- AdvancedAE Quantum Crafter.
- AdvancedAE Quantum Armor.
- AdvancedAE Reaction Chamber.
- Qualquer multiblock novo que altere profundamente crafting CPUs ou scheduling de autocrafting.

## Notas por projeto

### Ars Energistique

O código atual expõe explicitamente Source Cell, Portable Source Cell, ME Source Jar, Source Converter, Source P2P e Spell P2P. Para 1.16.5, a abordagem deve usar um canal customizado baseado em `IAEStack` e adaptar a API do Ars Nouveau da época.

### ExtendedAE

O README atual documenta Pattern Provider e Interface de 36 slots, células infinitas de água/cobblestone, Pattern Access Terminal melhorado e import/export buses 8x. A primeira entrega deve evitar tentar reproduzir todo o mod e focar em Provider/Interface/buses.

### AE2Things

O principal candidato é o DISK: armazenamento de itens sem limite de tipos e com custo de capacidade diferente das células normais.

### Applied Botanics

O código atual implementa mana como recurso da rede, incluindo storage/portable cells, integração com recipientes, P2P e Fluix Mana Pool. Em 1.16.5 isso deve ser refeito sobre o storage channel antigo do AE2.

### Growth Accelerator Tiers

Três conceitos principais: Cranked, Boosted e Directional Growth Accelerator. É relativamente isolado do restante da arquitetura de storage/crafting.

### Applied Pneumatics

O projeto moderno inclui armazenamento de ar, interfaces de pressão e temperatura, P2P e automação do Amadron. É um excelente módulo opcional, mas possui superfície de integração grande.

### Create Applied Kinetics

O projeto moderno contém:
- Energy Provider cinético que injeta energia na rede AE;
- ME Proxy que expõe o storage de itens/fluidos da rede como capabilities;
- itens intermediários/receitas para sequenced assembly.

### Applied Experienced

O código atual implementa células de experiência, portable cells, Experience Acceptor, Experience Converter e Experience P2P.

### AE2 Draconic Fusion Autocrafter

A funcionalidade central é um Pattern Provider especializado que detecta o Fusion Crafting Core, separa catalyst de ingredients, distribui ingredients aos injectors e espera/reexecuta quando o core está ocupado.

### ME Beam Former

O código atual contém Beam Former, Omni Beam Former, ferramenta de binding, monitor de energia e Wireless Energy Tower. O arquivo LICENSE do repositório é LGPL-3.0-or-later, embora a API do GitHub não o classifique automaticamente.

### AppliedE

Integra EMC diretamente à rede e expõe itens conhecidos/transmutáveis. É conceitualmente possível no AE2 8.4.x por storage channel customizado, mas exige desenho cuidadoso para identidade do proprietário, segurança e distribuição de EMC.

### Create: AE2 Recipes

É majoritariamente conteúdo/receitas. Pode ser um módulo compat opcional de baixo risco, desde que as receitas sejam redesenhadas para Create 0.3.x/1.16.5 em vez de copiadas cegamente de 1.21.

### AppliedFlux

A branch fornecida declara `mod_id=appflux` e descrição "Store energy in cells!". Deve ser tratada como referência separada de funcionalidade, apesar de viver no repositório ExtendedAE.

### ME Requester

Mantém quantidades mínimas de recursos em estoque e oferece um terminal para configurar/monitorar requests. Em 1.16.5 o primeiro escopo deve começar com itens; suporte a outros canais pode vir depois.

### Applied Mekanistics

O projeto moderno trabalha com chemicals como recurso AE, células químicas portáteis/normais e Chemical P2P. A versão 1.16.5 do Mekanism separa gases/infusion/pigments/slurries de forma diferente da API atual, então o design precisa ser específico para essa geração.

### AdvancedAE

Funcionalidades candidatas de curto/médio prazo: IO buses, throughput monitor e pattern routing por face. Quantum Computer, Quantum Crafter, Armor e Reaction Chamber ficam deliberadamente fora da primeira etapa.

## Critérios para aceitar uma feature

Uma feature só entra na implementação quando:

1. tiver comportamento definido para singleplayer e servidor dedicado;
2. não exigir que uma integração opcional vire dependência obrigatória;
3. possuir estratégia de persistência/NBT;
4. possuir comportamento previsível em unload/reload de chunk;
5. não quebrar segurança/canais/autocrafting do AE2;
6. tiver origem/licença registradas em `NOTICE.md`;
7. puder ser testada isoladamente antes de ser combinada com outros módulos.

## Próxima etapa

O scaffold já compila e o primeiro vertical slice escolhido foi o DISK. A ordem imediata agora é:

1. validar inserção/extração por um storage grid/ME Terminal real;
2. validar unload/reload de chunk e quebra/recolocação do Drive;
3. validar duas cópias com o mesmo UUID quando hospedadas por Drives/grids reais;
4. fazer uma passagem manual pela GUI do Cell Workbench;
5. somente depois expandir o DISK para outros tiers e iniciar a próxima feature P1.

Canais customizados de mana/XP/químicos/EMC continuam bloqueados até essa camada de persistência estar comprovada em runtime.


## Validação em andamento — DISK

O primeiro vertical slice implementado é o `expansionae:1k_disk`.

Estado atual:

- custom `ICellHandler` registrado no AE2;
- custom `ICellInventory<IAEItemStack>`;
- capacidade experimental de 1000 itens com 1 item = 1 unidade;
- sem limite artificial de tipos;
- persistência externa via `WorldSavedData` indexada por UUID;
- conteúdo completo fica fora do NBT do ItemStack;
- receita experimental disponível;
- GitHub Actions compila, executa os testes JUnit e empacota a feature com sucesso;
- registros externos malformados são sanitizados durante o load;
- qualquer DISK que já possua UUID e esteja sem backing record é tratado como persistência corrompida/incompleta; leitura e escrita ficam bloqueadas em vez de recriar ou sobrescrever silenciosamente o armazenamento;
- UUID é a identidade do armazenamento: cópias exatas do ItemStack com o mesmo UUID são aliases do mesmo conteúdo, não discos independentes;
- quando um DISK com UUID fica vazio, o registro vazio e o UUID são preservados para que aliases existentes continuem sincronizados;
- o CI possui smoke test de dedicated server no evento de pull request, validado com Forge 36.2.42 + AE2 8.4.7 usando MCP `20210309-1.16.5`;
- o runtime valida o contrato do Cell Workbench: 63 slots de configuração, 2 slots de upgrade, 1× FUZZY e 1× INVERTER;
- o runtime valida capacidade de 1000 itens, inserção/extração, sincronização de duas cópias com o mesmo UUID e preservação do backing record vazio;
- o CI executa duas inicializações consecutivas do dedicated server no mesmo mundo: a primeira grava 321 itens no `WorldSavedData`, encerra via RCON e a segunda recupera/extrai os 321 itens antes de limpar o registro;
- a persistência externa do DISK através de save/restart do servidor está coberta automaticamente;
- o runtime coloca um DISK pré-carregado em um ME Drive real, confirma que o host aceita a célula, preserva o conteúdo e reporta estado `NOT_EMPTY`;
- o runtime coloca um DISK em um ME Chest real e confirma que o monitor de itens usado pela superfície de terminal enxerga a quantidade armazenada.

Antes de promover a feature para concluída ainda faltam: grid/ME Terminal real para inserção/extração end-to-end, unload/reload de chunk, quebra/recolocação do Drive, aliases de UUID hospedados em Drives/redes reais e uma passagem manual pela GUI do Cell Workbench.
