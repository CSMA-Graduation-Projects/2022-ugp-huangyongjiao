package com.example.entity;

import java.util.List;

public class BatchExperimentResponse {
    private String batchNo;
    private Integer functionCount;
    private Integer totalTaskCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer averageEvaluationScore;
    private Integer averageTesterScore;
    private String outputLanguage;
    private Integer runCount;
    private List<BatchExperimentModelStat> modelStats;
    private List<BatchExperimentItem> items;

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Integer getFunctionCount() {
        return functionCount;
    }

    public void setFunctionCount(Integer functionCount) {
        this.functionCount = functionCount;
    }

    public Integer getTotalTaskCount() {
        return totalTaskCount;
    }

    public void setTotalTaskCount(Integer totalTaskCount) {
        this.totalTaskCount = totalTaskCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public Integer getAverageEvaluationScore() {
        return averageEvaluationScore;
    }

    public void setAverageEvaluationScore(Integer averageEvaluationScore) {
        this.averageEvaluationScore = averageEvaluationScore;
    }

    public Integer getAverageTesterScore() {
        return averageTesterScore;
    }

    public void setAverageTesterScore(Integer averageTesterScore) {
        this.averageTesterScore = averageTesterScore;
    }

    public String getOutputLanguage() {
        return outputLanguage;
    }

    public void setOutputLanguage(String outputLanguage) {
        this.outputLanguage = outputLanguage;
    }

    public Integer getRunCount() {
        return runCount;
    }

    public void setRunCount(Integer runCount) {
        this.runCount = runCount;
    }

    public List<BatchExperimentModelStat> getModelStats() {
        return modelStats;
    }

    public void setModelStats(List<BatchExperimentModelStat> modelStats) {
        this.modelStats = modelStats;
    }

    public List<BatchExperimentItem> getItems() {
        return items;
    }

    public void setItems(List<BatchExperimentItem> items) {
        this.items = items;
    }
}
