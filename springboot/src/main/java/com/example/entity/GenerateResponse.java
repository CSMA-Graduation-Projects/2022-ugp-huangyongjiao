package com.example.entity;

import java.math.BigDecimal;

public class GenerateResponse {
    private Integer recordId;
    private String resultText;
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

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
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
