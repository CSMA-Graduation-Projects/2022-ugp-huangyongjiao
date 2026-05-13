package com.example.entity;

import java.util.List;

public class BatchExperimentRequest {
    private List<Integer> functionIds;
    private List<Integer> modelConfigIds;
    private List<String> strategies;
    private Integer runCount;
    private String outputLanguage;
    private Integer evaluationModelConfigId;
    private Integer currentUserId;
    private String currentUserRole;

    public List<Integer> getFunctionIds() {
        return functionIds;
    }

    public void setFunctionIds(List<Integer> functionIds) {
        this.functionIds = functionIds;
    }

    public List<Integer> getModelConfigIds() {
        return modelConfigIds;
    }

    public void setModelConfigIds(List<Integer> modelConfigIds) {
        this.modelConfigIds = modelConfigIds;
    }

    public List<String> getStrategies() {
        return strategies;
    }

    public void setStrategies(List<String> strategies) {
        this.strategies = strategies;
    }

    public Integer getRunCount() {
        return runCount;
    }

    public void setRunCount(Integer runCount) {
        this.runCount = runCount;
    }

    public String getOutputLanguage() {
        return outputLanguage;
    }

    public void setOutputLanguage(String outputLanguage) {
        this.outputLanguage = outputLanguage;
    }

    public Integer getEvaluationModelConfigId() {
        return evaluationModelConfigId;
    }

    public void setEvaluationModelConfigId(Integer evaluationModelConfigId) {
        this.evaluationModelConfigId = evaluationModelConfigId;
    }

    public Integer getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(Integer currentUserId) {
        this.currentUserId = currentUserId;
    }

    public String getCurrentUserRole() {
        return currentUserRole;
    }

    public void setCurrentUserRole(String currentUserRole) {
        this.currentUserRole = currentUserRole;
    }
}
