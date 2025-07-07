package com.eaglebank.api;

import com.eaglebank.model.CreateBankAccountRequest;
import com.eaglebank.model.CreateTransactionRequest;
import com.eaglebank.repository.BankAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.eaglebank.api.EagleBankApiTestConstants.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for validating transaction-account association logic in {@link com.eaglebank.entity.BankAccountEntity}.
 *
 * This test class focuses on ensuring that transaction IDs are correctly validated against
 * their associated bank accounts. Specifically, it verifies the behavior of the method
 * that checks whether a given transaction ID belongs to the list of transactions within same account {@link com.eaglebank.entity.BankAccountEntity}.
 *
 * Scenarios covered include:
 *     Valid transaction ID associated with the account
 *     Invalid or non-existent transaction ID
 *     Edge cases such as null or empty transaction ID
 *     Accounts with no transactions<
 */

class TransactionIntegrationTest extends EagleBankApiBaseTest {


    @Autowired
    private BankAccountRepository accountRepository;

    /**
     * Scenario: User wants to deposit money into their bank account
     * Given a user has successfully authenticated
     * When the user makes a `POST` request to the `/v1/accounts/{accountId}/transactions` endpoint with all the required data And the transaction type is `deposit`
     * And the account is associated with their `userId`
     * Then the deposit is registered against the account
     * And the account balance is updated
     */
    @Test
    void givenValidDeposit_whenDepositedToTheirAccount_thenBalanceUpdated() throws Exception {
        // 1. Create user
        var userName = "user_email1@hi.com";
        createUser(userName, PASSWORD);

        // 2. authenticate user
        final String jwtToken = authenticateUserAndGetJWTToken(userName, PASSWORD);

        // 3. Create a bank account and associate with current user
        String acctResp = createAccount(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL, jwtToken);
        final String accountNumber = objectMapper.readTree(acctResp).get(ACCOUNTER_NUMBER).asText();
        final String accountCurrentBalance = objectMapper.readTree(acctResp).get(BALANCE).asText();

        // 4. Expected balance after transaction
        final BigDecimal expectedBalance =
                new BigDecimal(accountCurrentBalance).add(new BigDecimal(DEPOSIT_AMOUNT)).setScale(2, RoundingMode.UP);

        // 5.  Deposit money = 98.99 and create transaction for the same:
        final CreateTransactionRequest transactionRequest =
                createTransactionRequest(TRANSACTION_TYPE_DEPOSIT, DEPOSIT_AMOUNT);

        // 6. Apply transaction on account
        applyTransaction(accountNumber, jwtToken, transactionRequest);

        // 7. Fetch account balance after deposit transaciton successfully executed
        final String res = fetchAcountBalanceInfo(accountNumber, jwtToken);
        final BigDecimal balanceAfterUpdate = new BigDecimal(objectMapper.readTree(res).get(BALANCE).asText());

        // 8. Expected and Update balance should be same.
        assertTrue(expectedBalance.compareTo(balanceAfterUpdate) == 0,
                   "Expected: " + expectedBalance + ", but was: " + balanceAfterUpdate);
    }

    /**
     * Scenario: User wants to withdraw money from their bank account
     * Given a user has successfully authenticated
     * When the user makes a `POST` request to the `/v1/accounts/{accountId}/transactions` endpoint with all the required data And the transaction type is `withdrawal`
     * And the account has sufficient funds
     * And the account is associated with their `userId`
     * Then the withdrawal is registered against the account
     * And the account balance is updated
     */
    @Test
    void givenValidWithdrawal_whenWithdrawingFromAccount_thenBalanceIsUpdated() throws Exception {
        // 1. Create user
        var userName = "user_email2@hi.com";
        createUser(userName, PASSWORD);

        // 2. authenticate user
        final String jwtToken = authenticateUserAndGetJWTToken(userName, PASSWORD);

        // 3. Create a bank account and associate with current user
        String acctResp = createAccount(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL, jwtToken);
        final String accountNumber = objectMapper.readTree(acctResp).get(ACCOUNTER_NUMBER).asText();
        final String accountCurrentBalance = objectMapper.readTree(acctResp).get(BALANCE).asText();

        // 4. Expected balance after transaction
        BigDecimal expectedBalance =
                new BigDecimal(accountCurrentBalance).subtract(new BigDecimal(WITHDRAWL_AMOUNT)).setScale(2,
                                                                                                          RoundingMode.UP);

        // 5.  Withdraw money = 97.99 and create transaction for the same:
        final CreateTransactionRequest transactionRequest =
                createTransactionRequest(TRANSACTION_TYPE_WITHDRAW, WITHDRAWL_AMOUNT);

        // 6. Apply transaction on account
        applyTransaction(accountNumber, jwtToken, transactionRequest);


        // 7. Fetch account balance after deposit transaciton successfully executed
        final String accountInfo = fetchAcountBalanceInfo(accountNumber, jwtToken);
        final BigDecimal balanceAfterUpdate =
                new BigDecimal(objectMapper.readTree(accountInfo).get(BALANCE).asText());

        // 8. Expected and Update balance should be same.
        assertTrue(expectedBalance.compareTo(balanceAfterUpdate) == 0,
                   "Expected: " + expectedBalance + ", after update: " + balanceAfterUpdate);

    }

    /**
     * Scenario: User wants to withdraw money from their bank account but they have insufficient funds
     * Given a user has successfully authenticated
     * When the user makes a `POST` request to the `/v1/accounts/{accountId}/transactions` endpoint with all the required data And the transaction type is `withdrawal`
     * And the account has insufficient funds
     * And the account is associated with their `userId`
     * Then the system returns a Unprocessable Entity status code and error message
     */
    @Test
    void givenInValidWithdrawal_whenInSufficientBalance_thenUnprocessable_422() throws Exception {
        // 1. Create user
        var userName = "user_email03@hi.com";
        createUser(userName, PASSWORD);

        // 2. authenticate user
        final String jwtToken = authenticateUserAndGetJWTToken(userName, PASSWORD);

        // 3. Create a bank account and associate with current user
        String acctResp = createAccount(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL, jwtToken);
        final String accountNumber = objectMapper.readTree(acctResp).get(ACCOUNTER_NUMBER).asText();

        // 4.  Withdraw money = 1000.00 which is more than available balance
        final CreateTransactionRequest transactionRequest =
                createTransactionRequest(TRANSACTION_TYPE_WITHDRAW, "10000.00");

        // 5. Apply transaction on account
        mockMvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                                .header("Authorization", jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isUnprocessableEntity());

    }

    /**
     * Scenario: User wants to deposit or withdraw money into another user's bank account
     * Given a user has successfully authenticated
     * When the user makes a `POST` request to the `/v1/accounts/{accountId}/transactions` endpoint with all the required data And the account is not associated with their `userId`
     * Then the system returns a Forbidden status code and error message
     */
    @Test
    void givenWithdrawal_whenWithdrawingFromOtherUserAccount_thenReturnForbidden_403() throws Exception {
        // 1. Create user 1
        var userName = "user_email04@hi.com";
        createUser(userName, PASSWORD);

        // 2. Create user 2
        var userName2 = "user_email05@hi.com";
        createUser(userName2, PASSWORD);

        // 3. authenticate user 1
        final String jwtToken = authenticateUserAndGetJWTToken(userName, PASSWORD);

        // 4. authenticate user 2
        final String jwtToken2 = authenticateUserAndGetJWTToken(userName2, PASSWORD);


        // 5. Create a bank account for user 1 and associate with user
        String acctResp = createAccount(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL, jwtToken);
        final String accountNumber = objectMapper.readTree(acctResp).get(ACCOUNTER_NUMBER).asText();
        final String accountCurrentBalance = objectMapper.readTree(acctResp).get(BALANCE).asText();

        // 6. Create a bank account for user 2 and associate with user
        String acctResp2 = createAccount(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL, jwtToken2);
        final String accountNumber2 = objectMapper.readTree(acctResp2).get(ACCOUNTER_NUMBER).asText();
        final String accountCurrentBalance2 = objectMapper.readTree(acctResp2).get(BALANCE).asText();


        // 7.  Withdraw money = 98.99 and create transaction for the same:
        final CreateTransactionRequest transactionRequest =
                createTransactionRequest(TRANSACTION_TYPE_WITHDRAW, WITHDRAWL_AMOUNT);

        // 8. User 1 wants to deposit or withdraw money into user 2's bank account
        mockMvc.perform(post("/v1/accounts/" + accountNumber2 + "/transactions")
                                .header("Authorization", jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isForbidden());


    }

    /**
     * Scenario: User wants to deposit or withdraw money into a non-existent bank account
     * Given a user has successfully authenticated
     * When the user makes a `POST` request to the `/v1/accounts/{accountId}/transactions` endpoint with all the required data And the `accountId` doesn't exist
     * Then the system returns a Not Found status code and error message
     */
    @Test
    void givenWithdrawal_whenWithdrawFromNonExistentAccount_thenReturn_404() throws Exception {
        // 1. Create user
        var userName = "user_email006@hi.com";
        createUser(userName, PASSWORD);

        // 2. authenticate user
        final String jwtToken = authenticateUserAndGetJWTToken(userName, PASSWORD);

        // 3. Create a bank account and associate with current user
        String acctResp = createAccount(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL, jwtToken);
        final String accountNumber = objectMapper.readTree(acctResp).get(ACCOUNTER_NUMBER).asText();

        // 4.  Withdraw money = 98.99 and create transaction for the same:
        final CreateTransactionRequest transactionRequest =
                createTransactionRequest(TRANSACTION_TYPE_WITHDRAW, "10000.00");

        // 5. Apply transaction on account
        mockMvc.perform(post("/v1/accounts/" + NON_EXISTENT_ACCOUNT + "/transactions")
                                .header("Authorization", jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isNotFound());

    }

    /**
     * Scenario: User wants to deposit or withdraw money without supplying all required data
     * Given a user has successfully authenticated
     * When the user makes a `POST` request to the `/v1/accounts/{accountId}/transactions` endpoint with required data missing Then the system returns a Bad Request status code and error message
     */
    @Test
    void givenDeposit_whenMissingRequiredData_thenReturn_400() throws Exception {
        // 1. Create user
        var userName = "user_email06@hi.com";
        createUser(userName, PASSWORD);

        // 2. authenticate user
        final String jwtToken = authenticateUserAndGetJWTToken(userName, PASSWORD);

        // 3. Create a bank account and associate with current user
        String acctResp = createAccount(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL, jwtToken);
        final String accountNumber = objectMapper.readTree(acctResp).get(ACCOUNTER_NUMBER).asText();

        // 4.  deposit money = 98.99 and create transaction for the same:
        final CreateTransactionRequest transactionRequest = createTransactionRequest(null, DEPOSIT_AMOUNT);

        // 5. Apply transaction on account
        mockMvc.perform(post("/v1/accounts/" + NON_EXISTENT_ACCOUNT + "/transactions")
                                .header("Authorization", jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isBadRequest());

    }

    /**
     * Scenario: User wants to fetch a transaction on their bank account
     * Given a user has successfully authenticated
     * When the user makes a `GET` request to the `/v1/accounts/{accountId}/transactions/{transactionId}` endpoint And the account is associated with their `userId`
     * And the `transactionId` is associated with the `accountId` specified
     * Then the transaction details are returned
     */
    @Test
    void givenProcessTransaction_whenUserFetchesOwnTransaction_thenReturnTransaction() throws Exception {
        // 1. Create user
        var userName = "user_email10@test.com";
        createUser(userName, PASSWORD);

        // 2. authenticate user
        final String jwtToken = authenticateUserAndGetJWTToken(userName, PASSWORD);

        // 3. Create a bank account and associate with current user
        String acctResp = createAccount(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL, jwtToken);
        final String accountNumber = objectMapper.readTree(acctResp).get(ACCOUNTER_NUMBER).asText();

        // 4.  Deposit money = 98.99 and create transaction for the same:
        final CreateTransactionRequest transactionRequest =
                createTransactionRequest(TRANSACTION_TYPE_DEPOSIT, DEPOSIT_AMOUNT);

        // 5. Apply transaction on account
        final ResultActions resultActions = applyTransaction(accountNumber, jwtToken, transactionRequest);
        final String trasactionId = objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString())
                .get(TRANSACTION_ID).asText();

        // 6. Fetch the transaction
        mockMvc.perform(get("/v1/accounts/" + accountNumber + "/transactions/" + trasactionId)
                                .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(trasactionId));
    }

    /**
     * Scenario: User wants to fetch a transaction on another user's bank account
     * Given a user has successfully authenticated
     * When the user makes a `GET` request to the `/v1/accounts/{accountId}/transactions/{transactionId}` endpoint And the account is not associated with their `userId`
     * Then the system returns a Forbidden status code and error message
     */
    @Test
    void givenProcessTransaction_whenUserFetchesOtherUserTransaction_thenReturnForbidden_403() throws Exception {
        // 1. Create user
        var userName = "user_email11@test.com";
        createUser(userName, PASSWORD);

        // 2. Create user
        var userName2 = "user_emailtest11@test.com";
        createUser(userName2, PASSWORD);

        // 3. authenticate user
        final String jwtToken = authenticateUserAndGetJWTToken(userName, PASSWORD);

        // 4. authenticate user
        final String jwtToken2 = authenticateUserAndGetJWTToken(userName2, PASSWORD);

        // 5. Create a bank account and associate with current user 1
        String acctResp = createAccount(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL, jwtToken);
        final String accountNumber = objectMapper.readTree(acctResp).get(ACCOUNTER_NUMBER).asText();

        // 6. Create a bank account and associate with current user 2
        String acctResp2 = createAccount(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL, jwtToken2);
        final String accountNumber2 = objectMapper.readTree(acctResp2).get(ACCOUNTER_NUMBER).asText();

        // 7.  Deposit money = 98.99 and create transaction for the same:
        final CreateTransactionRequest transactionRequest =
                createTransactionRequest(TRANSACTION_TYPE_DEPOSIT, DEPOSIT_AMOUNT);

        // 8. Apply transaction on user 1 account
        final ResultActions resultActions = applyTransaction(accountNumber, jwtToken, transactionRequest);

        // 9. Apply transaction on user 2 account
        final ResultActions resultActions2 = applyTransaction(accountNumber2, jwtToken2, transactionRequest);
        final String trasactionId2 =
                objectMapper.readTree(resultActions2.andReturn().getResponse().getContentAsString())
                        .get(TRANSACTION_ID).asText();

        // 10. When user 1 fetches the transaction of other user
        mockMvc.perform(get("/v1/accounts/" + accountNumber2 + "/transactions/" + trasactionId2)
                                .header("Authorization", jwtToken))
                .andExpect(status().isForbidden());
    }

    /**
     * Scenario: User wants to fetch a transaction on a non-existent bank account
     * Given a user has successfully authenticated
     * When the user makes a `GET` request to the `/v1/accounts/{accountId}/transactions/{transactionId}` endpoint And the `accountId` doesn't exist
     * Then the system returns a Not Found status code and error message
     *
     * @throws Exception
     */
    @Test
    void givenProcessTransaction_whenUserFetchesTransactionOnNonExistentAccount_thenReturnNotFound_404() throws Exception {
        // 1. Create user
        var userName = "user_email12@test.com";
        createUser(userName, PASSWORD);

        // 2. authenticate user
        final String jwtToken = authenticateUserAndGetJWTToken(userName, PASSWORD);

        // 3. Create a bank account and associate with current user
        String acctResp = createAccount(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL, jwtToken);
        final String accountNumber = objectMapper.readTree(acctResp).get(ACCOUNTER_NUMBER).asText();

        // 4.  Deposit money = 98.99 and create transaction for the same:
        final CreateTransactionRequest transactionRequest =
                createTransactionRequest(TRANSACTION_TYPE_DEPOSIT, DEPOSIT_AMOUNT);

        // 5. Apply transaction on account
        final ResultActions resultActions = applyTransaction(accountNumber, jwtToken, transactionRequest);
        final String trasactionId = objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString())
                .get(TRANSACTION_ID).asText();

        // 6. Fetch the transaction
        mockMvc.perform(get("/v1/accounts/" + NON_EXISTENT_ACCOUNT + "/transactions/" + trasactionId)
                                .header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }

    /**
     * Scenario: User wants to fetch a transactions on a non-existent transaction ID
     * Given a user has successfully authenticated
     * When the user makes a `GET` request to the `/v1/accounts/{accountId}/transactions/{transactionId}` endpoint And the account is associated with their `userId`
     * And the `transactionId` does not exist
     * Then the system returns a Not Found status code and error message
     */
    @Test
    void givenProcessTransaction_whenUserFetchesNonExistentTransaction_thenReturnNotFound_404() throws Exception {
        // 1. Create user
        var userName = "user_email13@test.com";
        createUser(userName, PASSWORD);

        // 2. authenticate user
        final String jwtToken = authenticateUserAndGetJWTToken(userName, PASSWORD);

        // 3. Create a bank account and associate with current user
        String acctResp = createAccount(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL, jwtToken);
        final String accountNumber = objectMapper.readTree(acctResp).get(ACCOUNTER_NUMBER).asText();

        // 4.  Deposit money = 98.99 and create transaction for the same:
        final CreateTransactionRequest transactionRequest =
                createTransactionRequest(TRANSACTION_TYPE_DEPOSIT, DEPOSIT_AMOUNT);

        // 5. Apply transaction on account
        applyTransaction(accountNumber, jwtToken, transactionRequest);

        // 6. Fetch the transaction
        mockMvc.perform(get("/v1/accounts/" + accountNumber + "/transactions/" + NON_EXISTENT_TRANSACTION_ID)
                                .header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }

    /**
     * Scenario: User wants to fetch a transaction against the wrong bank account(his own wrong bank account)
     * Given a user has successfully authenticated
     * When the user makes a `GET` request to the `/v1/accounts/{accountId}/transactions/{transactionId}` endpoint And the account is associated with their `userId`
     * And the `transactionId` is not associated with the `accountId` specified
     * Then the system returns a Not Found status code and error message
     */
    @Test
    void givenProcessTransaction_whenUserFetchesTransactionOnWrongAccount_thenReturnNotFound_404() throws Exception {
        // 1. Create user
        var userName = "user_email14@test.com";
        createUser(userName, PASSWORD);

        // 2. authenticate user
        final String jwtToken = authenticateUserAndGetJWTToken(userName, PASSWORD);

        // 3. Create a bank account 1 and associate with current user 2
        String acctResp = createAccount(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL, jwtToken);
        final String accountNumber = objectMapper.readTree(acctResp).get(ACCOUNTER_NUMBER).asText();

        // 4. Create a bank account 2 and associate with current user 2
        String acctResp2 = createAccount(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL, jwtToken);
        final String accountNumber2 = objectMapper.readTree(acctResp2).get(ACCOUNTER_NUMBER).asText();

        //5.  Deposit money = 98.99 and create transaction for the same:
        final CreateTransactionRequest transactionRequest =
                createTransactionRequest(TRANSACTION_TYPE_DEPOSIT, DEPOSIT_AMOUNT);

        // 6. Apply transaction on account 1
        final ResultActions resultActions = applyTransaction(accountNumber, jwtToken, transactionRequest);
        final String trasactionId = objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString())
                .get(TRANSACTION_ID).asText();

        // 7. Apply transaction on account 2
        final ResultActions resultActions2 = applyTransaction(accountNumber2, jwtToken, transactionRequest);
        final String trasactionId2 =
                objectMapper.readTree(resultActions2.andReturn().getResponse().getContentAsString())
                        .get(TRANSACTION_ID).asText();


        // 6. Fetch the transaction - Whe user 1 fetches transaction 1 (which is associated with account1) from account 2
        mockMvc.perform(get("/v1/accounts/" + accountNumber2 + "/transactions/" + trasactionId)
                                .header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }


    private String createAccount(final String name, final String accountType, final String jwtToken) throws Exception {
        CreateBankAccountRequest acctReq = CreateBankAccountRequest.builder()
                .name(name)
                .accountType(accountType)
                .build();
        String acctResp = mockMvc.perform(post("/v1/accounts")
                                                  .contentType(MediaType.APPLICATION_JSON)
                                                  .header("Authorization", jwtToken)
                                                  .content(objectMapper.writeValueAsString(acctReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return acctResp;

    }

    private String fetchAcountBalanceInfo(final String accountNumber, final String jwtToken) throws Exception {
        return mockMvc.perform(get("/v1/accounts/" + accountNumber)
                                       .header("Authorization", jwtToken)
                                       .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private ResultActions applyTransaction(final String accountNumber, final String jwtToken,
                                           final CreateTransactionRequest txReq) throws Exception {
        return mockMvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                                       .header("Authorization", jwtToken)
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .content(objectMapper.writeValueAsString(txReq)))
                .andExpect(status().isCreated());
    }

    private CreateTransactionRequest createTransactionRequest(final String tansactionType, final String amount) {
        return CreateTransactionRequest.builder()
                .type(tansactionType)
                .amount(new BigDecimal(amount))
                .currency(GBP)
                .build();
    }
}
