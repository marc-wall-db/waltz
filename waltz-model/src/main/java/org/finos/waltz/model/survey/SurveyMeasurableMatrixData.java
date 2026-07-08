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

package org.finos.waltz.model.survey;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.finos.waltz.model.EntityReference;
import org.finos.waltz.model.Nullable;
import org.immutables.value.Value;

import java.util.List;
import java.util.Set;

/**
 * Data required to render a MEASURABLE_MATRIX survey question for a given survey instance:
 * the row/column measurables (restricted to leaf descendants of measurables already rated against
 * the instance's subject entity, within the question's two qualifier categories - each carrying its
 * full ancestor path so hierarchical headers can be rendered), plus the survey instance's subject
 * (app), qualifier entity (e.g. the Product this instance was bulk-issued for) and that entity's leaf
 * descendants (or itself, if already a leaf) - each with its own full ancestor path. A saved response
 * is exploded to one selection per qualifier leaf, since ratings/breakdowns are always meaningful at
 * leaf level even when a survey is issued against a higher-level rollup node.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableSurveyMeasurableMatrixData.class)
public interface SurveyMeasurableMatrixData {

    EntityReference app();

    Set<MeasurableWithHierarchy> rowMeasurables();

    Set<MeasurableWithHierarchy> columnMeasurables();

    @Nullable
    EntityReference product();

    Set<MeasurableWithHierarchy> qualifierLeaves();

    @Nullable
    EntityReference qualifierCategory();
}
