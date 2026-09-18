package com.meowflow.user.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserCreateRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validRequest_shouldHaveNoViolations() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setNickName("Test User");

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void nullUsername_shouldHaveViolation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername(null);
        request.setPassword("password123");
        request.setNickName("Test User");

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    void blankUsername_shouldHaveViolation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("   ");
        request.setPassword("password123");
        request.setNickName("Test User");

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    @Test
    void invalidUsernameFormat_shouldHaveViolation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("123user");
        request.setPassword("password123");
        request.setNickName("Test User");

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("字母开头")));
    }

    @Test
    void usernameTooShort_shouldHaveViolation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("ab");
        request.setPassword("password123");
        request.setNickName("Test User");

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    @Test
    void nullPassword_shouldHaveViolation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword(null);
        request.setNickName("Test User");

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void passwordTooShort_shouldHaveViolation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("12345");
        request.setNickName("Test User");

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("6-20")));
    }

    @Test
    void passwordTooLong_shouldHaveViolation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("123456789012345678901");
        request.setNickName("Test User");

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("6-20")));
    }

    @Test
    void nullNickName_shouldHaveViolation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setNickName(null);

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nickName")));
    }

    @Test
    void invalidEmail_shouldHaveViolation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setNickName("Test User");
        request.setEmail("invalid-email");

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void validEmail_shouldHaveNoViolation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setNickName("Test User");
        request.setEmail("test@example.com");

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void invalidPhone_shouldHaveViolation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setNickName("Test User");
        request.setPhone("12345");

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("phone")));
    }

    @Test
    void validPhone_shouldHaveNoViolation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setNickName("Test User");
        request.setPhone("13812345678");

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void optionalFieldsCanBeNull() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setNickName("Test User");
        request.setEmail(null);
        request.setPhone(null);
        request.setSex(null);
        request.setStatus(null);
        request.setOrgId(null);
        request.setPostId(null);
        request.setRoleIds(null);
        request.setRemark(null);

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
