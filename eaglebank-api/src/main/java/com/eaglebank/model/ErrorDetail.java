package com.eaglebank.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class ErrorDetail {
    private String field;
    private String message;
    private String type;
}