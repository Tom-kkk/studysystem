package com.example.stu_backend.service;

import com.example.stu_backend.entity.User;
import com.example.stu_backend.repository.UserRepository;
import com.example.stu_backend.util.Md5Util;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户业务入口，封装 {@link UserRepository}（JdbcTemplate），替代教程中直接 new {@code UserDAO} 的用法。
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 登录校验：用户名 + 口令（通常为 MD5 后）匹配则返回用户实体 */
    public Optional<User> login(String userName, String userPasswordMd5) {
        return userRepository.findByUserNameAndPassword(userName, userPasswordMd5);
    }

    /** 明文口令登录：内部 MD5 后与库中字段比较（与教程 LoginServlet 一致）。 */
    public Optional<User> loginWithPlainPassword(String userName, String plainPassword) {
        if (plainPassword == null) {
            return Optional.empty();
        }
        return login(userName, Md5Util.md5Hex(plainPassword));
    }

    /** 仅按用户名查询 */
    public Optional<User> findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }

    /** 更新口令与邮箱 */
    public boolean updatePasswordAndEmail(User user) {
        if (user == null || user.getUserName() == null) {
            return false;
        }
        int n = userRepository.updatePasswordAndEmail(
                user.getUserName(),
                user.getUserPassword(),
                user.getEmail());
        return n > 0;
    }

    /** 仅更新口令（忘记密码流程）。 */
    public boolean updatePassword(String userName, String userPasswordMd5) {
        if (userName == null || userName.isBlank() || userPasswordMd5 == null || userPasswordMd5.isBlank()) {
            return false;
        }
        return userRepository.updatePassword(userName, userPasswordMd5) > 0;
    }
}
