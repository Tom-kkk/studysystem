package com.example.stu_backend.entity;

/**
 * 教学班中一条学生记录（来自 Excel）。
 */
public class ClassStudent {

    private Integer id;
    private String sno;
    private String studentName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSno() {
        return sno;
    }

    public void setSno(String sno) {
        this.sno = sno;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
}
