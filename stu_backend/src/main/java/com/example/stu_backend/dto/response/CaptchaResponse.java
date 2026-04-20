package com.example.stu_backend.dto.response;

/**
 * 验证码接口返回：前端可将 {@code imageBase64} 拼为 {@code data:image/png;base64,} + imageBase64 显示。
 */
public record CaptchaResponse(String captchaId, String imageBase64) {}
