package org.finos.waltz.service.survey;

import org.finos.waltz.model.EntityReference;
import org.finos.waltz.model.survey.SurveyQuestion;
import org.finos.waltz.model.survey.SurveyQuestionResponse;

import java.util.Optional;

public class SurveyInstanceUtilities {

    public static Optional<? extends Object> getVal(SurveyQuestion question,
                                                    SurveyQuestionResponse resp) {
        if (resp == null) {
            return Optional.empty();
        }

        switch (question.fieldType()) {
            case TEXT:
            case TEXTAREA:
            case DROPDOWN:
                return resp.stringResponse();
            case NUMBER:
                return resp.numberResponse();
            case DATE:
                return resp.dateResponse();
            case BOOLEAN:
                return resp.booleanResponse();
            case DROPDOWN_MULTI_SELECT:
            case STRING_LIST:
                return resp.listResponse();
            case APPLICATION:
            case PERSON:
                return resp.entityResponse();
            case MEASURABLE_MULTI_SELECT:
            case LEGAL_ENTITY:
                return resp.entityListResponse();
            case ARC:
            case MEASURABLE_MATRIX:
                return resp.jsonResponse();
            default:
                return Optional.empty();
        }
    }


    /**
     * EntityReference equality compares every field (name, externalId, description, ...), so a reference
     * resolved via one path and one resolved via another will often fail .equals() despite denoting the
     * same entity. Identity for matching purposes (e.g. "is this the same target/qualifier entity as
     * before?") should only ever be (kind, id).
     */
    public static String entityKey(EntityReference ref) {
        return ref == null ? null : ref.kind().name() + ":" + ref.id();
    }
}
