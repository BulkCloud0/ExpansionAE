# ExpansionAE — Arquitetura proposta

## Objetivo

Um único addon para AE2 8.4.7 em Minecraft 1.16.5, com núcleo independente e módulos opcionais de integração.

## Dependências

### Obrigatória

- Applied Energistics 2

### Opcionais

- Ars Nouveau
- Botania
- Create
- Draconic Evolution
- Mekanism
- PneumaticCraft: Repressurized
- ProjectE
- outras integrações futuras

Nenhuma classe do core deve carregar diretamente tipos de uma dependência opcional durante classloading.

## Pacotes propostos

```text
com.bulkcloud.expansionae
├── ExpansionAE
├── core
│   ├── config
│   ├── network
│   ├── registry
│   ├── util
│   └── compat
├── ae2
│   ├── cell
│   ├── crafting
│   ├── grid
│   ├── part
│   ├── storage
│   └── terminal
├── feature
│   ├── disk
│   ├── extendedprovider
│   ├── requester
│   ├── iobus
│   └── accelerator
└── integration
    ├── ars
    ├── botania
    ├── create
    ├── draconic
    ├── mekanism
    ├── pneumaticcraft
    └── projecte
```

## Storage channels no AE2 8.4.x

O AE2 8.4.x permite registrar `IStorageChannel<T>` customizados. Para qualquer recurso não-item/não-fluid, o módulo deve fornecer no mínimo:

- implementação de `IAEStack<T>`;
- implementação de `IStorageChannel<T>`;
- registro em `IStorageHelper#registerStorageChannel`;
- inventário/handler ME;
- persistência NBT;
- representação visual/item para GUI quando aplicável;
- cells/containers e integração com o grid;
- regras de import/export e conversão para a capability/API do mod externo.

Essa camada deve ser genérica o suficiente para ser reaproveitada por XP, mana, source, chemicals, EMC e energia, sem criar uma superclasse que dependa de nenhum mod opcional.

## Regra de classloading

Classes em `integration.<mod>` só podem ser tocadas após confirmar `ModList.get().isLoaded("<modid>")`.

Quando necessário, registrar listeners/handlers por uma classe bootstrap específica da integração, evitando referências diretas a classes externas no construtor principal do ExpansionAE.

## Compatibilidade e persistência

Toda feature de rede deve testar:

- criar/remover nó;
- reconectar após reload;
- save/load de chunk;
- save/load de mundo;
- quebra/reposicionamento do bloco;
- segurança do AE2;
- rede sem canais disponíveis;
- rede sem energia;
- servidor dedicado sem classes client;
- simulação vs modulação em inserção/extração.

## Estratégia de implementação

1. Scaffold e CI.
2. Vertical slice AE2 simples.
3. Features P1.
4. Abstração de custom storage channel.
5. Uma integração de recurso simples (XP é boa candidata por não depender de outro mod).
6. Reusar a infraestrutura para mana/source/chemicals/EMC.
7. Só então implementar integrações multi-mod/multiblock de alta complexidade.
