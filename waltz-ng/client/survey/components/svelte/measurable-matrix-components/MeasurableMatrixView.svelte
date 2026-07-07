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
    import {mkKey, buildHierarchyGroups, extractCheckedKeys} from "./measurable-matrix-utils";
    import MeasurableMatrixGrid from "./MeasurableMatrixGrid.svelte";
    import NoData from "../../../../common/svelte/NoData.svelte";

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
</script>

{#if !currentResponse || rows.length === 0 || columns.length === 0}
    <NoData type="info">No response recorded.</NoData>
{:else}
    <MeasurableMatrixGrid {rowGroups} {colGroups}>
        <svelte:fragment slot="col-group-header" let:group>{group.name}</svelte:fragment>
        <svelte:fragment slot="leaf-col-header" let:col>{col.name}</svelte:fragment>
        <svelte:fragment slot="row-group-header" let:group>{group.name}</svelte:fragment>
        <svelte:fragment slot="leaf-row-header" let:row>{row.name}</svelte:fragment>
        <svelte:fragment slot="cell" let:row let:col>
            {#if isChecked(checkedCells, row.id, col.id)}
                <i class="fa fa-check text-success"></i>
            {/if}
        </svelte:fragment>
    </MeasurableMatrixGrid>
{/if}
