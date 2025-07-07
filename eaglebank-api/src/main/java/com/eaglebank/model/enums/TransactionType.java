package com.eaglebank.model.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum TransactionType {
    DEPOSIT("deposit"),
    WITHDRAW("withdraw");
    String label;

    TransactionType(String st) {
        label = st;
    }

    public String getTransactionType() {
        return label;
    }

    public static String getEnumValues() {
        return Arrays.stream(TransactionType.values())
                .map(a -> a.name())
                .collect(Collectors.joining(","));
    }
}
