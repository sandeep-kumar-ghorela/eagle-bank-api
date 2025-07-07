package com.eaglebank.model.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum AccountType {
    PERSONAL("personal"),
    SAVING("saving");

    String accountType;

     AccountType(String s) {
        this.accountType = s;
    }

    public static String getEnumValues() {
        return Arrays.stream(AccountType.values())
                .map(a -> a.name())
                .collect(Collectors.joining(","));
    }

    public String getAccountType() {
        return accountType;
    }
}
