package com.example.stu_backend.web.common;

import com.example.stu_backend.util.EmailUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 临时联调接口：发送邮箱验证码，并把验证码写入 Session（key: emailCheckcode）。
 */
@RestController
@RequestMapping("/api/email")
public class EmailCheckcodeController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final EmailUtil emailUtil;

    public EmailCheckcodeController(EmailUtil emailUtil) {
        this.emailUtil = emailUtil;
    }

    @PostMapping("/checkcode")
    public ResponseEntity<Map<String, Object>> sendCheckcode(@RequestParam String email, HttpSession session) {
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "邮箱格式不正确"));
        }
        try {
            String checkCode = emailUtil.sendEmail(email.trim());
            session.setAttribute("emailCheckcode", checkCode);
            session.setAttribute("email", email.trim());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "验证码发送成功"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "验证码发送失败，请检查邮箱配置或稍后重试",
                    "detail", ex.getMessage()));
        }
    }
}
