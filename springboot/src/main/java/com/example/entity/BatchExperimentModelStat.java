package com.example.entity;

public class BatchExperimentModelStat {
    private Integer modelConfigId;
    private String modelName;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer averageEvaluationScore;
    private Integer averageTesterScore;

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

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
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
}
