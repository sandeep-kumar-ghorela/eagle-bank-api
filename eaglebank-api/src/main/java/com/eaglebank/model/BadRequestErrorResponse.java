package com.eaglebank.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
public class BadRequestErrorResponse {
    private String message;
    private List<ErrorDetail> details;
}