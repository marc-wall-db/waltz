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
    import {parseJSON} from "../arc-survey-components/arc-survey-utils";

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
</script>

<br/>
{#if isDataLoaded}
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
