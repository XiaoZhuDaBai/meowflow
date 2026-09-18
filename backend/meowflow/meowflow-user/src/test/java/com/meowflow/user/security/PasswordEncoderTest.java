package com.meowflow.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new PasswordEncoder();
    }

    @Test
    void encode_shouldReturnNonNullHash() {
        String rawPassword = "testPassword123";
        String encoded = passwordEncoder.encode(rawPassword);
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
    }

    @Test
    void encode_shouldReturnDifferentHashesForSamePassword() {
        String rawPassword = "testPassword123";
        String hash1 = passwordEncoder.encode(rawPassword);
        String hash2 = passwordEncoder.encode(rawPassword);
        assertNotEquals(hash1, hash2);
    }

    @Test
    void encode_shouldReturnBcryptFormattedHash() {
        String rawPassword = "testPassword123";
        String encoded = passwordEncoder.encode(rawPassword);
        assertTrue(encoded.startsWith("$2"));
    }

    @Test
    void matches_shouldReturnTrueForCorrectPassword() {
        String rawPassword = "testPassword123";
        String encoded = passwordEncoder.encode(rawPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
    }

    @Test
    void matches_shouldReturnFalseForIncorrectPassword() {
        String rawPassword = "testPassword123";
        String wrongPassword = "wrongPassword";
        String encoded = passwordEncoder.encode(rawPassword);
        assertFalse(passwordEncoder.matches(wrongPassword, encoded));
    }

    @Test
    void matches_shouldReturnFalseForNullRawPassword() {
        String encoded = passwordEncoder.encode("testPassword123");
        assertFalse(passwordEncoder.matches(null, encoded));
    }

    @Test
    void matches_shouldReturnFalseForNullEncodedPassword() {
        assertFalse(passwordEncoder.matches("testPassword123", null));
    }

    @Test
    void matches_shouldReturnFalseForBothNull() {
        assertFalse(passwordEncoder.matches(null, null));
    }

    @Test
    void matches_shouldBeCaseSensitive() {
        String rawPassword = "TestPassword";
        String encoded = passwordEncoder.encode(rawPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
        assertFalse(passwordEncoder.matches("testpassword", encoded));
    }

    @Test
    void matches_shouldHandleSpecialCharacters() {
        String rawPassword = "P@ssw0rd!#$%^&*()";
        String encoded = passwordEncoder.encode(rawPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
    }

    @Test
    void matches_shouldHandleUnicodeCharacters() {
        String rawPassword = "密码测试123";
        String encoded = passwordEncoder.encode(rawPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
    }

    @Test
    void matches_shouldHandleEmptyPassword() {
        String rawPassword = "";
        String encoded = passwordEncoder.encode(rawPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
    }
}
