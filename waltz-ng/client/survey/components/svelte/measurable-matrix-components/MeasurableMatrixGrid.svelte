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

<!--
 Shared table shell for MeasurableMatrixEdit/MeasurableMatrixView: fixed-width columns, sticky
 row/column headers, rotated leaf column headers, and column virtualization for large matrices. Cell
 content (checkboxes vs read-only check-marks) is supplied by the caller via named slots so this
 component owns only layout/rendering, not response state.
-->
<script>
    import _ from "lodash";
    import {clipGroupsToWindow} from "./measurable-matrix-utils";

    export let rowGroups;
    export let colGroups;
    export let colWidth = 90;
    export let rowHeaderColWidth = 160;
    export let groupHeaderRowHeight = 32;
    export let leafHeaderHeight = 110;
    export let virtualizeThreshold = 30;

    const BUFFER_COLS = 5;

    $: rows = rowGroups?.leaves ?? [];
    $: columns = colGroups?.leaves ?? [];
    $: rowDepth = rowGroups?.depth ?? 1;
    $: colDepth = colGroups?.depth ?? 1;
    $: totalCols = columns.length;
    $: shouldVirtualize = totalCols > virtualizeThreshold;

    // scroll/viewport state driving the virtualization window - deliberately kept local to this
    // component (measurable-matrix-utils.js stays pure/DOM-free)
    let clientWidth = 0;
    let scrollLeft = 0;
    let rafScheduled = false;

    function onScroll(e) {
        if (rafScheduled) {
            return;
        }
        rafScheduled = true;
        requestAnimationFrame(() => {
            scrollLeft = e.target.scrollLeft;
            rafScheduled = false;
        });
    }

    $: rowHeaderTotalWidth = rowDepth * rowHeaderColWidth;

    function computeVisibleRange(scrollLeft, clientWidth, colWidth, totalCols, buffer, offset) {
        if (totalCols === 0) {
            return {start: 0, end: -1};
        }
        if (!clientWidth) {
            return {start: 0, end: totalCols - 1};
        }
        const contentStart = Math.max(0, scrollLeft - offset);
        const visibleWidth = Math.max(0, clientWidth - offset);
        const rawStart = Math.floor(contentStart / colWidth) - buffer;
        const rawEnd = Math.ceil((contentStart + visibleWidth) / colWidth) + buffer;
        return {
            start: Math.max(0, rawStart),
            end: Math.min(totalCols - 1, rawEnd)
        };
    }

    $: visibleRange = shouldVirtualize
        ? computeVisibleRange(scrollLeft, clientWidth, colWidth, totalCols, BUFFER_COLS, rowHeaderTotalWidth)
        : {start: 0, end: totalCols - 1};

    $: visibleColumns = columns.slice(visibleRange.start, visibleRange.end + 1);

    $: hasLeadingSpacer = shouldVirtualize && visibleRange.start > 0;
    $: hasTrailingSpacer = shouldVirtualize && visibleRange.end < totalCols - 1;

    $: leadingSpacerWidth = hasLeadingSpacer ? visibleRange.start * colWidth : 0;
    $: trailingSpacerWidth = hasTrailingSpacer ? (totalCols - 1 - visibleRange.end) * colWidth : 0;

    $: colHeaderLevels = _
        .range(colDepth - 1)
        .map(level => clipGroupsToWindow(colGroups?.headerLevels?.[level] ?? [], visibleRange.start, visibleRange.end));

    $: leafRowTop = (colDepth - 1) * groupHeaderRowHeight;
    $: leafColLeft = (rowDepth - 1) * rowHeaderColWidth;

    const rowGroupStartingAt = level => _.keyBy(rowGroups?.headerLevels?.[level] ?? [], "startIndex");
</script>

<div class="table-container" bind:clientWidth on:scroll={onScroll}>
    <table class="fixed-layout">
        <colgroup>
            {#each _.range(rowDepth) as _level}
                <col style="width: {rowHeaderColWidth}px"/>
            {/each}
            {#if hasLeadingSpacer}
                <col style="width: {leadingSpacerWidth}px"/>
            {/if}
            {#each visibleColumns as col (col.id)}
                <col style="width: {colWidth}px"/>
            {/each}
            {#if hasTrailingSpacer}
                <col style="width: {trailingSpacerWidth}px"/>
            {/if}
        </colgroup>
        <thead>
        {#each _.range(colDepth - 1) as level}
            <tr>
                <th colspan={rowDepth}
                    class="group-header corner-cell"
                    style="top: {level * groupHeaderRowHeight}px; height: {groupHeaderRowHeight}px;
                           width: {rowHeaderTotalWidth}px; min-width: {rowHeaderTotalWidth}px; max-width: {rowHeaderTotalWidth}px"></th>
                {#if hasLeadingSpacer}
                    <th class="spacer-cell" style="top: {level * groupHeaderRowHeight}px"></th>
                {/if}
                {#each colHeaderLevels[level] as group (group.id + ':' + group.startIndex)}
                    {@const groupWidth = group.span * colWidth}
                    <th colspan={group.span}
                        class="group-header sticky-header"
                        style="top: {level * groupHeaderRowHeight}px; height: {groupHeaderRowHeight}px;
                               width: {groupWidth}px; min-width: {groupWidth}px; max-width: {groupWidth}px">
                        <slot name="col-group-header" {group}/>
                    </th>
                {/each}
                {#if hasTrailingSpacer}
                    <th class="spacer-cell" style="top: {level * groupHeaderRowHeight}px"></th>
                {/if}
            </tr>
        {/each}
        <tr>
            <th colspan={rowDepth}
                class="group-header corner-cell"
                style="top: {leafRowTop}px; height: {leafHeaderHeight}px;
                       width: {rowHeaderTotalWidth}px; min-width: {rowHeaderTotalWidth}px; max-width: {rowHeaderTotalWidth}px"></th>
            {#if hasLeadingSpacer}
                <th class="spacer-cell" style="top: {leafRowTop}px"></th>
            {/if}
            {#each visibleColumns as col (col.id)}
                <th class="leaf-header sticky-header rotated-header"
                    style="top: {leafRowTop}px; height: {leafHeaderHeight}px;
                           width: {colWidth}px; min-width: {colWidth}px; max-width: {colWidth}px">
                    <div class="rotated-content">
                        <slot name="leaf-col-header" {col}/>
                    </div>
                </th>
            {/each}
            {#if hasTrailingSpacer}
                <th class="spacer-cell" style="top: {leafRowTop}px"></th>
            {/if}
        </tr>
        </thead>
        <tbody>
        {#each rows as row, idx (row.id)}
            <tr>
                {#each _.range(rowDepth - 1) as level}
                    {@const group = rowGroupStartingAt(level)[idx]}
                    {#if group}
                        <th rowspan={group.span}
                            class="group-header sticky-col"
                            style="left: {level * rowHeaderColWidth}px;
                                   width: {rowHeaderColWidth}px; min-width: {rowHeaderColWidth}px; max-width: {rowHeaderColWidth}px">
                            <slot name="row-group-header" {group}/>
                        </th>
                    {/if}
                {/each}
                <th class="leaf-header sticky-col"
                    style="left: {leafColLeft}px;
                           width: {rowHeaderColWidth}px; min-width: {rowHeaderColWidth}px; max-width: {rowHeaderColWidth}px">
                    <slot name="leaf-row-header" {row}/>
                </th>
                {#if hasLeadingSpacer}<td class="spacer-cell"></td>{/if}
                {#each visibleColumns as col (col.id)}
                    <td class="checkbox-cell">
                        <slot name="cell" {row} {col}/>
                    </td>
                {/each}
                {#if hasTrailingSpacer}<td class="spacer-cell"></td>{/if}
            </tr>
        {/each}
        </tbody>
    </table>
</div>

<style>
    .table-container {
        overflow-x: auto;
        width: 100%;
    }

    table.fixed-layout {
        table-layout: fixed;
        border-collapse: separate;
        border-spacing: 0;
    }

    th, td {
        box-sizing: border-box;
        padding: 4px 6px;
        border-right: 1px solid #ddd;
        border-bottom: 1px solid #ddd;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    /* border-collapse doesn't play well with position: sticky (collapsed borders get miscomputed at
       sticky boundaries, causing sticky cells to bleed into their neighbour) - each cell draws its own
       right/bottom edge instead, with the table's outer left/top edge added back here */
    thead tr:first-child th {
        border-top: 1px solid #ddd;
    }

    tr > :first-child {
        border-left: 1px solid #ddd;
    }

    th.group-header, th.leaf-header {
        background-color: #f5f5f5;
        text-align: left;
        font-weight: normal;
    }

    th.group-header :global(label), th.leaf-header :global(label) {
        font-weight: normal;
        margin: 0;
        cursor: pointer;
    }

    .checkbox-cell {
        text-align: center;
    }

    .spacer-cell {
        border-left: none;
        border-right: none;
        border-top: none;
        background: transparent;
        padding: 0;
    }

    /* column headers stay pinned to the top of the page/scroll ancestor while scrolling through rows */
    thead .sticky-header {
        position: sticky;
        z-index: 2;
    }

    /* row headers stay pinned to the left of the table container while scrolling through columns */
    tbody .sticky-col {
        position: sticky;
        z-index: 1;
    }

    /* the blank top-left cell needs both axes plus the highest z-index so it wins where they cross */
    .corner-cell {
        position: sticky;
        left: 0;
        z-index: 3;
    }

    /* only the leaf-level column header rotates - merged ancestor group headers span a variable number
       of columns and don't read well rotated */
    .rotated-header {
        vertical-align: bottom;
    }

    .rotated-content {
        display: inline-block;
        transform: rotate(-45deg);
        transform-origin: left bottom;
        white-space: nowrap;
    }
</style>
