package com.example.entity;

import java.util.ArrayList;
import java.util.List;

public class EvaluationResult {
    private String generatedTestCase;
    private String normalPathCoverage;
    private String boundaryCoverage;
    private String exceptionCoverage;
    private String syntaxNorm;
    private Integer score;
    private List<String> suggestions = new ArrayList<>();
    private Double lineCoverage;
    private Double branchCoverage;
    private String coverageStatus;
    private String coverageMessage;

    public String getGeneratedTestCase() {
        return generatedTestCase;
    }

    public void setGeneratedTestCase(String generatedTestCase) {
        this.generatedTestCase = generatedTestCase;
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

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
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
