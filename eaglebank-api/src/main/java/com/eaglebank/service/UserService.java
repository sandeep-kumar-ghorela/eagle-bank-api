package com.eaglebank.service;

import com.eaglebank.model.CreateUserRequest;
import com.eaglebank.model.UserResponse;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    UserResponse fetchUserByID(String userId);
}
