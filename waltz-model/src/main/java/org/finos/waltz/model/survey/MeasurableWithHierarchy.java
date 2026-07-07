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
import org.immutables.value.Value;

import java.util.List;

/**
 * A measurable together with its full ancestor path (root first, this measurable last),
 * so hierarchical UI (e.g. the MEASURABLE_MATRIX question type) can render grouped headers
 * without a further round-trip to resolve ancestors.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableMeasurableWithHierarchy.class)
public interface MeasurableWithHierarchy {

    EntityReference measurable();

    List<EntityReference> hierarchy();
}
