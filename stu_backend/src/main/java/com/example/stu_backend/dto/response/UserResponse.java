package com.example.stu_backend.dto.response;

import com.example.stu_backend.entity.User;

/**
 * 返回给前端：不含口令。
 */
public record UserResponse(
        String userName,
        String fullName,
        String email,
        String roleType
) {
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getUserName(),
                user.getFullName(),
                user.getEmail(),
                user.getRoleType());
    }
}
