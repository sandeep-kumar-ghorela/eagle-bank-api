package com.eaglebank.service;

import com.eaglebank.model.BankAccountResponse;
import com.eaglebank.model.CreateBankAccountRequest;

public interface BankAccountService {
    BankAccountResponse createAccount(final CreateBankAccountRequest createBankAccountRequest);
    BankAccountResponse fetchAccountByAccountNumber(String acountId);
}
