package com.example.stu_backend.web.user;

import com.example.stu_backend.dto.request.LoginRequest;
import com.example.stu_backend.dto.request.ForgetPasswordRequest;
import com.example.stu_backend.dto.request.SetPasswordEmailRequest;
import com.example.stu_backend.dto.response.UserResponse;
import com.example.stu_backend.entity.User;
import com.example.stu_backend.service.UserService;
import com.example.stu_backend.util.Md5Util;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * 用户相关 HTTP 接口，便于 Postman / curl 手动联调。
 */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 登录：请求体传明文口令，服务端按教程做 MD5 后与库中 {@code user_password} 比较。
     */
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body, HttpSession session) {
        if (body == null || body.userName() == null || body.userName().isBlank()
                || body.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少用户名或密码"));
        }
        Optional<User> user = userService.loginWithPlainPassword(body.userName().trim(), body.password());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "用户名或口令错误"));
        }

        User loggedInUser = user.get();
        session.setAttribute("currentUser", loggedInUser);
        session.setAttribute("userId", loggedInUser.getUserName());
        session.setAttribute("roleType", loggedInUser.getRoleType());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "user", UserResponse.from(loggedInUser)));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true, "message", "登出成功"));
    }

    /**
     * 按用户名查询（库中有记录则返回，不含口令）。
     */
    @GetMapping("/users/{userName}")
    public ResponseEntity<?> getByUserName(@PathVariable String userName) {
        Optional<User> user = userService.findByUserName(userName);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "用户不存在"));
        }
        return ResponseEntity.ok(UserResponse.from(user.get()));
    }

    /**
     * 对齐教程 SetPasswordEmail.do：
     * 首次登录时，校验旧密码 + 邮箱验证码，设置新密码与预留邮箱。
     */
    @PostMapping("/auth/set-password-email")
    public ResponseEntity<?> setPasswordEmail(@RequestBody SetPasswordEmailRequest body, HttpSession session) {
        if (body == null || isBlank(body.userName()) || isBlank(body.oldPass()) || isBlank(body.newPass())
                || isBlank(body.confirmPass()) || isBlank(body.email()) || isBlank(body.emailCheckcode())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "参数不完整"));
        }
        if (body.newPass().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "新密码长度不足（至少6位）"));
        }
        if (!body.newPass().equals(body.confirmPass())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "两次新密码不一致"));
        }
        String sessionCode = (String) session.getAttribute("emailCheckcode");
        if (sessionCode == null || !body.emailCheckcode().equalsIgnoreCase(sessionCode)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "邮箱验证码错误或已失效"));
        }

        Optional<User> userOpt = userService.findByUserName(body.userName().trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "用户不存在"));
        }
        User user = userOpt.get();
        Optional<User> checkOld = userService.login(body.userName().trim(), Md5Util.md5Hex(body.oldPass()));
        if (checkOld.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "原密码错误"));
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "该账号已设置预留邮箱"));
        }

        user.setEmail(body.email().trim());
        user.setUserPassword(Md5Util.md5Hex(body.newPass()));
        boolean ok = userService.updatePasswordAndEmail(user);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "保存失败"));
        }
        session.removeAttribute("emailCheckcode");
        return ResponseEntity.ok(Map.of("success", true, "message", "密码与邮箱设置成功"));
    }

    /**
     * 对齐教程 forgetPass.do：
     * 用户名 + 预留邮箱 + 邮箱验证码重置口令。
     */
    @PostMapping("/auth/forget-password")
    public ResponseEntity<?> forgetPassword(@RequestBody ForgetPasswordRequest body, HttpSession session) {
        if (body == null || isBlank(body.userName()) || isBlank(body.email()) || isBlank(body.emailCheckcode())
                || isBlank(body.newPass()) || isBlank(body.confirmPass())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "参数不完整"));
        }
        if (body.newPass().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "新密码长度不足（至少6位）"));
        }
        if (!body.newPass().equals(body.confirmPass())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "两次新密码不一致"));
        }
        String sessionCode = (String) session.getAttribute("emailCheckcode");
        if (sessionCode == null || !body.emailCheckcode().equalsIgnoreCase(sessionCode)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "邮箱验证码错误或已失效"));
        }

        Optional<User> userOpt = userService.findByUserName(body.userName().trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "用户名错误"));
        }
        User user = userOpt.get();
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(body.email().trim())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "邮箱与预留邮箱不一致"));
        }

        boolean ok = userService.updatePassword(user.getUserName(), Md5Util.md5Hex(body.newPass()));
        if (!ok) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "密码更新失败"));
        }
        session.removeAttribute("emailCheckcode");
        return ResponseEntity.ok(Map.of("success", true, "message", "密码重置成功"));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
