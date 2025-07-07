package com.eaglebank.service.impl;

import com.eaglebank.entity.BankAccountEntity;
import com.eaglebank.entity.TransactionEntity;
import com.eaglebank.entity.UserEntity;
import com.eaglebank.exception.EntityNotFountException;
import com.eaglebank.exception.InSufficientBalanceException;
import com.eaglebank.model.CreateTransactionRequest;
import com.eaglebank.model.TransactionResponse;
import com.eaglebank.model.enums.CurrencyType;
import com.eaglebank.model.enums.TransactionType;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.TransactionRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.eaglebank.constants.EagleBankApiConstants.ACCOUNT_NOT_FOUND;
import static com.eaglebank.constants.EagleBankApiConstants.INSUFFICIENT_BALANCE;
import static com.eaglebank.constants.EagleBankApiConstants.NOT_ALLOWED_TO_ACCESS;
import static com.eaglebank.constants.EagleBankApiConstants.TRANSACTION_NOT_FOUND;

/**
 * Service class to handle banking transactions such as deposits and withdrawals.
 *
 * It manages transaction creation, validates user access, updates bank account balances,
 * and stores transaction records.
 */
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;

    private static AtomicInteger counter = new AtomicInteger(1);

    /**
     * Creates a transaction (deposit or withdrawal) for a given account.
     * Validates the account and user access before performing the operation.
     *
     * @param accountNumber the bank account number
     * @param request       the transaction request containing type and amount
     * @return a response with transaction details
     * @throws EntityNotFountException if the account is not found
     * @throws AccessDeniedException         if the user tries to access another user's account
     * @throws InSufficientBalanceException if there are insufficient funds for withdrawal
     * @throws UnsupportedOperationException if the transaction type is not supported
     */
    @Override
    @Transactional
    public TransactionResponse createTransaction(final String accountNumber, final CreateTransactionRequest request) {
        verifyCurrencyType(request.getCurrency());
        setScale(request);
        final BankAccountEntity existingEntity = validateAndGetAccount(accountNumber);
        final BankAccountEntity updatedAccount = handleDepositWithdraw(request, existingEntity);
        return updateTransactionEntity(request, updatedAccount);
    }

    /**
     * Retrieves a transaction for a given account and transaction ID.
     * Ensures the account exists and the transaction is linked to it.
     *
     * @param accountNumber the account number to verify ownership
     * @param transId the ID of the transaction to retrieve
     * @return the transaction response
     * @throws EntityNotFountException if the account doesn't exist or the transaction isn't linked to it
     */
    @Override
    public TransactionResponse fetchTransactionById(final String accountNumber, final String transId) {
        validateAndGetAccount(accountNumber);
        final TransactionEntity transactionEntity = validateAndGetransactionEntity(accountNumber, transId);
        return constructTransactionResponse(transactionEntity);

    }

    /**
     * Validates that a transaction exists and is associated with the given account.
     *
     * @param accountNumber the account number expected to own the transaction
     * @param transId the transaction ID to look up
     * @return the matching transaction entity
     * @throws EntityNotFountException if the transaction doesn't exist or isn't linked to the account
     */

    private TransactionEntity validateAndGetransactionEntity(final String accountNumber, final String transId) {
        final Optional<TransactionEntity> transEntity = transactionRepository.findByTransactionId(transId);
        if (!transEntity.isPresent()) {
            throw new EntityNotFountException(TRANSACTION_NOT_FOUND + transId);
        }
        if (!(transEntity.get().getBankAccount().getAccountNumber().equals(accountNumber))) {
            throw new EntityNotFountException(String.format("Transaction id %s not associated with %s account number ", transId, accountNumber));
        }
        return transEntity.get();
    }

    /**
     * Validates that the given currency string matches a supported CurrencyType.
     *
     * @param currency the currency code to validate (e.g., "GBP", "USD")
     * @throws UnsupportedOperationException if the currency is not supported
     */
    private void verifyCurrencyType(String currency) {
        if (!Arrays.asList(CurrencyType.values()).stream().anyMatch(
                value -> value.getCurrency().equalsIgnoreCase(currency))) {
            throw new UnsupportedOperationException(
                    "Only supported currency types are: " + CurrencyType.getEnumValues());
        }
    }

    /**
     * Validates if the account exists and the currently logged-in user is authorized to access it.
     *
     * @param accountNumber the bank account number
     * @return the BankAccountEntity if validation passes
     * @throws EntityNotFountException if account is not found
     * @throws AccessDeniedException         if user is not authorized to access the account
     */
    private BankAccountEntity validateAndGetAccount(String accountNumber) {
        Optional<BankAccountEntity> user = bankAccountRepository.findByAccountNumber(accountNumber);
        if (!user.isPresent()) {
            throw new EntityNotFountException(ACCOUNT_NOT_FOUND + accountNumber);
        }
        shouldNotAccessOtherAccount(accountNumber);
        return user.get();
    }

    /**
     * Checks that the logged-in user has access only to their own account.
     *
     * @param accountId the account ID to verify
     * @throws AccessDeniedException if access is unauthorized
     */
    private void shouldNotAccessOtherAccount(final String accountId) {
        String loggedInUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        final Optional<UserEntity> user = userRepository.findByEmail(loggedInUsername);

        boolean isSameUser = user.stream()
                .map(a -> a.getAccounts())
                .flatMap(List::stream)
                .anyMatch(acnt -> acnt.getAccountNumber().equalsIgnoreCase(accountId));

        if (!isSameUser) {
            throw new AccessDeniedException(NOT_ALLOWED_TO_ACCESS);
        }
    }

    /**
     * Handles deposit or withdrawal operation based on the transaction type.
     *
     * @param request    the transaction request
     * @param bankEntity the bank account entity to update
     * @return updated bank account entity after transaction
     * @throws UnsupportedOperationException if transaction type is not supported
     */
    private BankAccountEntity handleDepositWithdraw(final CreateTransactionRequest request,
                                                    final BankAccountEntity bankEntity) {
        if (TransactionType.DEPOSIT.getTransactionType().equalsIgnoreCase(request.getType())) {
            return depositMoney(request, bankEntity);
        }

        if (TransactionType.WITHDRAW.getTransactionType().equalsIgnoreCase(request.getType())) {
            return withdrawMoney(request, bankEntity);
        }
        throw new UnsupportedOperationException(
                "Only supported transaction types are: " + TransactionType.getEnumValues());
    }

    /**
     * Deposits money into the bank account.
     *
     * @param request    the transaction request
     * @param bankEntity the bank account entity to update
     * @return updated bank account entity after deposit
     */
    private BankAccountEntity depositMoney(final CreateTransactionRequest request, final BankAccountEntity bankEntity) {
        final BigDecimal updatedAmount = bankEntity.getBalance().add(request.getAmount());
        bankEntity.setBalance(updatedAmount);
        return bankEntity;
    }

    /**
     * Withdraws money from the bank account after checking sufficient balance.
     *
     * @param request    the transaction request
     * @param bankEntity the bank account entity to update
     * @return updated bank account entity after withdrawal
     * @throws InSufficientBalanceException if balance is insufficient
     */
    private BankAccountEntity withdrawMoney(final CreateTransactionRequest request,
                                            final BankAccountEntity bankEntity) {
        if (hasAccountSufficientBalance(request, bankEntity)) {
            final BigDecimal updatedAmount = bankEntity.getBalance().subtract(request.getAmount());
            bankEntity.setBalance(updatedAmount);
            return bankEntity;
        }
        throw new InSufficientBalanceException(INSUFFICIENT_BALANCE);
    }

    /**
     * Checks if the bank account has sufficient balance for withdrawal.
     *
     * @param request    the transaction request
     * @param bankEntity the bank account entity
     * @return true if sufficient balance, false otherwise
     */
    private boolean hasAccountSufficientBalance(final CreateTransactionRequest request,
                                                final BankAccountEntity bankEntity) {
        return bankEntity.getBalance().compareTo(request.getAmount()) < 0 ? Boolean.FALSE : Boolean.TRUE;
    }

    /**
     * Persists the transaction and constructs the response.
     *
     * @param request           the transaction request
     * @param bankAccountEntity  the bank account entity involved
     * @return transaction response with details of saved transaction
     */
    private TransactionResponse updateTransactionEntity(final CreateTransactionRequest request,
                                                  final BankAccountEntity bankAccountEntity) {
        final TransactionEntity te = populateTransactionEntity(request, bankAccountEntity);
        TransactionEntity savedTrasaction = transactionRepository.save(te);
        return constructTransactionResponse(savedTrasaction);
    }

    /**
     * Populates the TransactionEntity from the request and bank account.
     *
     * @param request          the transaction request
     * @param bankAccountEntity the bank account entity
     * @return a TransactionEntity ready to be saved
     */
    private TransactionEntity populateTransactionEntity(final CreateTransactionRequest request,
                                                        final BankAccountEntity bankAccountEntity) {
        final String createdTimestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        final var te = new TransactionEntity();
        te.setTransactionId(generateTransactionId());
        te.setAmount(request.getAmount());
        te.setCurrency(request.getCurrency().toString());
        te.setType(request.getType());
        te.setCreatedTimestamp(createdTimestamp);
        te.setBankAccount(bankAccountEntity);
        return te;
    }

    /**
     * Constructs a TransactionResponse from the saved transaction entity.
     *
     * @param entity the saved transaction entity
     * @return transaction response DTO
     */
    private TransactionResponse constructTransactionResponse(final TransactionEntity entity) {
        return TransactionResponse.builder()
                .transactionId(entity.getTransactionId())
                .type(entity.getType())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .createdTimestamp(entity.getCreatedTimestamp())
                .build();

    }

    /**
     * set scale for 2 precisions
     * @param request
     */
    private void setScale(final CreateTransactionRequest request) {
        request.setAmount(request.getAmount().setScale(2, RoundingMode.UP));
    }

    /**
     * Generates a unique transaction ID prefixed with "tan-".
     *
     * @return generated unique transaction ID
     */
    private String generateTransactionId() {
        return String.format("tan-%02d", counter.getAndIncrement());
    }
}
