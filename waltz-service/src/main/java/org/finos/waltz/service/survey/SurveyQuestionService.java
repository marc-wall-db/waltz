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

package org.finos.waltz.service.survey;


import org.finos.waltz.data.measurable.MeasurableDao;
import org.finos.waltz.data.measurable.MeasurableIdSelectorFactory;
import org.finos.waltz.data.measurable_rating.MeasurableRatingDao;
import org.finos.waltz.data.measurable_rating.MeasurableRatingIdSelectorFactory;
import org.finos.waltz.data.survey.SurveyInstanceDao;
import org.finos.waltz.data.survey.SurveyQuestionDao;
import org.finos.waltz.model.EntityKind;
import org.finos.waltz.model.EntityReference;
import org.finos.waltz.model.HierarchyQueryScope;
import org.finos.waltz.model.IdSelectionOptions;
import org.finos.waltz.model.ImmutableIdSelectionOptions;
import org.finos.waltz.model.measurable.Measurable;
import org.finos.waltz.model.measurable.MeasurableHierarchy;
import org.finos.waltz.model.measurable.MeasurableHierarchyAlignment;
import org.finos.waltz.model.measurable_rating.MeasurableRating;
import org.finos.waltz.model.survey.ImmutableMeasurableWithHierarchy;
import org.finos.waltz.model.survey.ImmutableSurveyMeasurableMatrixData;
import org.finos.waltz.model.survey.MeasurableWithHierarchy;
import org.finos.waltz.model.survey.SurveyInstance;
import org.finos.waltz.model.survey.SurveyMeasurableMatrixData;
import org.finos.waltz.model.survey.SurveyQuestion;
import org.jooq.Record1;
import org.jooq.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.finos.waltz.common.Checks.checkNotNull;
import static org.finos.waltz.common.Checks.checkTrue;
import static org.finos.waltz.common.CollectionUtilities.isEmpty;
import static org.finos.waltz.common.SetUtilities.map;

@Service
public class SurveyQuestionService {

    private final SurveyQuestionDao surveyQuestionDao;
    private final SurveyInstanceEvaluator evaluator;
    private final SurveyInstanceDao surveyInstanceDao;
    private final MeasurableRatingDao measurableRatingDao;
    private final MeasurableDao measurableDao;

    private final MeasurableRatingIdSelectorFactory measurableRatingIdSelectorFactory = new MeasurableRatingIdSelectorFactory();
    private final MeasurableIdSelectorFactory measurableIdSelectorFactory = new MeasurableIdSelectorFactory();


    @Autowired
    public SurveyQuestionService(SurveyQuestionDao surveyQuestionDao,
                                 SurveyInstanceEvaluator evaluator,
                                 SurveyInstanceDao surveyInstanceDao,
                                 MeasurableRatingDao measurableRatingDao,
                                 MeasurableDao measurableDao) {
        checkNotNull(surveyQuestionDao, "surveyQuestionDao cannot be null");
        checkNotNull(surveyInstanceDao, "surveyInstanceDao cannot be null");
        checkNotNull(measurableRatingDao, "measurableRatingDao cannot be null");
        checkNotNull(measurableDao, "measurableDao cannot be null");

        this.surveyQuestionDao = surveyQuestionDao;
        this.evaluator = evaluator;
        this.surveyInstanceDao = surveyInstanceDao;
        this.measurableRatingDao = measurableRatingDao;
        this.measurableDao = measurableDao;
    }


    public List<SurveyQuestion> findForSurveyTemplate(long templateId) {
        return surveyQuestionDao.findForTemplate(templateId);
    }


    public List<SurveyQuestion> findForSurveyRun(long surveyRunId) {
        return surveyQuestionDao.findForSurveyRun(surveyRunId);
    }


    public List<SurveyQuestion> findForSurveyInstance(long surveyInstanceId) {
        return evaluator.eval(surveyInstanceId).activeQuestions();
    }


    public long create(SurveyQuestion surveyQuestion) {
        checkNotNull(surveyQuestion, "surveyQuestion cannot be null");

        return surveyQuestionDao.create(surveyQuestion);
    }


    public int update(SurveyQuestion surveyQuestion) {
        checkNotNull(surveyQuestion, "surveyQuestion cannot be null");
        checkTrue(surveyQuestion.id().isPresent(), "question id cannot be null");

        return surveyQuestionDao.update(surveyQuestion);
    }


    public int delete(long questionId) {

        if(!surveyQuestionDao.hasResponses(questionId)){
            return surveyQuestionDao.delete(questionId);
        } else {
            throw new IllegalArgumentException("There are responses to this question so it cannot be deleted");
        }
    }


    public Set<SurveyQuestion> findForIds(Set<Long> surveyQuestionsIds) {
        return isEmpty(surveyQuestionsIds)
                ? emptySet()
                : surveyQuestionDao.findForIds(surveyQuestionsIds);
    }


    /**
     * Data for a MEASURABLE_MATRIX question: the row/column measurables (restricted to those
     * already rated against the instance's subject entity, within the question's two qualifier
     * categories), plus the instance's qualifier entity (e.g. a Product) and its children.
     */
    public SurveyMeasurableMatrixData getMeasurableMatrixData(long instanceId, long questionId) {
        SurveyInstance instance = surveyInstanceDao.getById(instanceId);
        checkNotNull(instance, "surveyInstance " + instanceId + " not found");

        Set<SurveyQuestion> questions = surveyQuestionDao.findForIds(singleton(questionId));
        SurveyQuestion question = questions
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("surveyQuestion " + questionId + " not found"));

        Set<MeasurableWithHierarchy> rowMeasurables = question
                .qualifierEntity()
                .map(ref -> findRatedLeafMeasurablesWithHierarchy(instance.surveyEntity(), ref.id()))
                .orElse(emptySet());

        Set<MeasurableWithHierarchy> columnMeasurables = question
                .qualifierEntity2()
                .map(ref -> findRatedLeafMeasurablesWithHierarchy(instance.surveyEntity(), ref.id()))
                .orElse(emptySet());

        EntityReference qualifierRef = instance.qualifierEntity();

        List<EntityReference> productHierarchy = qualifierRef != null && qualifierRef.kind() == EntityKind.MEASURABLE
                ? findMeasurableHierarchyPath(qualifierRef)
                : emptyList();

        // instance.qualifierEntity() only carries kind/id (no name); prefer the fully resolved
        // reference (with name) from the hierarchy lookup when available.
        EntityReference product = !productHierarchy.isEmpty()
                ? productHierarchy.get(productHierarchy.size() - 1)
                : qualifierRef;

        Set<EntityReference> childProducts = product != null && product.kind() == EntityKind.MEASURABLE
                ? findChildMeasurables(product)
                : emptySet();

        return ImmutableSurveyMeasurableMatrixData.builder()
                .app(instance.surveyEntity())
                .rowMeasurables(rowMeasurables)
                .columnMeasurables(columnMeasurables)
                .product(product)
                .productHierarchy(productHierarchy)
                .childProducts(childProducts)
                .build();
    }


    private Set<Measurable> findRatedMeasurablesForCategory(EntityReference subjectRef, long categoryId) {
        Select<Record1<Long>> ratingSelector = measurableRatingIdSelectorFactory.apply(
                ImmutableIdSelectionOptions.builder()
                        .entityReference(subjectRef)
                        .scope(HierarchyQueryScope.EXACT)
                        .build());

        List<MeasurableRating> ratings = measurableRatingDao.findForCategoryAndMeasurableRatingIdSelector(ratingSelector, categoryId);

        Set<Long> measurableIds = map(ratings, MeasurableRating::measurableId);

        return isEmpty(measurableIds)
                ? emptySet()
                : measurableDao.findByIds(measurableIds);
    }


    /**
     * Ratings can be recorded against a measurable at any level of its category's hierarchy, but the
     * matrix is always rendered/answered at the lowest (leaf) level. This expands each rated measurable
     * to its leaf descendants (or itself, if already a leaf) and attaches the full root-to-leaf ancestor
     * path to each, so the UI can render merged hierarchical headers without further lookups.
     */
    private Set<MeasurableWithHierarchy> findRatedLeafMeasurablesWithHierarchy(EntityReference subjectRef, long categoryId) {
        Set<Measurable> ratedMeasurables = findRatedMeasurablesForCategory(subjectRef, categoryId);

        if (isEmpty(ratedMeasurables)) {
            return emptySet();
        }

        List<Measurable> categoryMeasurables = measurableDao.findByCategoryId(categoryId);

        Map<Long, List<Measurable>> childrenByParentId = categoryMeasurables
                .stream()
                .filter(m -> m.parentId().isPresent())
                .collect(Collectors.groupingBy(m -> m.parentId().get()));

        Set<Measurable> leafMeasurables = ratedMeasurables
                .stream()
                .flatMap(m -> collectLeafDescendants(m, childrenByParentId).stream())
                .collect(Collectors.toSet());

        Map<Long, MeasurableHierarchy> hierarchyByMeasurableId = measurableDao
                .findHierarchyForCategory(categoryId)
                .stream()
                .collect(Collectors.toMap(MeasurableHierarchy::measurableId, Function.identity()));

        return map(leafMeasurables, m -> mkMeasurableWithHierarchy(m, hierarchyByMeasurableId));
    }


    private Set<Measurable> collectLeafDescendants(Measurable measurable, Map<Long, List<Measurable>> childrenByParentId) {
        List<Measurable> children = childrenByParentId.getOrDefault(measurable.id().get(), emptyList());

        return children.isEmpty()
                ? singleton(measurable)
                : children
                    .stream()
                    .flatMap(c -> collectLeafDescendants(c, childrenByParentId).stream())
                    .collect(Collectors.toSet());
    }


    /**
     * entity_hierarchy levels are the absolute depth of the ancestor from the root (root = 1), and a
     * self-row (ancestor_id = id, level = own depth) is always included - so sorting a measurable's
     * hierarchy alignments by level ascending yields the full root-to-self path directly.
     */
    private MeasurableWithHierarchy mkMeasurableWithHierarchy(Measurable measurable, Map<Long, MeasurableHierarchy> hierarchyByMeasurableId) {
        EntityReference selfRef = measurable.entityReference();

        List<EntityReference> path = Optional
                .ofNullable(hierarchyByMeasurableId.get(measurable.id().get()))
                .map(MeasurableHierarchy::parents)
                .orElse(emptySet())
                .stream()
                .sorted(Comparator.comparing(MeasurableHierarchyAlignment::level))
                .map(MeasurableHierarchyAlignment::parentReference)
                .collect(Collectors.toCollection(ArrayList::new));

        boolean endsWithSelf = !path.isEmpty() && path.get(path.size() - 1).id() == selfRef.id();
        if (!endsWithSelf) {
            path.add(selfRef);
        }

        return ImmutableMeasurableWithHierarchy.builder()
                .measurable(selfRef)
                .hierarchy(path)
                .build();
    }


    private List<EntityReference> findMeasurableHierarchyPath(EntityReference measurableRef) {
        Measurable measurable = measurableDao.getById(measurableRef.id());

        if (measurable == null) {
            return singletonList(measurableRef);
        }

        Map<Long, MeasurableHierarchy> hierarchyByMeasurableId = measurableDao
                .findHierarchyForCategory(measurable.categoryId())
                .stream()
                .collect(Collectors.toMap(MeasurableHierarchy::measurableId, Function.identity()));

        return mkMeasurableWithHierarchy(measurable, hierarchyByMeasurableId).hierarchy();
    }


    private Set<EntityReference> findChildMeasurables(EntityReference productRef) {
        IdSelectionOptions options = ImmutableIdSelectionOptions.builder()
                .entityReference(productRef)
                .scope(HierarchyQueryScope.CHILDREN)
                .build();

        Set<Measurable> descendants = measurableDao.findByMeasurableIdSelector(measurableIdSelectorFactory.apply(options))
                .stream()
                .filter(m -> m.id().map(id -> id != productRef.id()).orElse(true))
                .collect(java.util.stream.Collectors.toSet());

        return map(descendants, m -> EntityReference.mkRef(EntityKind.MEASURABLE, m.id().get(), m.name()));
    }

}
