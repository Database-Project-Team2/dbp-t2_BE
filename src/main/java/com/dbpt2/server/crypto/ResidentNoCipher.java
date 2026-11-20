package com.dbpt2.server.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 주민번호 AES-256-GCM 암복호화. 저장 형식은 {@code aesgcm:base64(IV 12바이트 || 암호문+태그)}.
 * 키는 환경변수 RESIDENT_NO_KEY(base64 32바이트)로만 받는다.
 */
@Component
public class ResidentNoCipher {

    private static final String PREFIX = "aesgcm:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public ResidentNoCipher(@Value("${app.resident-no-key}") String base64Key) {
        byte[] bytes = Base64.getDecoder().decode(base64Key.trim());
        if (bytes.length != 32) {
            throw new IllegalStateException("RESIDENT_NO_KEY는 base64로 인코딩한 32바이트여야 합니다.");
        }
        this.key = new SecretKeySpec(bytes, "AES");
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("주민번호 암호화에 실패했습니다.", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            throw new IllegalArgumentException("지원하지 않는 암호문 형식입니다.");
        }
        try {
            byte[] in = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            return new String(cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException("주민번호를 복호화할 수 없습니다.", e);
        }
    }

    /** 응답용 YYMMDD-*******. 키가 달라 복호화할 수 없으면 앞자리도 숨긴다. */
    public String mask(String stored) {
        try {
            return decrypt(stored).replace("-", "").substring(0, 6) + "-*******";
        } catch (IllegalArgumentException e) {
            return "******-*******";
        }
    }
}
