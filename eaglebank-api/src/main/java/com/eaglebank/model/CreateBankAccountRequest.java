package com.eaglebank.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateBankAccountRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Account type is required")
    private String accountType;

}