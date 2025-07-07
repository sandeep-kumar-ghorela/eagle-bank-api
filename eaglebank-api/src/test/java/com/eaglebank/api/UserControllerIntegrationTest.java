package com.eaglebank.api;


import com.eaglebank.model.CreateUserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static com.eaglebank.api.EagleBankApiTestConstants.EMPTY_STR;
import static com.eaglebank.api.EagleBankApiTestConstants.NON_EXISTENT_USER;
import static com.eaglebank.api.EagleBankApiTestConstants.PASSWORD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the UserController.
 *
 * This class verifies the behavior of the user-related API endpoints, including user creation
 * and retrieval. It uses full application context and MockMvc to simulate HTTP requests
 * and validate responses.
 *
 * Test scenarios include:
 * - Creating a user with valid input
 * - Attempting to create a user with missing required fields
 * - Fetching user details by authenticated users
 * - Handling access to another user's data (403 Forbidden)
 * - Attempting to fetch a non-existent user (404 Not Found)
 */

class UserControllerIntegrationTest extends EagleBankApiBaseTest {

    /**
     * Scenario: Create a new user
     * Given a user wants to signup for Eagle Bank
     * When the user makes a `POST` request to the `/v1/users` endpoint with all the required data Then a new user is created
     */
    @Test
    void givenUserCreation_whenUserSignsUp_thenCreateUserSuccessfully() throws Exception {
        // Create user
        ResultActions result = createUser("testinguser@gmail.com", PASSWORD);
        result.andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.name").value("firstname lastname"));
    }

    /**
     * Scenario: Create a new user without supplying all required data
     * Given a user has successfully authenticated
     * When the user makes a `POST` request to the `/v1/users` endpoint with required data missing Then the system returns a Bad Request status code and error message
     */
    @Test
    void givenUserCreation_whenRequiredInfoMissing_thenReturnBadRequest_400() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setName(EMPTY_STR);
        request.setEmail(EMPTY_STR);
        request.setPassword(PASSWORD);
        request.setPhoneNumber("+447900231111");
        request.setAddress(null);

        mockMvc.perform(post("/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    /**
     * Scenario: User wants to fetch their user details
     * Given a user has successfully authenticated
     * When the user makes a `GET` request to the `/v1/users/{userId}` endpoint supplying their `userId` Then the system fetches the user details
     */
    @Test
    void givenFetchUser_whenfetchUserById_thenReturnUserDetails() throws Exception {
        // 1. Create user
        var userName = "tttt-yyy@gmail.com";
        final ResultActions resultActions = createUser(userName, PASSWORD);
        final String result = resultActions.andReturn().getResponse().getContentAsString();
        final String userId = objectMapper.readTree(result).get("userId").asText();

        // 2. Authenticate user
        final String jwtToken = authenticateUserAndGetJWTToken(userName, PASSWORD);

        // 2. Fetch user details
        mockMvc.perform(get("/v1/users/{userId}", userId)
                                .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.phoneNumber").value("+447900231111"));
    }

    /**
     * cenario: User wants to fetch the user details of another user
     * Given a user has successfully authenticated
     * When the user makes a `GET` request to the `/v1/users/{userId}` endpoint supplying another user's `userId` Then the system returns a Forbidden status code and error message
     */
    @Test
    void givenFetchUser_whenUserFetchOtherUserDetails_thenReturnForbidden_403() throws Exception {
        // 1. Create user 1
        var userName = "test_email_101@gmail.com";
        createUser(userName, PASSWORD);

        // 2. Create user 2
        var userName2 = "test_email_102@gmail.com";
        final ResultActions resultActions2 = createUser(userName2, PASSWORD);
        final String result2 = resultActions2.andReturn().getResponse().getContentAsString();
        final String userId2 = objectMapper.readTree(result2).get("userId").asText();

        // 2. Authenticate user 1
        final String jwtToken = authenticateUserAndGetJWTToken(userName, PASSWORD);

        // 2. Fetch user details
        mockMvc.perform(get("/v1/users/{userId}", userId2)
                                .header("Authorization", jwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenOtherUserDetails_whenUserFetchNonExistentUser_thenReturnForbidden_404() throws Exception {
        // 1. Create user 1
        var userName2 = "test_email_104@gmail.com";
        final ResultActions resultActions2 = createUser(userName2, PASSWORD);
        final String result2 = resultActions2.andReturn().getResponse().getContentAsString();
        final String userId2 = objectMapper.readTree(result2).get("userId").asText();

        // 2. Authenticate user 1
        final String jwtToken = authenticateUserAndGetJWTToken(userName2, PASSWORD);

        // 2. Fetch user details
        mockMvc.perform(get("/v1/users/{userId}", NON_EXISTENT_USER)
                                .header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }
}
