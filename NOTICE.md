# NOTICE — referências, créditos e licenças

Este arquivo acompanha o planejamento de ExpansionAE.

## Estado atual

Até o momento desta revisão, o repositório ExpansionAE não contém código-fonte copiado dos projetos listados abaixo. Eles estão sendo usados como referência funcional e arquitetural para um backport/reimplementação destinado a Minecraft 1.16.5.

Antes de incorporar código ou assets de terceiros, a licença do ExpansionAE e as obrigações específicas de cada origem devem ser compatibilizadas e preservadas. Assets não devem ser copiados apenas porque o código do projeto permite reutilização; vários projetos usam licença separada para arte/texturas.

## Projetos de referência

| Projeto | Repositório | Licença de código observada | Observação |
| --- | --- | --- | --- |
| Ars Energistique | https://github.com/62832/ArsEnergistique | LGPL-3.0 | Integração AE2 + Ars Nouveau |
| ExtendedAE | https://github.com/GlodBlock/ExtendedAE | LGPL-3.0 | Provider/interface/buses e outras extensões |
| AE2Things-Forge | https://github.com/Technici4n/AE2Things-Forge | MIT | DISK e extensões AE2 |
| Applied Botanics | https://github.com/ramidzkh/Applied-Botanics | LGPL-3.0 | README declara assets CC BY-NC-SA 3.0 |
| Growth Accelerator Tiers | https://github.com/SathLabs/Growth-Accelerator-Tiers | LGPL-3.0 | Tiers de Growth Accelerator |
| Applied Pneumatics | https://github.com/Frostbite-time/AppliedPneumatics | LGPL-3.0 | README declara assets CC BY-NC-SA 3.0 |
| Create Applied Kinetics | https://github.com/Forsteri123/CreateAppliedKinetics | MIT | Integração Create + AE2 |
| Applied Experienced | https://github.com/alec016/Applied-Experienced | LGPL-3.0 | Experiência como recurso AE |
| AE2 Draconic Fusion Autocrafter | https://github.com/Franchino961-Mod/AE2-Draconic-Fusion-Autocrafter | MIT | Automação de Draconic Fusion |
| ME Beam Former | https://github.com/GaLicn/ME-Beam-Former | LGPL-3.0-or-later | GitHub reporta NOASSERTION, mas o arquivo LICENSE contém LGPLv3-or-later |
| AppliedE | https://github.com/62832/AppliedE | LGPL-3.0 | README declara assets CC BY-NC-SA 3.0 |
| Create: AE2 Recipes | https://github.com/kousuke1902/Create_AE2recipes | MIT | Receitas Create para AE2 |
| AppliedFlux | https://github.com/GlodBlock/ExtendedAE/tree/appflux/26.1.2-neoforge | LGPL-3.0 | Branch separada; armazenamento de energia em células |
| ME Requester | https://github.com/AlmostReliable/merequester | LGPL-3.0 | Stock/request management |
| Applied Mekanistics | https://github.com/AppliedEnergistics/Applied-Mekanistics | LGPL-3.0 | README declara assets CC BY-NC-SA 3.0 |
| AdvancedAE | https://github.com/pedroksl/AdvancedAE | LGPL-3.0 | QoL, IO buses, crafting avançado etc. |

## Applied Energistics 2

ExpansionAE é um addon e depende do Applied Energistics 2. O alvo é AE2 8.4.7 para Minecraft 1.16.5.

Referência:
https://github.com/AppliedEnergistics/Applied-Energistics-2

Partes da API do AE2 8.4.x usadas no planejamento incluem `IStorageChannel`, `IAEStack`, `IStorageGrid` e `IStorageHelper#registerStorageChannel`.

## Regras internas para reutilização

- Preferir reimplementação limpa baseada no comportamento/documentação quando a API moderna divergir muito da 1.16.5.
- Se código for adaptado diretamente, registrar projeto, arquivo/classe de origem e commit/ref de referência.
- Preservar notices de copyright exigidos por MIT/LGPL.
- Não copiar texturas/modelos/gui sem verificar a licença dos assets separadamente.
- Não misturar código sem rastreabilidade de origem.
- Manter integrações opcionais desacopladas do core.
