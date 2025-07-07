package com.eaglebank.constants;

public class EagleBankApiConstants {
    // REGEX
    public final static String ACCOUNT_NUMBER_REGEX = "^01\\d{6}$";
    public final static String TRANSACTION_ID_REGEX = "^tan-[A-Za-z0-9]+$";
    public final static String USER_ID_REGEX = "^usr-[A-Za-z0-9]+$";

    // Error messages
    public final static String INVALID_DETAILS_SUPPLIED = "Invalid details supplied";
    public final static String EXPIRED_TOKEN = "Token is expired";
    public final static String USER_NOT_FOUND = "User was not found ID: ";
    public final static String ACCOUNT_NOT_FOUND = "Account was not found ID: ";
    public final static String NOT_ALLOWED_TO_ACCESS = "Not allowed to access another user's data";
    public final static String NOT_A_VALID_TOKEN = "Not a valid token: ";
    public final static String INSUFFICIENT_BALANCE = "Insufficient funds to process transaction";
    public final static String TRANSACTION_NOT_FOUND = "Transaction was not found ID: ";

    // Others
    public final static String MESSAGE = "message";
    public final static String ERROR_DETAIL = "errorDetail";
    public final static String VALIDATION_ERROR = "ValidationError";
    public final static String ERROR = "error";
    public final static String ACCOUNT_PREFIX = "01";
    public final static String FORMAT_ACCOUNT = "%06d";
    public final static String ACCOUNT_INITIAL_BALANCE = "500.00";
    public final static String ROLE_USER = "ROLE_USER";
}
