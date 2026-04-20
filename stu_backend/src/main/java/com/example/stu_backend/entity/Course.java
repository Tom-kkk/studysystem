package com.example.stu_backend.entity;

import java.io.Serializable;

/**
 * 对应 tb_Course 实体。
 */
public class Course implements Serializable {

    private String courseId;
    private String courseName;
    private Integer numOfProject;
    private String tableName;
    private String teacherId;

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Integer getNumOfProject() {
        return numOfProject;
    }

    public void setNumOfProject(Integer numOfProject) {
        this.numOfProject = numOfProject;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }
}
