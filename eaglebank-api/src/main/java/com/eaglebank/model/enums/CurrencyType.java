package com.eaglebank.model.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum CurrencyType {
    GBP("gbp"),
    USD("usd");

    String currency;
    CurrencyType(String s) {
        currency = s;
    }
    public String getCurrency() {
        return currency;
    }

    public static String getEnumValues() {
        return Arrays.stream(CurrencyType.values())
                .map(a -> a.name())
                .collect(Collectors.joining(","));
    }

}
