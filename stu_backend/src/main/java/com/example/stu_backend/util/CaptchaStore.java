package com.example.stu_backend.util;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 前后端分离场景下，验证码不能仅依赖 Session（跨域图片请求默认不带 Cookie）；
 * 使用 captchaId + 服务端缓存保存明文，登录时传入 captchaId 与用户输入做校验。
 */
public final class CaptchaStore {

    private static final long TTL_MS = 5 * 60 * 1000L;

    private static final Map<String, Entry> STORE = new ConcurrentHashMap<>();

    private CaptchaStore() {}

    public static String put(String codePlain) {
        String id = UUID.randomUUID().toString();
        STORE.put(id, new Entry(codePlain.toUpperCase(Locale.ROOT), System.currentTimeMillis() + TTL_MS));
        purgeExpired();
        return id;
    }

    /**
     * @return 校验成功并消费该条记录时返回 true
     */
    public static boolean verifyAndRemove(String captchaId, String userInput) {
        if (captchaId == null || captchaId.isBlank() || userInput == null) {
            return false;
        }
        Entry e = STORE.remove(captchaId);
        if (e == null) {
            return false;
        }
        if (System.currentTimeMillis() > e.expiresAtMs) {
            return false;
        }
        return e.code.equals(userInput.trim().toUpperCase(Locale.ROOT));
    }

    private static void purgeExpired() {
        long now = System.currentTimeMillis();
        STORE.entrySet().removeIf(en -> en.getValue().expiresAtMs < now);
    }

    private record Entry(String code, long expiresAtMs) {}
}
