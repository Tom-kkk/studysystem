package com.example.stu_backend.dto.response;

import java.time.LocalDateTime;

public class StudentProjectView {

    private String projectId;
    private String projectName;
    private Integer projectOpen;
    private boolean uploaded;
    private String fileName;
    private String uploadStatus;
    private String uploadStatusText;
    private String checkText;
    private boolean canUpload;
    private boolean canCheck;
    private String checkUrl;
    private LocalDateTime projectDeadline;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Integer getProjectOpen() {
        return projectOpen;
    }

    public void setProjectOpen(Integer projectOpen) {
        this.projectOpen = projectOpen;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(String uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public String getUploadStatusText() {
        return uploadStatusText;
    }

    public void setUploadStatusText(String uploadStatusText) {
        this.uploadStatusText = uploadStatusText;
    }

    public String getCheckText() {
        return checkText;
    }

    public void setCheckText(String checkText) {
        this.checkText = checkText;
    }

    public boolean isCanUpload() {
        return canUpload;
    }

    public void setCanUpload(boolean canUpload) {
        this.canUpload = canUpload;
    }

    public boolean isCanCheck() {
        return canCheck;
    }

    public void setCanCheck(boolean canCheck) {
        this.canCheck = canCheck;
    }

    public String getCheckUrl() {
        return checkUrl;
    }

    public void setCheckUrl(String checkUrl) {
        this.checkUrl = checkUrl;
    }

    public LocalDateTime getProjectDeadline() {
        return projectDeadline;
    }

    public void setProjectDeadline(LocalDateTime projectDeadline) {
        this.projectDeadline = projectDeadline;
    }
}
