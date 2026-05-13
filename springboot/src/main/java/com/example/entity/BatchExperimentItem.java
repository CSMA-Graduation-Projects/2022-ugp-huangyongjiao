package com.example.entity;

import java.util.ArrayList;
import java.util.List;

public class BatchExperimentItem {
    private Integer functionId;
    private String functionName;
    private String className;
    private String language;
    private String remark;
    private Integer modelConfigId;
    private String modelName;
    private String strategy;
    private Integer runIndex;
    private Integer previousRecordId;
    private String outputLanguage;
    private Integer recordId;
    private String createTime;
    private Integer generatedCaseCount;
    private Integer evaluationScore;
    private Integer testerEvaluationScore;
    private String status;
    private String message;
    private String resultText;
    private String normalPathCoverage;
    private String boundaryCoverage;
    private String exceptionCoverage;
    private String syntaxNorm;
    private String suggestionText;
    private List<String> suggestions = new ArrayList<>();
    private Double lineCoverage;
    private Double branchCoverage;
    private String coverageStatus;
    private String coverageMessage;

    public Integer getFunctionId() {
        return functionId;
    }

    public void setFunctionId(Integer functionId) {
        this.functionId = functionId;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getModelConfigId() {
        return modelConfigId;
    }

    public void setModelConfigId(Integer modelConfigId) {
        this.modelConfigId = modelConfigId;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
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

    public String getOutputLanguage() {
        return outputLanguage;
    }

    public void setOutputLanguage(String outputLanguage) {
        this.outputLanguage = outputLanguage;
    }

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public Integer getGeneratedCaseCount() {
        return generatedCaseCount;
    }

    public void setGeneratedCaseCount(Integer generatedCaseCount) {
        this.generatedCaseCount = generatedCaseCount;
    }

    public Integer getEvaluationScore() {
        return evaluationScore;
    }

    public void setEvaluationScore(Integer evaluationScore) {
        this.evaluationScore = evaluationScore;
    }

    public Integer getTesterEvaluationScore() {
        return testerEvaluationScore;
    }

    public void setTesterEvaluationScore(Integer testerEvaluationScore) {
        this.testerEvaluationScore = testerEvaluationScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
    }

    public String getNormalPathCoverage() {
        return normalPathCoverage;
    }

    public void setNormalPathCoverage(String normalPathCoverage) {
        this.normalPathCoverage = normalPathCoverage;
    }

    public String getBoundaryCoverage() {
        return boundaryCoverage;
    }

    public void setBoundaryCoverage(String boundaryCoverage) {
        this.boundaryCoverage = boundaryCoverage;
    }

    public String getExceptionCoverage() {
        return exceptionCoverage;
    }

    public void setExceptionCoverage(String exceptionCoverage) {
        this.exceptionCoverage = exceptionCoverage;
    }

    public String getSyntaxNorm() {
        return syntaxNorm;
    }

    public void setSyntaxNorm(String syntaxNorm) {
        this.syntaxNorm = syntaxNorm;
    }

    public String getSuggestionText() {
        return suggestionText;
    }

    public void setSuggestionText(String suggestionText) {
        this.suggestionText = suggestionText;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public Double getLineCoverage() {
        return lineCoverage;
    }

    public void setLineCoverage(Double lineCoverage) {
        this.lineCoverage = lineCoverage;
    }

    public Double getBranchCoverage() {
        return branchCoverage;
    }

    public void setBranchCoverage(Double branchCoverage) {
        this.branchCoverage = branchCoverage;
    }

    public String getCoverageStatus() {
        return coverageStatus;
    }

    public void setCoverageStatus(String coverageStatus) {
        this.coverageStatus = coverageStatus;
    }

    public String getCoverageMessage() {
        return coverageMessage;
    }

    public void setCoverageMessage(String coverageMessage) {
        this.coverageMessage = coverageMessage;
    }
}
