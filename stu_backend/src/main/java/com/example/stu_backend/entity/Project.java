package com.example.stu_backend.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对应 tb_Project 实体。
 */
public class Project implements Serializable {

    private String courseId;
    private String projectId;
    private String projectName;
    /** 0=close, 1=open, 2=deadline */
    private Integer projectOpen;
    /** 项目截止时间，可为空。 */
    private LocalDateTime projectDeadline;
    /** 临期提醒是否已发送。 */
    private Boolean deadlineReminderSent;

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

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

    public LocalDateTime getProjectDeadline() {
        return projectDeadline;
    }

    public void setProjectDeadline(LocalDateTime projectDeadline) {
        this.projectDeadline = projectDeadline;
    }

    public Boolean getDeadlineReminderSent() {
        return deadlineReminderSent;
    }

    public void setDeadlineReminderSent(Boolean deadlineReminderSent) {
        this.deadlineReminderSent = deadlineReminderSent;
    }
}
