package com.eaglebank.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class BankAccountResponse {

    private String accountNumber;

    private String sortCode;

    private String name;

    private String accountType;

    private BigDecimal balance;

    private String currency;

    private String createdTimestamp;

    private String updatedTimestamp;

}