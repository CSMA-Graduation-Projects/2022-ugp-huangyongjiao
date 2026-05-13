package com.example.entity;

public class GenerateRequest {
    private Integer modelConfigId;
    private Integer evaluationModelConfigId;
    private String strategy;

    private String sourceType;
    private String outputLanguage;

    private Integer functionId;
    private Integer testerCaseCount;

    private String functionName;
    private String className;
    private String language;
    private String codeText;
    private String inputDesc;
    private String outputDesc;
    private String remark;
    private String publicTestContent;
    private String publicTestSource;
    private Integer publicAssertCount;

    private String requirementName;
    private String moduleName;
    private String requirementContent;
    private String preconditionDesc;
    private String expectedDesc;

    private String resultText;
    private Integer currentUserId;
    private String currentUserRole;

    private Boolean regenerate;
    private String previousResultText;
    private String previousEvaluationSummary;
    private Integer runIndex;
    private Integer previousRecordId;
    private Integer recordId;

    public Integer getModelConfigId() {
        return modelConfigId;
    }

    public void setModelConfigId(Integer modelConfigId) {
        this.modelConfigId = modelConfigId;
    }

    public Integer getEvaluationModelConfigId() {
        return evaluationModelConfigId;
    }

    public void setEvaluationModelConfigId(Integer evaluationModelConfigId) {
        this.evaluationModelConfigId = evaluationModelConfigId;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getOutputLanguage() {
        return outputLanguage;
    }

    public void setOutputLanguage(String outputLanguage) {
        this.outputLanguage = outputLanguage;
    }

    public Integer getFunctionId() {
        return functionId;
    }

    public void setFunctionId(Integer functionId) {
        this.functionId = functionId;
    }

    public Integer getTesterCaseCount() {
        return testerCaseCount;
    }

    public void setTesterCaseCount(Integer testerCaseCount) {
        this.testerCaseCount = testerCaseCount;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCodeText() {
        return codeText;
    }

    public void setCodeText(String codeText) {
        this.codeText = codeText;
    }

    public String getInputDesc() {
        return inputDesc;
    }

    public void setInputDesc(String inputDesc) {
        this.inputDesc = inputDesc;
    }

    public String getOutputDesc() {
        return outputDesc;
    }

    public void setOutputDesc(String outputDesc) {
        this.outputDesc = outputDesc;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getPublicTestContent() {
        return publicTestContent;
    }

    public void setPublicTestContent(String publicTestContent) {
        this.publicTestContent = publicTestContent;
    }

    public String getPublicTestSource() {
        return publicTestSource;
    }

    public void setPublicTestSource(String publicTestSource) {
        this.publicTestSource = publicTestSource;
    }

    public Integer getPublicAssertCount() {
        return publicAssertCount;
    }

    public void setPublicAssertCount(Integer publicAssertCount) {
        this.publicAssertCount = publicAssertCount;
    }

    public String getRequirementName() {
        return requirementName;
    }

    public void setRequirementName(String requirementName) {
        this.requirementName = requirementName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getRequirementContent() {
        return requirementContent;
    }

    public void setRequirementContent(String requirementContent) {
        this.requirementContent = requirementContent;
    }

    public String getPreconditionDesc() {
        return preconditionDesc;
    }

    public void setPreconditionDesc(String preconditionDesc) {
        this.preconditionDesc = preconditionDesc;
    }

    public String getExpectedDesc() {
        return expectedDesc;
    }

    public void setExpectedDesc(String expectedDesc) {
        this.expectedDesc = expectedDesc;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
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

    public Boolean getRegenerate() {
        return regenerate;
    }

    public void setRegenerate(Boolean regenerate) {
        this.regenerate = regenerate;
    }

    public String getPreviousResultText() {
        return previousResultText;
    }

    public void setPreviousResultText(String previousResultText) {
        this.previousResultText = previousResultText;
    }

    public String getPreviousEvaluationSummary() {
        return previousEvaluationSummary;
    }

    public void setPreviousEvaluationSummary(String previousEvaluationSummary) {
        this.previousEvaluationSummary = previousEvaluationSummary;
    }

    public Integer getRunIndex() {
        return runIndex;
    }

    public void setRunIndex(Integer runIndex) {
        this.runIndex = runIndex;
    }

    public Integer getPreviousRecordId() {
        return previousRecordId;
    }

    public void setPreviousRecordId(Integer previousRecordId) {
        this.previousRecordId = previousRecordId;
    }

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }
}
