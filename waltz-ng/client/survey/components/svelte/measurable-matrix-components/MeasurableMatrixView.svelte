<!--
  ~ Waltz - Enterprise Architecture
  ~ Copyright (C) 2016 - 2026 Waltz open source project
  ~ See README.md for more information
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific
  ~
  -->

<script>
    import _ from "lodash";
    import NoData from "../../../../common/svelte/NoData.svelte";
    import {mkKey, buildHierarchyGroups, extractCheckedKeys} from "./measurable-matrix-utils";

    export let matrixData;
    export let currentResponse;

    $: rowGroups = buildHierarchyGroups(matrixData?.rowMeasurables ?? []);
    $: colGroups = buildHierarchyGroups(matrixData?.columnMeasurables ?? []);

    $: rows = rowGroups.leaves;
    $: columns = colGroups.leaves;

    $: checkedCells = extractCheckedKeys(currentResponse);

    // `cells` is an explicit argument so Svelte's template dependency tracking sees `checkedCells`
    // as a dependency of `isChecked(checkedCells, row.id, col.id)` (see MeasurableMatrixEdit.svelte).
    const isChecked = (cells, rowId, colId) => cells.has(mkKey(rowId, colId));

    const rowGroupStartingAt = level => _.keyBy(rowGroups.headerLevels[level] ?? [], "startIndex");
</script>

{#if !currentResponse || rows.length === 0 || columns.length === 0}
    <NoData type="info">No response recorded.</NoData>
{:else}
    <div class="table-container">
        <table>
            <thead>
            {#each _.range(colGroups.depth - 1) as level}
                <tr>
                    <th colspan={rowGroups.depth}></th>
                    {#each colGroups.headerLevels[level] as group (group.startIndex)}
                        <th colspan={group.span} class="group-header">{group.name}</th>
                    {/each}
                </tr>
            {/each}
            <tr>
                <th colspan={rowGroups.depth}></th>
                {#each columns as col (col.id)}
                    <th class="leaf-header">{col.name}</th>
                {/each}
            </tr>
            </thead>
            <tbody>
            {#each rows as row, idx (row.id)}
                <tr>
                    {#each _.range(rowGroups.depth - 1) as level}
                        {@const group = rowGroupStartingAt(level)[idx]}
                        {#if group}
                            <th rowspan={group.span} class="group-header">{group.name}</th>
                        {/if}
                    {/each}
                    <th class="leaf-header">{row.name}</th>
                    {#each columns as col (col.id)}
                        <td class="checkbox-cell">
                            {#if isChecked(checkedCells, row.id, col.id)}
                                <i class="fa fa-check text-success"></i>
                            {/if}
                        </td>
                    {/each}
                </tr>
            {/each}
            </tbody>
        </table>
    </div>
{/if}

<style>
    .table-container {
        overflow-x: auto;
        width: 100%;
    }

    table {
        border-collapse: collapse;
    }

    th, td {
        padding: 6px 10px;
        border: 1px solid #ddd;
        white-space: nowrap;
    }

    th.group-header, th.leaf-header {
        background-color: #f5f5f5;
        text-align: left;
        font-weight: normal;
    }

    .checkbox-cell {
        text-align: center;
    }
</style>
