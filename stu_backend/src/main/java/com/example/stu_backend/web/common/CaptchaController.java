package com.example.stu_backend.web.common;

import com.example.stu_backend.dto.response.CaptchaResponse;
import com.example.stu_backend.util.CaptchaStore;
import com.example.stu_backend.util.CheckcodeUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/**
 * 替代教程中的 CheckcodeServlet：前后端分离下返回 JSON（captchaId + Base64 图片），
 * 登录时提交 captchaId 与用户输入，由 {@link CaptchaStore#verifyAndRemove} 校验。
 */
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public CaptchaResponse captcha() {
        Map<String, Object> map = CheckcodeUtil.getCheckCodeAndImage();
        String checkCode = (String) map.get("checkCode");
        BufferedImage image = (BufferedImage) map.get("image");
        String captchaId = CaptchaStore.put(checkCode);
        String base64 = encodePngBase64(image);
        return new CaptchaResponse(captchaId, base64);
    }

    private static String encodePngBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("验证码图片编码失败", e);
        }
    }
}
