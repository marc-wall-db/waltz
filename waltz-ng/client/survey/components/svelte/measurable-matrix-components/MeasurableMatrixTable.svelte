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
    import {surveyQuestionStore} from "../../../../svelte-stores/survey-question-store";
    import {REMOTE_API_STATUS} from "../../../../common/constants";
    import MeasurableMatrixEdit from "./MeasurableMatrixEdit.svelte";
    import MeasurableMatrixView from "./MeasurableMatrixView.svelte";
    import EntityLink from "../../../../common/svelte/EntityLink.svelte";
    import {parseJSON} from "../arc-survey-components/arc-survey-utils";
    import {mkQualifierContext} from "./measurable-matrix-utils";

    export let instanceId;
    export let question;
    export let currentResponse;
    export let mode;

    const MODES = {
        VIEW: "VIEW",
        EDIT: "EDIT"
    };

    $: mode = (mode === MODES.VIEW || mode === MODES.EDIT) ? mode : MODES.VIEW;

    $: matrixDataCall = question?.id && instanceId
        && surveyQuestionStore.getMeasurableMatrixData(instanceId, question.id);

    $: isDataLoaded = $matrixDataCall?.status === REMOTE_API_STATUS.LOADED;

    $: matrixData = $matrixDataCall?.data;

    $: parsedCurrentResponse = currentResponse && parseJSON(currentResponse);

    $: qualifierContext = mkQualifierContext(matrixData?.product, matrixData?.childProducts);
</script>

<br/>
{#if isDataLoaded}
    {#if qualifierContext}
        <div class="matrix-qualifier-context">
            This breakdown is being collected for <EntityLink ref={qualifierContext.product}/>
            {#if qualifierContext.visibleChildren.length > 0}
                , together with its underlaying node(s):
                {#each qualifierContext.visibleChildren as child, i (child.id)}
                    <EntityLink ref={child}/>{i < qualifierContext.visibleChildren.length - 1 ? ", " : ""}
                {/each}
                {#if qualifierContext.remainingCount > 0}
                    (and {qualifierContext.remainingCount} more)
                {/if}
            {/if}
        </div>
    {/if}
    {#if mode === MODES.EDIT}
        <MeasurableMatrixEdit {matrixData}
                              {instanceId}
                              {question}
                              currentResponse={parsedCurrentResponse}/>
    {:else}
        <MeasurableMatrixView {matrixData}
                              currentResponse={parsedCurrentResponse}/>
    {/if}
{/if}
<br/>

<style>
    .matrix-qualifier-context {
        background-color: #f5f9fc;
        border-left: 3px solid #6ba3d6;
        padding: 8px 12px;
        margin-bottom: 0.8em;
        font-size: 0.95em;
    }
</style>
