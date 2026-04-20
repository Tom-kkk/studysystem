package com.example.stu_backend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * POST /api/auth/login 请求体。
 * 兼容 JSON 字段名 {@code userName} 与 {@code username}。
 */
public record LoginRequest(
        @JsonAlias({"username"}) String userName,
        String password
) {}
