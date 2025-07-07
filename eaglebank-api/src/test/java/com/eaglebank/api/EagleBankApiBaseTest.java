package com.eaglebank.api;

import com.eaglebank.model.Address;
import com.eaglebank.model.CreateUserRequest;
import com.eaglebank.model.auth.AuthRequest;
import com.eaglebank.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base integration test class for Eagle Bank API tests.
 *
 * This class provides common setup, utilities, and helper methods for
 * other integration test classes that interact with the API endpoints.
 * It configures MockMvc for HTTP request simulation and provides shared
 * methods for creating users, authenticating users, and building address data.
 *
 * Shared test functionality includes:
 * - Creating a user with valid data
 * - Authenticating a user and retrieving a JWT token
 * - Generating a default address object
 *
 * Test classes such as UserControllerIntegrationTest and BankAccountControllerIntegrationTest
 * extend this base class to reuse these utilities.
 */

@SpringBootTest
@AutoConfigureMockMvc
public class EagleBankApiBaseTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    protected ResultActions createUser(final String userName, final String password) throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("firstname lastname");
        request.setEmail(userName);
        request.setPassword(password);
        request.setPhoneNumber("+447900231111");

        // Set address
        Address address = getAddress();

        request.setAddress(address);

        ResultActions result = mockMvc.perform(post("/v1/users")
                                                       .contentType(MediaType.APPLICATION_JSON)
                                                       .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        return result;
    }

    protected String authenticateUserAndGetJWTToken(final String userName, final String password) throws Exception {
        // Perform login and get JWT token
        AuthRequest loginRequest = AuthRequest.builder()
                .username(userName)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(responseBody, "$.token");
    }

    protected Address getAddress() {
        return Address.builder()
                .line1("Woodhatch Road")
                .town("Redhill")
                .postCode("RH9 7DX")
                .county("Surrey")
                .build();

    }
}
