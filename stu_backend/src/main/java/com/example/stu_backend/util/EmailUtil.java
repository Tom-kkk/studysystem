package com.example.stu_backend.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Random;

/**
 * 发送验证码邮件，逻辑与教程 {@code utils.EmailUtil}（javax.mail + Transport）一致；
 * 在 Spring Boot 中使用 {@link JavaMailSender}，包名为 {@code jakarta.mail.*}（对应原 {@code javax.mail}）。
 */
@Component
public class EmailUtil {

    private static final String CHARS =
            "OPASDFGHQWERTYKLZXCVUIJBNM7014368259cvbnmlkjzxhgfrtyuiopdsaqwe";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.mail.from-display-name:实验报告系统}")
    private String fromDisplayName;

    @Value("${app.mail.subject:实验报告系统验证码}")
    private String subject;

    public EmailUtil(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** 生成随机 n 位验证码（与教程一致）。 */
    public String getCheckcode(int codeCount) {
        StringBuilder code = new StringBuilder();
        Random ran = new Random();
        for (int i = 0; i < codeCount; i++) {
            code.append(CHARS.charAt(ran.nextInt(CHARS.length())));
        }
        return code.toString();
    }

    /**
     * 发送验证码邮件，返回本次生成的验证码（由调用方写入 Session，例如 {@code emailCheckcode}），
     * 切勿把验证码返回给前端 JSON。
     */
    public String sendEmail(String to) throws MessagingException, UnsupportedEncodingException {
        String checkCode = getCheckcode(4);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(new InternetAddress(from, fromDisplayName, "UTF-8"));
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText("验证码是：" + checkCode + " 如非本人操作，请检查账号安全。", true);
        helper.getMimeMessage().setSentDate(new Date());
        mailSender.send(message);
        return checkCode;
    }

    public void sendPlainEmail(String to, String customSubject, String content)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(new InternetAddress(from, fromDisplayName, "UTF-8"));
        helper.setTo(to);
        helper.setSubject(customSubject);
        helper.setText(content, false);
        helper.getMimeMessage().setSentDate(new Date());
        mailSender.send(message);
    }
}
