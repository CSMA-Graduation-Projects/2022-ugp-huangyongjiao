package com.example.entity;

public class CoverageResult {
    private boolean success;
    private Double lineCoverage;
    private Double branchCoverage;
    private String status;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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
}
