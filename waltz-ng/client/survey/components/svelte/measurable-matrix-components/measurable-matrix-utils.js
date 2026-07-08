/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016, 2017, 2018, 2019 Waltz open source project
 * See README.md for more information
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific
 *
 */

import _ from "lodash";

export function mkKey(rowId, colId) {
    return `${rowId}-${colId}`;
}


/**
 * Takes the flat list of {measurable, hierarchy} entries returned by the backend (each hierarchy is
 * root-first, leaf/self last) and produces the leaves in a stable grouped order, plus - for every
 * ancestor level (excluding the leaf level itself) - the runs of consecutive leaves sharing the same
 * ancestor at that level, so a table can render one merged header cell per run (rowspan/colspan).
 */
export function buildHierarchyGroups(entries = []) {
    const sorted = _.sortBy(entries, e => _.map(e.hierarchy, h => h.name));

    const depth = _
        .chain(sorted)
        .map(e => e.hierarchy.length)
        .max()
        .value() || 1;

    const pathAt = (entry, level) => entry.hierarchy[level] || entry.hierarchy[entry.hierarchy.length - 1];

    const headerLevels = _
        .range(depth - 1)
        .map(level => {
            const groups = [];
            sorted.forEach((entry, idx) => {
                const node = pathAt(entry, level);
                const prev = _.last(groups);
                if (prev && prev.id === node.id && prev.endIndex === idx - 1) {
                    prev.endIndex = idx;
                    prev.span += 1;
                } else {
                    groups.push({id: node.id, name: node.name, startIndex: idx, endIndex: idx, span: 1});
                }
            });
            return groups;
        });

    const leaves = sorted.map((entry, idx) => ({
        id: entry.measurable.id,
        name: entry.measurable.name,
        kind: entry.measurable.kind,
        hierarchy: entry.hierarchy,
        index: idx
    }));

    return {leaves, headerLevels, depth};
}


/**
 * Given one header level's groups (from buildHierarchyGroups) and a visible leaf-index window
 * [startIndex, endIndex], returns only the groups intersecting that window, with startIndex/endIndex/span
 * clipped to it - so a group spanning e.g. columns 10-25 renders with colspan=6 when only 15-20 are
 * currently rendered (column virtualization).
 */
export function clipGroupsToWindow(groups = [], startIndex, endIndex) {
    return groups
        .filter(g => g.endIndex >= startIndex && g.startIndex <= endIndex)
        .map(g => {
            const clippedStart = Math.max(g.startIndex, startIndex);
            const clippedEnd = Math.min(g.endIndex, endIndex);
            return {
                ...g,
                startIndex: clippedStart,
                endIndex: clippedEnd,
                span: clippedEnd - clippedStart + 1
            };
        });
}


export function isFullyChecked(checkedCells, rowIds = [], colIds = []) {
    return rowIds.length > 0
        && colIds.length > 0
        && _.every(rowIds, r => _.every(colIds, c => checkedCells.has(mkKey(r, c))));
}


export function withCellsSet(checkedCells, rowIds = [], colIds = [], checked) {
    const updated = new Set(checkedCells);
    rowIds.forEach(r => colIds.forEach(c => {
        const key = mkKey(r, c);
        if (checked) {
            updated.add(key);
        } else {
            updated.delete(key);
        }
    }));
    return updated;
}


export function extractCheckedKeys(currentResponse) {
    return new Set(
        _.map(currentResponse?.selections ?? [], s => mkKey(s.row?.id, s.column?.id)));
}


/**
 * Builds the display model for the "this breakdown is being collected for X, together with its
 * descendant node(s) ..." note shown above the matrix grid. Returns null when there's no qualifying
 * product to describe (e.g. the question wasn't issued in a qualifier-based survey run).
 */
export function mkQualifierContext(product, qualifierLeaves = [], maxVisible = 10) {
    if (!product) {
        return null;
    }

    const leafRefs = _
        .chain(Array.from(qualifierLeaves ?? []))
        .map(l => l.measurable)
        .reject(m => m.id === product.id) // covers the already-a-leaf case, where the product is its own sole "leaf"
        .sortBy(m => m.name)
        .value();

    return {
        product,
        visibleChildren: leafRefs.slice(0, maxVisible),
        remainingCount: Math.max(0, leafRefs.length - maxVisible)
    };
}


/**
 * Converts a root-first ancestor path (self last, the shape returned by the backend for
 * row/column/qualifier-leaf hierarchies) into the self node augmented with its `level` (1 = root) and a
 * recursive `parent` chain back to the root. The root node has no `parent` key at all. Every node in the
 * chain (self and every ancestor) gets the same `measurableCategory` name attached, since a whole
 * hierarchy branch always belongs to a single measurable category.
 */
export function mkNestedHierarchy(hierarchy = [], measurableCategory = null) {
    if (_.isEmpty(hierarchy)) {
        return null;
    }

    const build = idx => {
        const level = idx + 1;
        const node = {...hierarchy[idx], level, measurableCategory};
        return idx > 0
            ? {...node, parent: build(idx - 1)}
            : node;
    };

    return build(hierarchy.length - 1);
}


export function mkPayload({app, qualifierLeaves, qualifierCategoryName, rowCategoryName, columnCategoryName, rowsById, columnsById, checkedCells}) {
    const leaves = _.isEmpty(qualifierLeaves) ? [null] : Array.from(qualifierLeaves);

    const selections = _
        .chain(Array.from(checkedCells))
        .flatMap(key => {
            const [rowId, colId] = key.split("-").map(Number);
            const row = rowsById[rowId];
            const column = columnsById[colId];

            return leaves.map(leaf => ({
                targetEntity: app,
                qualifierEntity: leaf ? mkNestedHierarchy(leaf.hierarchy, qualifierCategoryName) : null,
                row: {category: rowCategoryName, ...mkNestedHierarchy(row.hierarchy, rowCategoryName)},
                column: {category: columnCategoryName, ...mkNestedHierarchy(column.hierarchy, columnCategoryName)}
            }));
        })
        .value();

    return {
        responseType: "MEASURABLE_MATRIX",
        selections
    };
}
