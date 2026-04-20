package com.example.stu_backend.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 属性工具类：从 classpath 加载 {@code config.properties}，与教程中从类路径读取配置的方式一致；
 * 使用 {@link InputStream} 避免 JAR/WAR 打包后 {@code FileInputStream} 路径失效。
 */
public final class PropertiesUtil {

    public static final Properties pro = new Properties();

    static {
        try (InputStream is = PropertiesUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) {
                throw new IllegalStateException("classpath 中未找到 config.properties");
            }
            pro.load(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private PropertiesUtil() {}
}
