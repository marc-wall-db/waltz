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
    import {surveyInstanceStore} from "../../../../svelte-stores/survey-instance-store";
    import {displayError} from "../../../../common/error-utils";
    import NoData from "../../../../common/svelte/NoData.svelte";
    import MeasurableMatrixGrid from "./MeasurableMatrixGrid.svelte";
    import {
        mkKey,
        buildHierarchyGroups,
        isFullyChecked,
        withCellsSet,
        extractCheckedKeys,
        mkPayload
    } from "./measurable-matrix-utils";

    export let matrixData;
    export let instanceId;
    export let question;
    export let currentResponse;

    $: rowGroups = buildHierarchyGroups(matrixData?.rowMeasurables ?? []);
    $: colGroups = buildHierarchyGroups(matrixData?.columnMeasurables ?? []);

    $: rows = rowGroups.leaves;
    $: columns = colGroups.leaves;

    $: rowsById = _.keyBy(rows, "id");
    $: columnsById = _.keyBy(columns, "id");
    $: rowIds = _.map(rows, "id");
    $: columnIds = _.map(columns, "id");

    $: rowCategoryName = question?.qualifierEntity?.name;
    $: columnCategoryName = question?.qualifierEntity2?.name;

    let checkedCells = extractCheckedKeys(currentResponse);
    let saving = false;

    // `cells` is taken as an explicit argument (rather than closing over `checkedCells`) so that
    // Svelte's template dependency tracking - which only looks at identifiers written directly in
    // the markup expression, not inside called function bodies - sees `checkedCells` as a dependency
    // of `checked={isChecked(checkedCells, row.id, col.id)}` and re-renders the cell when it changes.
    const isChecked = (cells, rowId, colId) => cells.has(mkKey(rowId, colId));

    const rowIdsInRange = (startIndex, endIndex) => _.map(rows.slice(startIndex, endIndex + 1), "id");
    const colIdsInRange = (startIndex, endIndex) => _.map(columns.slice(startIndex, endIndex + 1), "id");

    const save = () => {
        saving = true;
        const payload = mkPayload({
            app: matrixData?.app,
            qualifierLeaves: matrixData?.qualifierLeaves,
            qualifierCategoryName: matrixData?.qualifierCategory?.name,
            rowCategoryName,
            columnCategoryName,
            rowsById,
            columnsById,
            checkedCells
        });
        surveyInstanceStore
            .saveResponse(instanceId, {questionId: question.id, jsonResponse: JSON.stringify(payload)})
            .then(() => saving = false)
            .catch(e => {
                saving = false;
                displayError("Could not save response", e);
            });
    };

    const applyAndSave = (updated) => {
        checkedCells = updated;
        save();
    };

    const toggleCell = (rowId, colId) =>
        applyAndSave(withCellsSet(checkedCells, [rowId], [colId], !isChecked(checkedCells, rowId, colId)));

    const toggleRow = (rowId, checked) =>
        applyAndSave(withCellsSet(checkedCells, [rowId], columnIds, checked));

    const toggleColumn = (colId, checked) =>
        applyAndSave(withCellsSet(checkedCells, rowIds, [colId], checked));

    const toggleRowGroup = (group, checked) =>
        applyAndSave(withCellsSet(checkedCells, rowIdsInRange(group.startIndex, group.endIndex), columnIds, checked));

    const toggleColumnGroup = (group, checked) =>
        applyAndSave(withCellsSet(checkedCells, rowIds, colIdsInRange(group.startIndex, group.endIndex), checked));
</script>

{#if rows.length === 0 || columns.length === 0}
    <NoData type="info">
        No measurables have been rated against this application in the configured categories yet -
        there is nothing to break down.
    </NoData>
{:else}
    <MeasurableMatrixGrid {rowGroups} {colGroups}>
        <svelte:fragment slot="col-group-header" let:group>
            <label>
                <input type="checkbox"
                       checked={isFullyChecked(checkedCells, rowIds, colIdsInRange(group.startIndex, group.endIndex))}
                       on:change={(e) => toggleColumnGroup(group, e.target.checked)}/>
                {group.name}
            </label>
        </svelte:fragment>
        <svelte:fragment slot="leaf-col-header" let:col>
            <label>
                <input type="checkbox"
                       checked={isFullyChecked(checkedCells, rowIds, [col.id])}
                       on:change={(e) => toggleColumn(col.id, e.target.checked)}/>
                {col.name}
            </label>
        </svelte:fragment>
        <svelte:fragment slot="row-group-header" let:group>
            <label>
                <input type="checkbox"
                       checked={isFullyChecked(checkedCells, rowIdsInRange(group.startIndex, group.endIndex), columnIds)}
                       on:change={(e) => toggleRowGroup(group, e.target.checked)}/>
                {group.name}
            </label>
        </svelte:fragment>
        <svelte:fragment slot="leaf-row-header" let:row>
            <label>
                <input type="checkbox"
                       checked={isFullyChecked(checkedCells, [row.id], columnIds)}
                       on:change={(e) => toggleRow(row.id, e.target.checked)}/>
                {row.name}
            </label>
        </svelte:fragment>
        <svelte:fragment slot="cell" let:row let:col>
            <input type="checkbox"
                   checked={isChecked(checkedCells, row.id, col.id)}
                   on:change={() => toggleCell(row.id, col.id)}/>
        </svelte:fragment>
    </MeasurableMatrixGrid>
    {#if saving}
        <p class="help-block"><i class="fa fa-spin fa-spinner"></i> Saving...</p>
    {/if}
{/if}
