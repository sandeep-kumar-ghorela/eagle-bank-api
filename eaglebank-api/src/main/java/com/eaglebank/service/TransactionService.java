package com.eaglebank.service;

import com.eaglebank.model.CreateTransactionRequest;
import com.eaglebank.model.TransactionResponse;

public interface TransactionService {
    TransactionResponse createTransaction(final String accountNumber, final CreateTransactionRequest request);
    TransactionResponse fetchTransactionById(final String accountNumber, final String transId);
}
