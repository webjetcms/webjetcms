package sk.iway.iwcm.components.multistep_form.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.multistep_form.jpa.ConditionType;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.JoinOperatorType;
import sk.iway.iwcm.components.multistep_form.jpa.OperatorType;


public class FormConditionsHandler {

    private final FormItemsConditionsRepository formItemsConditionsRepository;
    private final FormItemsRepository formItemsRepository;

    private final Map<Long, Map<ConditionType, List<FormItemsConditionEntity>>> conditionsCache = new HashMap<>();


    private String formName;
    private HttpServletRequest request;

    public FormConditionsHandler(String formName, HttpServletRequest request) {

        if(Tools.isEmpty(formName)) throw new IllegalArgumentException("Form name must be provided to FormConditionsHandler.");
        this.formName = formName;
        this.request = request;

        this.formItemsConditionsRepository = Tools.getSpringBean("formItemsConditionsRepository", FormItemsConditionsRepository.class);
        if(this.formItemsConditionsRepository == null) throw new IllegalStateException("FormHtmlHandler was not able to obtain FormItemsConditionsRepository");

        this.formItemsRepository = Tools.getSpringBean("formItemsRepository", FormItemsRepository.class);
        if(this.formItemsRepository == null) throw new IllegalStateException("FormHtmlHandler was not able to obtain FormItemsRepository");
    }


    /**
     * Check if a field should be hidden based on its visibility conditions.
     * Evaluates conditions against both session data (previous steps) and received data (current step).
     * Field is hidden when conditions are NOT met (inverse logic).
     *
     * @param stepItem the form item with potential visibility conditions
     * @param received the current step's submitted JSON data
     * @return true if the field should be hidden (conditions NOT met), false if visible
     */
    protected Boolean isFieldHiddenByCondition(FormItemEntity stepItem, JSONObject received) {
        Boolean result = evaluateFieldConditions(stepItem, received, ConditionType.VISIBILITY);
        return result == null ? null : !result;
    }

    /**
     * Check if a field should be required based on its REQUIREMENT conditions.
     * Evaluates conditions against both session data (previous steps) and received data (current step).
     * Field is required when conditions are met (direct logic).
     *
     * @param stepItem the form item with potential requirement conditions
     * @param received the current step's submitted JSON data
     * @return true if the field should be required (all conditions met), false otherwise
     */
    protected Boolean isFieldRequiredByCondition(FormItemEntity stepItem, JSONObject received) {
        return evaluateFieldConditions(stepItem, received, ConditionType.REQUIREMENT);
    }

    /**
     * Evaluates if all conditions are met for a field.
     * Returns whether the overall condition result is true (all individual conditions are met).
     *
     * @param stepItem the form item with conditions
     * @param received the current step's submitted JSON data
     * @param conditionType the type of conditions to evaluate (VISIBILITY or REQUIREMENT)
     * @param conditionsCache optional cache of conditions for performance
     * @return true if all conditions are met, false if not met, null if no conditions found
     */
    private Boolean evaluateFieldConditions(FormItemEntity stepItem, JSONObject received, ConditionType conditionType) {

        List<FormItemsConditionEntity> conditions = null;
        if (conditionsCache != null) {
            Map<ConditionType, List<FormItemsConditionEntity>> byType = conditionsCache.get(stepItem.getId());
            if (byType != null) conditions = byType.get(conditionType);
        }

        if(conditions == null) {
            conditions = formItemsConditionsRepository.findAllByFormItemIdAndConditionTypeAndDomainIdOrderBySortPriorityAsc(stepItem.getId(), conditionType, CloudToolsForCore.getDomainId());
        }

        if (conditions == null || conditions.isEmpty()) return null; // no conditions found

        String sessionPrefix = MultistepFormsService.getSessionKey(formName, request) + "_";

        try {
            Boolean combinedResult = null;
            JoinOperatorType prevJoinOperator = JoinOperatorType.AND;

            for (FormItemsConditionEntity condition : conditions) {
                String fieldId = condition.getItemFormId();
                if (Tools.isEmpty(fieldId)) continue;

                OperatorType operatorType = condition.getOperator();
                String requiredValue = condition.getValue();
                boolean caseInsensitive = Boolean.TRUE.equals(condition.getCaseInsensitive());

                // First check current step data (received JSON), then previous steps from session.
                String actualValue = received.optString(fieldId, null);
                if (actualValue == null) {
                    Object sessionValue = request.getSession().getAttribute(sessionPrefix + fieldId);
                    actualValue = sessionValue != null ? sessionValue.toString() : "";
                }

                boolean met = evaluateOperator(operatorType, actualValue, requiredValue, caseInsensitive);
                if (combinedResult == null) {
                    combinedResult = met;
                } else {
                    if (prevJoinOperator == JoinOperatorType.OR) {
                        combinedResult = combinedResult || met;
                    } else {
                        combinedResult = combinedResult && met;
                    }
                }

                JoinOperatorType joinOperator = condition.getJoinOperatorType();
                prevJoinOperator = joinOperator != null ? joinOperator : JoinOperatorType.AND;
            }

            boolean allMet = combinedResult != null ? combinedResult : true;
            return allMet;
        } catch (Exception e) {
            Logger.debug(MultistepFormsService.class, "Failed to evaluate " + conditionType.name() + " condition for " + stepItem.getItemFormId() + ": " + e.getMessage());
            return false;
        }
    }


        /**
     * Evaluate a single condition operator.
     * @param operator the operator (equals, not_equals, contains, not_contains, empty, not_empty)
     * @param actualValue the actual field value
     * @param requiredValue the expected/required value
     * @return true if the condition is met
     */
    static boolean evaluateOperator(OperatorType operator, String actualValue, String requiredValue) {
        return evaluateOperator(operator, actualValue, requiredValue, false);
    }

    /**
     * Evaluate a single condition operator with optional case-insensitive comparison.
     * @param operator the operator (equals, not_equals, contains, not_contains, empty, not_empty)
     * @param actualValue the actual field value
     * @param requiredValue the expected/required value
     * @param caseInsensitive true if textual comparison should ignore case
     * @return true if the condition is met
     */
    static boolean evaluateOperator(OperatorType operator, String actualValue, String requiredValue, boolean caseInsensitive) {
        if (actualValue == null) actualValue = "";
        if (requiredValue == null) requiredValue = "";
        if (operator == null) operator = OperatorType.EQUALS;

        if (caseInsensitive) {
            actualValue = actualValue.toLowerCase(Locale.ROOT);
            requiredValue = requiredValue.toLowerCase(Locale.ROOT);
        }

        boolean hasActualValue = Tools.isNotEmpty(actualValue);

        switch (operator) {
            case NOT_EQUALS:
                return actualValue.equals(requiredValue) == false;
            case CONTAINS:
                return hasActualValue && actualValue.contains(requiredValue);
            case NOT_CONTAINS:
                return hasActualValue == false || actualValue.contains(requiredValue) == false;
            case STARTS_WITH:
                return hasActualValue && actualValue.startsWith(requiredValue);
            case ENDS_WITH:
                return hasActualValue && actualValue.endsWith(requiredValue);
            case EMPTY:
                return hasActualValue == false;
            case NOT_EMPTY:
                return hasActualValue;
            case EQUALS:
            default:
                return actualValue.equals(requiredValue);
        }
    }

    /**
     * Builds a map of visibility conditions for all items in a step.
     * Each entry maps the item's fieldFormId to its conditions array and server-side hide flag.
     * This separates visibility metadata from HTML rendering.
     *
     * @param stepId ID of the step
     * @return JSONObject where keys are itemFormId and values are objects with "conditions" array and "hidden" boolean
     */
    public final JSONObject getVisibilityConditions(Long stepId) {
        return getConditionalMap(stepId, ConditionType.VISIBILITY, "hidden", false, "visibility");
    }

    /**
     * Build a JSON map of requirement conditions for the given step.
     * Structure mirrors getVisibilityConditions: keys are itemFormId, values contain
     * "conditions" array and "required" boolean (true when cross-step conditions are all met).
     *
     * @param stepId ID of the step
     * @return JSONObject where keys are itemFormId and values are objects with "conditions" array and "required" boolean
     */
    public final JSONObject getRequirementConditions(Long stepId) {
        return getConditionalMap(stepId, ConditionType.REQUIREMENT, "required", true, "requirement");
    }

    private JSONObject getConditionalMap(Long stepId, ConditionType conditionType, String resultKey, boolean resultWhenAllMet, String conditionLabel) {

        JSONObject result = new JSONObject();
        Integer domainId = CloudToolsForCore.getDomainId();

        List<FormItemEntity> stepItems = formItemsRepository.getAllStepItems(stepId, domainId);
        Set<String> currentStepFieldIds = new HashSet<>();
        List<Long> stepItemIds = new ArrayList<>();
        for (FormItemEntity stepItem : stepItems) {
            if (Tools.isNotEmpty(stepItem.getItemFormId())) currentStepFieldIds.add(stepItem.getItemFormId());
            if (stepItem.getId() != null) stepItemIds.add(stepItem.getId());
        }

        Map<Long, List<FormItemsConditionEntity>> conditionsByItemId = new HashMap<>();
        if (stepItemIds.isEmpty() == false) {
            List<FormItemsConditionEntity> allStepConditions = formItemsConditionsRepository.findAllByFormItemIdInAndDomainIdOrderByFormItemIdAscSortPriorityAsc(stepItemIds, domainId);
            for (FormItemsConditionEntity condition : allStepConditions) {
                if (conditionType.equals(condition.getConditionType()) == false) continue;
                conditionsByItemId.computeIfAbsent(condition.getFormItemId(), key -> new ArrayList<>()).add(condition);
            }
        }

        String sessionPrefix = MultistepFormsService.getSessionKey(formName, request) + "_";

        for (FormItemEntity stepItem : stepItems) {
            List<FormItemsConditionEntity> conditionsX = conditionsByItemId.get(stepItem.getId());
            if (conditionsX == null || conditionsX.isEmpty()) continue;

            JSONArray conditions = new JSONArray();
            for (FormItemsConditionEntity condition : conditionsX) {
                JSONObject cond = new JSONObject();
                cond.put("fieldId", condition.getItemFormId());
                cond.put("operator", condition.getOperator().getValue());
                cond.put("value", condition.getValue());
                cond.put("caseInsensitive", Boolean.TRUE.equals(condition.getCaseInsensitive()));
                if (condition.getJoinOperatorType() != null) {
                    cond.put("joinOperator", condition.getJoinOperatorType().name());
                }
                conditions.put(cond);
            }

            boolean conditionResult = false;
            try {

                boolean allFromPreviousSteps = true;
                Boolean combinedResult = null;

                for (int i = 0; i < conditions.length(); i++) {
                    JSONObject cond = conditions.getJSONObject(i);
                    String fieldId = cond.optString("fieldId", "");
                    OperatorType operatorType = OperatorType.fromString(cond.optString("operator", "equals"));
                    String requiredValue = cond.optString("value", "");
                    boolean caseInsensitive = cond.optBoolean("caseInsensitive", false);

                    if (Tools.isEmpty(fieldId)) continue;

                    if (currentStepFieldIds.contains(fieldId)) {
                        allFromPreviousSteps = false;
                        continue;
                    }

                    Object sessionValue = request.getSession().getAttribute(sessionPrefix + fieldId);
                    String storedValue = sessionValue != null ? sessionValue.toString() : "";

                    boolean met = evaluateOperator(operatorType, storedValue, requiredValue, caseInsensitive);

                    if (combinedResult == null) {
                        combinedResult = met;
                    } else {
                        // Use joinOperator from the PREVIOUS condition (postfix operator)
                        JSONObject prevCond = conditions.getJSONObject(i - 1);
                        String joinOp = prevCond.optString("joinOperator", "AND");
                        if ("OR".equalsIgnoreCase(joinOp)) {
                            combinedResult = combinedResult || met;
                        } else {
                            combinedResult = combinedResult && met;
                        }
                    }
                }

                boolean allMet = combinedResult != null ? combinedResult : true;
                conditionResult = allFromPreviousSteps && (resultWhenAllMet ? allMet : !allMet);
            } catch (Exception e) {
                Logger.debug(FormHtmlHandler.class, "Failed to evaluate " + conditionLabel + " condition for item " + stepItem.getItemFormId() + ": " + e.getMessage());
            }

            JSONObject entry = new JSONObject();
            entry.put("conditions", conditions);
            entry.put(resultKey, conditionResult);
            result.put(stepItem.getItemFormId(), entry);
        }

        return result;
    }
}
