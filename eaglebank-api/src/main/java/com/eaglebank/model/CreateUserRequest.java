package com.eaglebank.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class  CreateUserRequest {
  @NotBlank(message = "Name is a required field")
  private String name;
  @NotNull(message = "Address is a required field")
  @Valid
  private Address address;
  @NotBlank(message = "Phone number is a required field")
  @Pattern(regexp = "^\\+[1-9]\\d{1,14}$")
  private String phoneNumber;
  @NotBlank(message = "Email is a required field")
  private String email;
  @NotBlank(message = "Password is a required field")
  private String password;
}

