package com.eaglebank.model.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class AuthRequest {
    private String username;
    private String password;
}