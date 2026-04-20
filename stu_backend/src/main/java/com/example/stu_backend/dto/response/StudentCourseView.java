package com.example.stu_backend.dto.response;

import java.util.List;

public class StudentCourseView {

    private String courseId;
    private List<StudentProjectView> projects;

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public List<StudentProjectView> getProjects() {
        return projects;
    }

    public void setProjects(List<StudentProjectView> projects) {
        this.projects = projects;
    }
}
