package com.example.stu_backend.util;

import com.example.stu_backend.entity.User;
import jakarta.servlet.http.HttpSession;

public class SessionUtils {

    private static final String CURRENT_USER_KEY = "currentUser";
    private static final String USER_ID_KEY = "userId";
    private static final String ROLE_TYPE_KEY = "roleType";

    public static User getCurrentUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute(CURRENT_USER_KEY);
    }

    public static String getCurrentUserId(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute(USER_ID_KEY);
    }

    public static String getCurrentRoleType(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute(ROLE_TYPE_KEY);
    }

    public static boolean isLoggedIn(HttpSession session) {
        return getCurrentUser(session) != null;
    }

    public static boolean isTeacher(HttpSession session) {
        return "teacher".equals(getCurrentRoleType(session));
    }

    public static boolean isStudent(HttpSession session) {
        return "student".equals(getCurrentRoleType(session));
    }

    public static void clearSession(HttpSession session) {
        if (session != null) {
            session.removeAttribute(CURRENT_USER_KEY);
            session.removeAttribute(USER_ID_KEY);
            session.removeAttribute(ROLE_TYPE_KEY);
        }
    }
}
