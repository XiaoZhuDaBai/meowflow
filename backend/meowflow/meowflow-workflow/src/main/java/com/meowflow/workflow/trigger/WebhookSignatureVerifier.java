package com.meowflow.workflow.trigger;

import lombok.Data;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Data
public class WebhookSignatureVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public static boolean verify(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            return false;
        }

        try {
            String computed = computeHmacSha256(payload, secret);
            return constantTimeEquals(computed, signature);
        } catch (Exception e) {
            return false;
        }
    }

    public static String computeHmacSha256(String payload, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
