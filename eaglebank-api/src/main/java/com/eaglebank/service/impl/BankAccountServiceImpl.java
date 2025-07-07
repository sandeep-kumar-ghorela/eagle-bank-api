package com.eaglebank.service.impl;

import com.eaglebank.entity.BankAccountEntity;
import com.eaglebank.entity.UserEntity;
import com.eaglebank.exception.EntityNotFountException;
import com.eaglebank.exception.UserNotFoundException;
import com.eaglebank.model.BankAccountResponse;
import com.eaglebank.model.CreateBankAccountRequest;
import com.eaglebank.model.enums.AccountType;
import com.eaglebank.model.enums.CurrencyType;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.service.BankAccountService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.eaglebank.constants.EagleBankApiConstants.ACCOUNT_INITIAL_BALANCE;
import static com.eaglebank.constants.EagleBankApiConstants.ACCOUNT_NOT_FOUND;
import static com.eaglebank.constants.EagleBankApiConstants.ACCOUNT_PREFIX;
import static com.eaglebank.constants.EagleBankApiConstants.FORMAT_ACCOUNT;
import static com.eaglebank.constants.EagleBankApiConstants.NOT_ALLOWED_TO_ACCESS;
import static com.eaglebank.constants.EagleBankApiConstants.USER_NOT_FOUND;

/**
 * Service class responsible for bank account-related operations such as
 * creating accounts and fetching account details.
 */
@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository accountRepository;
    private final UserRepository userRepository;

    private final AtomicInteger AccountNumberCounter = new AtomicInteger(100000);
    private final AtomicInteger sortCodeCounter = new AtomicInteger(109528);

    /**
     * Creates a new bank account for a user.
     *
     * @param createBankAccountRequest contains userId and account request details
     * @return a response object containing the created bank account details
     */
    @Override
    @Transactional
    public BankAccountResponse createAccount(final CreateBankAccountRequest createBankAccountRequest) {
        verifyAccountType(createBankAccountRequest.getAccountType());
        final BankAccountEntity accountEntity = populateEntity(createBankAccountRequest);
        final BankAccountEntity savedEntity = accountRepository.save(accountEntity);
        return constructBankAccountResponse(savedEntity);
    }

    /**
     * Checks if the given account type string matches any supported {@link AccountType}.
     *
     * @param accountType the input string to validate (e.g., "personal", "saving")
     * @throws UnsupportedOperationException if the input does not match any AccountType
     */
    private void verifyAccountType(String accountType) {
        if (!Arrays.asList(AccountType.values()).stream().anyMatch(
                value -> value.getAccountType().equalsIgnoreCase(accountType))) {
            throw new UnsupportedOperationException(
                    "Only supported account types are: " + AccountType.getEnumValues());
        }

    }

    /**
     * Fetches a bank account by its account number.
     * Also checks if the currently authenticated user is allowed to access it.
     *
     * @param acountId the account number
     * @return account details if found and access is allowed
     * @throws EntityNotFountException if no such account exists
     * @throws AccessDeniedException if the user tries to access someone else's account
     */
    @Override
    public BankAccountResponse fetchAccountByAccountNumber(final String acountId) {
        final BankAccountEntity account = validateAndGetAccount(acountId);
        return  constructBankAccountResponse(account);
    }

    /**
     * Retrieves the BankAccountEntity associated with the given account ID,
     * This method performs two validations:
     *  - Checks whether the account exists in the repository.
     *  - Verifies that the current user has permission to access the account.
     *
     * @param accountId the unique identifier of the bank account to retrieve
     * @return the corresponding BankAccountEntity, if found and accessible
     */
    private BankAccountEntity validateAndGetAccount(final String accountId) {
        final Optional<BankAccountEntity> account = accountRepository.findByAccountNumber(accountId);
        if (!account.isPresent()) {
            throw new EntityNotFountException(ACCOUNT_NOT_FOUND + accountId);
        }
        else {
            shouldNotAccessOtherAccount(accountId);
        }
        return account.get();
    }

    /**
     * Validates that the currently authenticated user owns the given account.
     *
     * @param accountId the account number
     * @throws AccessDeniedException if the user does not own the account
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
     * Builds and populates a new BankAccountEntity from the request.
     *
     * @param req the account creation request
     * @return populated BankAccountEntity
     */
    private BankAccountEntity populateEntity(final CreateBankAccountRequest req) {
        final String timeStamp = getTimeStmap();
        final var entity = new BankAccountEntity();

        final String loggedInUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        final UserEntity user = userRepository.findByEmail(loggedInUsername)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND + loggedInUsername));
        entity.setAccountNumber(createAccountNumber());
        entity.setAccountType(req.getAccountType());
        entity.setBalance(getBalance());
        entity.setSortCode(getSortCode());
        entity.setName(req.getName());
        entity.setCreatedTimestamp(timeStamp);
        entity.setUpdatedTimestamp(timeStamp);
        entity.setCurrency(getCurrency());
        entity.setUser(user);
        return entity;
    }

    /**
     * Converts a BankAccountEntity into a response object for client use.
     *
     * @param savedEntity the persisted bank account entity
     * @return response object with account details
     */
    private BankAccountResponse constructBankAccountResponse(final BankAccountEntity savedEntity) {
        return BankAccountResponse.builder()
                .accountNumber(savedEntity.getAccountNumber())
                .accountType(savedEntity.getAccountType())
                .balance(savedEntity.getBalance())
                .sortCode(savedEntity.getSortCode())
                .name(savedEntity.getName())
                .createdTimestamp(savedEntity.getCreatedTimestamp())
                .updatedTimestamp(savedEntity.getUpdatedTimestamp())
                .currency(savedEntity.getCurrency())
                .build();
    }

    /**
     * Generates a new unique account number starting with "01".
     *
     * @return generated account number
     */
    private String createAccountNumber() {
        int next = AccountNumberCounter.getAndIncrement();
        return ACCOUNT_PREFIX+ String.format(FORMAT_ACCOUNT, next);
    }

    /**
     * Generates a sort code in the format "xx-xx-xx".
     *
     * @return formatted sort code
     */
    private String getSortCode() {
        int next = sortCodeCounter.getAndIncrement();
        var value = String.format(FORMAT_ACCOUNT, next);
        char c[] = value.toCharArray();
        var builder = new StringBuilder();
        for(int i = 0; i < c.length; i++ ) {
            builder.append(c[i]);
            if(i == 1 || i ==3) {
                builder.append("-");
            }
        }
        return builder.toString();
    }

    /**
     * Returns the current timestamp in ISO-8601 format.
     *
     * @return formatted timestamp
     */
    private String getTimeStmap() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    /**
     * Returns the default currency for the account.
     *
     * @return currency string
     */
    private String getCurrency() {
        return CurrencyType.GBP.toString();
    }

    /**
     * Returns the initial balance for new accounts.
     *
     * @return initial balance as BigDecimal
     */
    private BigDecimal getBalance() {
        return new BigDecimal(ACCOUNT_INITIAL_BALANCE);
    }

}
