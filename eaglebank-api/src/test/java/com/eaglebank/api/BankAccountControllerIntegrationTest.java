package com.eaglebank.api;

import com.eaglebank.model.CreateBankAccountRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.eaglebank.api.EagleBankApiTestConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the BankAccountController.
 *
 * This test class verifies the full lifecycle of bank account operations through the REST API,
 * including account creation and retrieval. It uses MockMvc to simulate HTTP requests
 * and assert expected outcomes for different scenarios.
 *
 * Test scenarios include:
 * - Creating a bank account with valid input
 * - Handling missing required fields when creating an account
 * - Fetching the authenticated user's own bank account
 * - Preventing access to another user's bank account (403 Forbidden)
 * - Attempting to fetch a non-existent bank account (404 Not Found)
 *
 */

class BankAccountControllerIntegrationTest extends EagleBankApiBaseTest {

    /**
     * Scenario: User wants to create a new bank account
     * Given a user has successfully authenticated
     * When the user makes a `POST` request to the `/v1/accounts` endpoint with all the required data Then a new bank account is created, and the account details are returned
     */
    @Test
    void givenValidInput_whenCreateAccount_thenAccountCreatedAndReturnAccountDetails() throws Exception {
        // 1. Create user
        String username1 = "test_0.lastname@example.com";
        createUser(username1, PASSWORD);

        // 2. Authenticate user
        String jwtToken1 = authenticateUserAndGetJWTToken(username1, PASSWORD);

        // 3. Create bank account for user
        final CreateBankAccountRequest request = createBankAccountRequest(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_SAVING);

        mockMvc.perform(post("/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", jwtToken1)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").exists())
                .andExpect(jsonPath("$.accountType").value("saving"))
                .andExpect(jsonPath("$.currency").value("GBP"));
    }

    /**
     * Scenario: User wants to create a new bank account without supplying all required data
     * Given a user has successfully authenticated
     * When the user makes a `POST` request to the `/v1/accounts` endpoint with required data missing Then the system returns a Bad Request status code and error message
     */
    @Test
    void givenRequiredInfoMissing_whenCreateAccount_thenReturnBadRequest_400() throws Exception {
        // 1. Creat user
        String username1 = "first00.last@example.com";
        createUser(username1, PASSWORD);

        // 2. Authenticate user
        String jwtToken1 = authenticateUserAndGetJWTToken(username1, PASSWORD);

        // 3. Create bank account for user
        final CreateBankAccountRequest request = createBankAccountRequest(EMPTY_STR, ACCOUNT_TYPE_SAVING);
        mockMvc.perform(post("/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", jwtToken1)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Scenario: User wants to fetch their bank account details
     * Given a user has successfully authenticated
     * When the user makes a `GET` request to the `/v1/accounts/{accountId}` endpoint And the account is associated with their `userId`
     * Then the system fetches the bank account detail
     */
    @Test
    void givenCreateAccount_whenUserFetchesHisOwnAccount_thenReturnAcountInfo() throws Exception {
        // 1. Create user
        String username1 = "username.lastname@example.com";
        createUser(username1, PASSWORD);

        // 2. Authenticate user
        String jwtToken1 = authenticateUserAndGetJWTToken(username1, PASSWORD);

        // 3. Create bank account for user
        final CreateBankAccountRequest request = createBankAccountRequest(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_SAVING);

        mockMvc.perform(post("/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", jwtToken1)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").exists())
                .andExpect(jsonPath("$.accountType").value("saving"))
                .andExpect(jsonPath("$.currency").value("GBP"));
    }

    /**
     * Scenario: User wants to fetch another user's bank account details
     * Given a user has successfully authenticated
     * When the user makes a `GET` request to the `/v1/accounts/{accountId}` endpoint And the account is not associated with their `userId`
     * Then the system returns a Forbidden status code and error message
     */
    @Test
    void givenFetchAccount_whenUserFetchesOtherUserAccount_thenReturnForbidden_403() throws Exception {
        // 1. Create user1
        String username1 = "id1.ghorela@example.com";
        createUser(username1, PASSWORD);
        String jwtToken1 = authenticateUserAndGetJWTToken(username1, PASSWORD);


        // 2. Create user2
        String username2 = "id2.ghorela@example.com";
        createUser(username2, PASSWORD);
        String jwtToken2 = authenticateUserAndGetJWTToken(username2, PASSWORD);

        // 3. Create bank account for user1
        final CreateBankAccountRequest request = createBankAccountRequest(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_SAVING);
        mockMvc.perform(post("/v1/accounts")
                                                  .contentType(MediaType.APPLICATION_JSON)
                                                  .header("Authorization", jwtToken1)
                                                  .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        // 4. Create bank account for user 2
        final CreateBankAccountRequest request2 = createBankAccountRequest(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_SAVING);
        String acctResp2 = mockMvc.perform(post("/v1/accounts")
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .header("Authorization", jwtToken2)
                                                   .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String accountNumber2 = objectMapper.readTree(acctResp2).get("accountNumber").asText();

        // 5. When user1 try to fetch user 2 bank account
        mockMvc.perform(get("/v1/accounts/" + accountNumber2)
                                .header("Authorization", jwtToken1)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    /**
     * Scenario: User wants to fetch a non-existent bank account
     * Given a user has successfully authenticated
     * When the user makes a `GET` request to the `/v1/accounts/{accountId}` endpoint And the `accountId` doesn't exist
     * Then the system returns a Not Found status code and error message
     */
    @Test
    void givenFetchAccount_whenUserFetchesNonExistentAccount_thenReturnNotFound_404() throws Exception {
        // 1. Create user
        String username1 = "userid01.ghorela@example.com";
        createUser(username1, PASSWORD);

        // 2. Authenticate user
        String jwtToken1 = authenticateUserAndGetJWTToken(username1, PASSWORD);

        // 3. Create bank account for user1
        final CreateBankAccountRequest request = createBankAccountRequest(SAVING_ACCOUNT_NAME, ACCOUNT_TYPE_SAVING);
        String acctResp = mockMvc.perform(post("/v1/accounts")
                                                  .contentType(MediaType.APPLICATION_JSON)
                                                  .header("Authorization", jwtToken1)
                                                  .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        // 5. When user to fetch non-existent account

        mockMvc.perform(get("/v1/accounts/" + NON_EXISTENT_ACCOUNT)
                                .header("Authorization", jwtToken1)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private CreateBankAccountRequest createBankAccountRequest(final String accountName, final String accountType) {
        return CreateBankAccountRequest.builder()
                .name(accountName)
                .accountType(accountType)
                .build();
    }
}