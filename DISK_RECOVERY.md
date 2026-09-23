# DISK quarantine diagnostics e recovery

Este documento descreve a superfície administrativa **read-only** para diagnosticar DISKs em estado fail-closed/quarentena.

O tooling atual não faz repair automático e não altera registros persistidos. Recovery mutável permanece fora deste escopo até existir uma especificação explícita por tipo de corrupção.

## Regras de segurança

1. Não edite `expansionae_disk_storage.dat` ou outros arquivos do world enquanto o servidor estiver rodando.
2. Não tente “corrigir” UUID duplicado escolhendo um record arbitrariamente.
3. Não reduza `item_count`, `amounts` ou capacity para fazer um payload caber.
4. Não gere um backing novo para um ItemStack que já possui UUID.
5. Antes de qualquer recovery futuro, pare o servidor e faça um backup completo do world.
6. Preserve também os exports SNBT gerados pelo ExpansionAE; eles servem como evidência do payload bruto antes da intervenção.

## Comandos

A raiz administrativa é:

```text
/expansionae disk quarantine ...
```

### list

```text
/expansionae disk quarantine list [page]
```

Permissão mínima: operador nível 2.

Lista os diagnostics persistidos conhecidos. A saída é resumida; payload NBT completo não é despejado no chat.

Campos usuais:

```text
UUID | reason | capacity | expectedCapacity | itemCount | typeCount | duplicateCount
```

### inspect

```text
/expansionae disk quarantine inspect <uuid>
```

Permissão mínima: operador nível 2.

Mostra o diagnóstico detalhado de um UUID persistido. Em caso de UUID duplicado, todos os records são reportados e nenhum deles é tratado como vencedor.

### aliases

```text
/expansionae disk quarantine aliases <uuid>
```

Permissão mínima: operador nível 2.

Mostra somente aliases/views que já estão carregados e rastreados em memória.

Este comando:

- não faz scan do world;
- não carrega chunks;
- não força descoberta de hosts;
- pode mostrar dimensão e coordenadas para hosts AE2 já carregados;
- mostra `slot=?` quando o tracking atual não expõe slot;
- pode mostrar `<unhosted>` para views abertas fora de um host AE2.

A ausência de resultado **não prova** que não existam cópias do ItemStack em chunks descarregados, inventários offline ou outros arquivos.

### export

```text
/expansionae disk quarantine export <uuid>
```

Permissão mínima: operador nível 3.

Exporta cada payload bruto diagnosticado para um arquivo SNBT server-side.

Diretório atual:

```text
expansionae-quarantine/
```

O caminho é relativo ao working directory do servidor.

Cada arquivo inclui:

- timestamp UTC;
- UUID, ou marcador global/anonymous;
- reason code;
- duplicate count;
- capacidade esperada pelo tier quando o diagnóstico vem de um ItemStack carregado;
- payload bruto em SNBT.

O export nunca sobrescreve silenciosamente um arquivo existente. Colisões de nome recebem sufixo novo.

### export-global

```text
/expansionae disk quarantine export-global
```

Permissão mínima: operador nível 3.

Disponível para corrupção global da tag raiz `disks`. Exporta o payload bruto sem tentar reinterpretá-lo como lista válida de records.

## Reason codes persistidos atualmente expostos

- `INVALID_ROOT_DISKS`
- `MISSING_PERSISTED_UUID`
- `DUPLICATE_PERSISTED_UUID`
- `INVALID_CAPACITY_TAG`
- `INCOMPLETE_KEYS_AMOUNTS`
- `INVALID_KEYS_AMOUNTS`
- `INVALID_RECORD_STRUCTURE`
- `NEGATIVE_CAPACITY`

## Reason codes runtime atualmente expostos

Estes reason codes são derivados somente de `DiskCellInventory` já carregados/rastreados em memória; o diagnóstico não faz scan do world nem carrega chunks:

- `MALFORMED_ITEMSTACK_UUID`
- `MISSING_BACKING`
- `OVER_CAPACITY`
- `TIER_CAPACITY_MISMATCH`
- `UNDECODABLE_ITEM_KEY`
- `INCONSISTENT_ITEM_COUNT` — defesa runtime; divergências persistidas normais já são recalculadas deterministicamente por `DiskStorageData.read()` e não exigem comando de recovery.

Os reason codes runtime são observacionais. Eles não fazem bind, normalização, recriação de backing ou repair automático.

## Procedimento de backup antes de recovery futuro

Quando um caso exigir intervenção mutável:

1. Pare o servidor de forma limpa.
2. Copie o diretório completo do world para um local separado.
3. Copie também o diretório `expansionae-quarantine/`.
4. Registre o UUID, reason code e nomes dos exports relacionados.
5. Faça hash dos exports ou preserve os hashes já registrados pela operação/admin tooling quando disponíveis.
6. Só então execute uma ferramenta de recovery explicitamente projetada para aquele reason code.
7. Após a mudança, valide o payload com as mesmas invariantes usadas pelo runtime antes de liberar o DISK.
8. Inicie o servidor e verifique logs, terminal/Drive e contagens antes de descartar o backup.

## O que ainda não existe

Não existe atualmente comando que:

- delete quarantine;
- escolha automaticamente um record duplicado;
- trunque itens para caber em capacity;
- regenere UUID/backing faltante;
- reescreva keys/amounts;
- faça repair automático de NBT.

Qualquer futura operação mutável deve ser permission level 4, criar/exportar backup antes da mutação e falhar sem modificar dados caso a validação pós-repair não passe.

No estado atual, nenhum repair mutável genérico é recomendado: a fase read-only é suficiente até surgir um caso operacional real com transformação inequivocamente segura.
