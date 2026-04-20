package com.example.stu_backend.entity;

import java.io.Serializable;

/**
 * 与 {@code report.sql} 中 {@code tb_User} 字段一致。
 */
public class User implements Serializable {

    private String userName;
    private String userPassword;
    private String fullName;
    private String email;
    /** 对应列 {@code role_type}，如 student / teacher */
    private String roleType;
    /** 对应列 {@code attemps}（脚本拼写） */
    private Integer attemps;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public Integer getAttemps() {
        return attemps;
    }

    public void setAttemps(Integer attemps) {
        this.attemps = attemps;
    }
}
