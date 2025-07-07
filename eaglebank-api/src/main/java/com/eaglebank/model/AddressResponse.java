package com.eaglebank.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AddressResponse {
    private String line1;

    private String line2;

    private String line3;

    private String town;

    private String county;

    private String postcode;

}