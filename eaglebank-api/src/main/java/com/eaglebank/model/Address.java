package com.eaglebank.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class Address {
    @NotBlank(message = "Line1 is a required field")
    private String line1;
    private String line2;
    private String line3;
    @NotBlank(message = "Town is a required field")
    private String town;
    @NotBlank(message = "county is a required field")
    private String county;
    @NotBlank(message = "Post code is a required field")
    private String postCode;
}
