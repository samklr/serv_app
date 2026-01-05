package com.servantin.api.dto.auth;

import com.servantin.api.domain.model.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private long expiresIn;
    private UserDto user;

    @Data
    @Builder
    public static class UserDto {
        private UUID id;
        private String email;
        private String name;
        private UserRole role;
        private boolean hasProviderProfile;
    }
}
