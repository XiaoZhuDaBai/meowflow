package com.meowflow.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private Long orgId;
    private String orgName;
    private String email;
    private List<String> roles;
    private List<String> permissions;
    private Map<String, Object> ext = new HashMap<>();

    public static UserContext anonymous() {
        return UserContext.builder()
                .userId(0L)
                .username("anonymous")
                .build();
    }
}