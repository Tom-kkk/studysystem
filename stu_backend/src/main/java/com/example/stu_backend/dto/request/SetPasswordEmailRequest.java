package com.example.stu_backend.dto.request;

/**
 * 对齐教程 SetPasswordEmail.do：首次登录时校验旧密码，设置新密码与邮箱。
 */
public record SetPasswordEmailRequest(
        String userName,
        String oldPass,
        String newPass,
        String confirmPass,
        String email,
        String emailCheckcode
) {}
