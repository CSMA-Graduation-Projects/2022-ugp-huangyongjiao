package com.example.entity;

public class FunctionInfo {
    private Integer id;
    private String functionName;
    private String className;
    private String language;
    private String codeText;
    private String inputDesc;
    private String outputDesc;
    private String remark;
    private Integer testerCaseCount;
    private String publicTestContent;
    private String publicTestSource;
    private Integer publicAssertCount;
    private Integer creatorId;
    private String creatorRole;
    private Integer managerId;
    private String createTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Integer getTesterCaseCount() {
        return testerCaseCount;
    }

    public void setTesterCaseCount(Integer testerCaseCount) {
        this.testerCaseCount = testerCaseCount;
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
}
