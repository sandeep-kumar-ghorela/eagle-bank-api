package com.eaglebank.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
@Builder
public class  UserResponse {

    private String userId;

    private String name;

    private AddressResponse address;

    private String phoneNumber;

    private String email;

    private String createdTimestamp;

    private String updatedTimestamp;

}
