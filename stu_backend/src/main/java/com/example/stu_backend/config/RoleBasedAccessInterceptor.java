package com.example.stu_backend.config;

import com.example.stu_backend.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;

@Component
public class RoleBasedAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        HttpSession session = request.getSession(false);

        // 登录、公共接口和邮箱验证码接口不需要认证
        if (uri.startsWith("/api/auth/")
                || uri.startsWith("/api/common/")
                || uri.startsWith("/api/email/")) {
            return true;
        }

        // 检查是否已登录
        if (session == null || !SessionUtils.isLoggedIn(session)) {
            sendUnauthorizedResponse(response, "请先登录");
            return false;
        }

        String roleType = SessionUtils.getCurrentRoleType(session);
        String userId = SessionUtils.getCurrentUserId(session);

        // 教师接口：只能由教师访问
        if (uri.startsWith("/api/courses")
                || uri.startsWith("/api/projects")
                || uri.startsWith("/api/teacher/reports")) {
            if (!"teacher".equals(roleType)) {
                sendForbiddenResponse(response, "只有教师可以访问此接口");
                return false;
            }
        }

        // 学生接口：只能由学生访问，且只能访问自己的数据
        if (uri.startsWith("/api/student/reports")) {
            if (!"student".equals(roleType)) {
                sendForbiddenResponse(response, "只有学生可以访问此接口");
                return false;
            }

            // 检查学生是否访问自己的数据（学号匹配）
            String requestedSno = request.getParameter("sno");
            if (requestedSno != null && !requestedSno.trim().equals(userId)) {
                sendForbiddenResponse(response, "只能查看自己的报告");
                return false;
            }
        }

        return true;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write("{\"success\":false,\"message\":\"" + message + "\"}");
        writer.flush();
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write("{\"success\":false,\"message\":\"" + message + "\"}");
        writer.flush();
    }
}
