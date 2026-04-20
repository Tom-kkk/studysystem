package com.example.stu_backend.dto.request;

/**
 * 对齐教程 forgetPass.do：用户名 + 预留邮箱 + 邮箱验证码重置密码。
 */
public record ForgetPasswordRequest(
        String userName,
        String email,
        String emailCheckcode,
        String newPass,
        String confirmPass
) {}
