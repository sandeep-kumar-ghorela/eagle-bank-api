package com.eaglebank.controller;

import com.eaglebank.model.BadRequestErrorResponse;
import com.eaglebank.model.BankAccountResponse;
import com.eaglebank.model.CreateBankAccountRequest;
import com.eaglebank.model.ErrorResponse;
import com.eaglebank.service.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static com.eaglebank.constants.EagleBankApiConstants.ACCOUNT_NUMBER_REGEX;

@RestController
@Validated
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    /**
     * POST /v1/accounts
     * Create a new bank account
     *
     * @param createBankAccountRequest Create a new bank account for the user (required)
     * @return Bank Account has been created successfully (status code 201)
     *         or Invalid details supplied (status code 400)
     *         or Access token is missing or invalid (status code 401)
     *         or The user is not allowed to access the transaction (status code 403)
     *         or An unexpected error occurred (status code 500)
     */
    @Operation(
            operationId = "createAccount",
            description = "Create a new bank account",
            tags = { "account" },
            responses = {
                    @ApiResponse(responseCode = "201", description = "Bank Account has been created successfully", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = BankAccountResponse.class))
                    }),
                    @ApiResponse(responseCode = "400", description = "Invalid details supplied", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = BadRequestErrorResponse.class))
                    }),
                    @ApiResponse(responseCode = "401", description = "Access token is missing or invalid", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                    }),
                    @ApiResponse(responseCode = "403", description = "The user is not allowed to access the transaction", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                    }),
                    @ApiResponse(responseCode = "500", description = "An unexpected error occurred", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                    })
            },
            security = {
                    @SecurityRequirement(name = "bearerAuth")
            }
    )
    @RequestMapping(
            method = RequestMethod.POST,
            value = "/v1/accounts",
            produces = { "application/json" },
            consumes = { "application/json" }
    )
    public ResponseEntity<BankAccountResponse> _createAccount(
            @Parameter(name = "CreateBankAccountRequest", description = "Create a new bank account for the user", required = true) @Valid @RequestBody CreateBankAccountRequest createBankAccountRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bankAccountService.createAccount(createBankAccountRequest));
    }

    /**
     * GET /v1/accounts/{accountNumber}
     * Fetch account by account number.
     *
     * @param accountNumber Account number of the bank account (required)
     * @return The bank account details (status code 200)
     *         or The request didn&#39;t supply all the necessary data (status code 400)
     *         or The user was not authenticated (status code 401)
     *         or The user is not allowed to access the bank account details (status code 403)
     *         or Bank account was not found (status code 404)
     *         or An unexpected error occurred (status code 500)
     */
    @Operation(
            operationId = "fetchAccountByAccountNumber",
            description = "Fetch account by account number.",
            tags = { "account" },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The bank account details", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = BankAccountResponse.class))
                    }),
                    @ApiResponse(responseCode = "400", description = "The request didn't supply all the necessary data", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = BadRequestErrorResponse.class))
                    }),
                    @ApiResponse(responseCode = "401", description = "The user was not authenticated", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                    }),
                    @ApiResponse(responseCode = "403", description = "The user is not allowed to access the bank account details", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                    }),
                    @ApiResponse(responseCode = "404", description = "Bank account was not found", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                    }),
                    @ApiResponse(responseCode = "500", description = "An unexpected error occurred", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                    })
            },
            security = {
                    @SecurityRequirement(name = "bearerAuth")
            }
    )
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/v1/accounts/{accountNumber}",
            produces = { "application/json" }
    )
    public ResponseEntity<BankAccountResponse> fetchAccountByAccountNumber(
            @Pattern(regexp = ACCOUNT_NUMBER_REGEX) @Parameter(name = "accountNumber", description = "Account number of the bank account", required = true, in = ParameterIn.PATH) @PathVariable("accountNumber") String accountNumber) {
        return ResponseEntity.status(HttpStatus.OK).body(bankAccountService.fetchAccountByAccountNumber(accountNumber));
    }
}
