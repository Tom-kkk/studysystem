package com.example.stu_backend.web.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将数据库等异常转为可读 JSON，便于本地联调；根因见 {@code detail}。
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex) {
        Throwable root = ex.getMostSpecificCause();
        log.error("数据库访问失败", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", "数据库访问失败：请确认 MySQL 已启动、已执行 report.sql、库名为 report、application.properties 中账号密码正确");
        body.put("detail", root.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
