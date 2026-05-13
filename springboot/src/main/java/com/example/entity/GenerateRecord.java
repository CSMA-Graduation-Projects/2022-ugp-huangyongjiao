package com.example.entity;

import java.math.BigDecimal;

public class GenerateRecord {
    private Integer id;
    private Integer functionId;
    private String functionName;
    private String sourceType;
    private String promptText;
    private String resultText;
    private String modelName;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long latencyMs;
    private String strategy;
    private Integer runIndex;
    private Integer previousRecordId;
    private Integer creatorId;
    private String creatorRole;
    private Integer managerId;
    private String createTime;
    private Integer evaluationScore;
    private String normalPathCoverage;
    private String boundaryCoverage;
    private String exceptionCoverage;
    private String syntaxNorm;
    private String suggestionText;
    private Integer testerCaseCount;
    private Integer llmCaseCount;
    private Integer testerEvaluationScore;
    private Double lineCoverage;
    private Double branchCoverage;
    private String coverageStatus;
    private String coverageMessage;
    private String publicCompareResult;
    private Integer publicCoveredCount;
    private Integer publicPartialCount;
    private Integer publicMissingCount;
    private String publicExtraResult;
    private Integer generatedCaseCount;
    private Integer publicMatchedCaseCount;
    private Integer publicExtraCaseCount;
    private Integer publicInvalidCaseCount;
    private BigDecimal publicExtraRate;
    private BigDecimal publicExpandRate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
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

    public Integer getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorRole() {
        return creatorRole;
    }

    public void setCreatorRole(String creatorRole) {
        this.creatorRole = creatorRole;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public Integer getEvaluationScore() {
        return evaluationScore;
    }

    public void setEvaluationScore(Integer evaluationScore) {
        this.evaluationScore = evaluationScore;
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

    public Integer getTesterCaseCount() {
        return testerCaseCount;
    }

    public void setTesterCaseCount(Integer testerCaseCount) {
        this.testerCaseCount = testerCaseCount;
    }

    public Integer getLlmCaseCount() {
        return llmCaseCount;
    }

    public void setLlmCaseCount(Integer llmCaseCount) {
        this.llmCaseCount = llmCaseCount;
    }

    public Integer getTesterEvaluationScore() {
        return testerEvaluationScore;
    }

    public void setTesterEvaluationScore(Integer testerEvaluationScore) {
        this.testerEvaluationScore = testerEvaluationScore;
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

    public String getPublicCompareResult() {
        return publicCompareResult;
    }

    public void setPublicCompareResult(String publicCompareResult) {
        this.publicCompareResult = publicCompareResult;
    }

    public Integer getPublicCoveredCount() {
        return publicCoveredCount;
    }

    public void setPublicCoveredCount(Integer publicCoveredCount) {
        this.publicCoveredCount = publicCoveredCount;
    }

    public Integer getPublicPartialCount() {
        return publicPartialCount;
    }

    public void setPublicPartialCount(Integer publicPartialCount) {
        this.publicPartialCount = publicPartialCount;
    }

    public Integer getPublicMissingCount() {
        return publicMissingCount;
    }

    public void setPublicMissingCount(Integer publicMissingCount) {
        this.publicMissingCount = publicMissingCount;
    }

    public String getPublicExtraResult() {
        return publicExtraResult;
    }

    public void setPublicExtraResult(String publicExtraResult) {
        this.publicExtraResult = publicExtraResult;
    }

    public Integer getGeneratedCaseCount() {
        return generatedCaseCount;
    }

    public void setGeneratedCaseCount(Integer generatedCaseCount) {
        this.generatedCaseCount = generatedCaseCount;
    }

    public Integer getPublicMatchedCaseCount() {
        return publicMatchedCaseCount;
    }

    public void setPublicMatchedCaseCount(Integer publicMatchedCaseCount) {
        this.publicMatchedCaseCount = publicMatchedCaseCount;
    }

    public Integer getPublicExtraCaseCount() {
        return publicExtraCaseCount;
    }

    public void setPublicExtraCaseCount(Integer publicExtraCaseCount) {
        this.publicExtraCaseCount = publicExtraCaseCount;
    }

    public Integer getPublicInvalidCaseCount() {
        return publicInvalidCaseCount;
    }

    public void setPublicInvalidCaseCount(Integer publicInvalidCaseCount) {
        this.publicInvalidCaseCount = publicInvalidCaseCount;
    }

    public BigDecimal getPublicExtraRate() {
        return publicExtraRate;
    }

    public void setPublicExtraRate(BigDecimal publicExtraRate) {
        this.publicExtraRate = publicExtraRate;
    }

    public BigDecimal getPublicExpandRate() {
        return publicExpandRate;
    }

    public void setPublicExpandRate(BigDecimal publicExpandRate) {
        this.publicExpandRate = publicExpandRate;
    }
}
