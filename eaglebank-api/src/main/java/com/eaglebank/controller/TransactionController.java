package com.eaglebank.controller;

import com.eaglebank.model.BadRequestErrorResponse;
import com.eaglebank.model.CreateTransactionRequest;
import com.eaglebank.model.ErrorResponse;
import com.eaglebank.model.TransactionResponse;
import com.eaglebank.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static com.eaglebank.constants.EagleBankApiConstants.ACCOUNT_NUMBER_REGEX;
import static com.eaglebank.constants.EagleBankApiConstants.TRANSACTION_ID_REGEX;

@RestController
@Validated
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /v1/accounts/{accountNumber}/transactions
     * Create a transaction
     *
     * @param accountNumber            Account number of the bank account (required)
     * @param createTransactionRequest Create a new transaction (required)
     * @return Transaction has been created successfully (status code 201)
     * or Invalid details supplied (status code 400)
     * or Access token is missing or invalid (status code 401)
     * or The user is not allowed to delete the bank account details (status code 403)
     * or Bank account was not found (status code 404)
     * or Insufficient funds to process transaction (status code 422)
     * or An unexpected error occurred (status code 500)
     */
    @Operation(
            operationId = "createTransaction",
            description = "Create a transaction",
            tags = {"transaction"},
            responses = {
                    @ApiResponse(responseCode = "201", description = "Transaction has been created successfully", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))
                    }),
                    @ApiResponse(responseCode = "400", description = "Invalid details supplied", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = BadRequestErrorResponse.class))
                    }),
                    @ApiResponse(responseCode = "401", description = "Access token is missing or invalid", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                    }),
                    @ApiResponse(responseCode = "403", description = "The user is not allowed to delete the bank account details", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                    }),
                    @ApiResponse(responseCode = "404", description = "Bank account was not found", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                    }),
                    @ApiResponse(responseCode = "422", description = "Insufficient funds to process transaction", content = {
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
            value = "/v1/accounts/{accountNumber}/transactions",
            produces = {"application/json"},
            consumes = {"application/json"}
    )
    public ResponseEntity<TransactionResponse> createTransaction(
            @Pattern(regexp = ACCOUNT_NUMBER_REGEX) @Parameter(name = "accountNumber", description = "Account number of the bank account", required = true, in = ParameterIn.PATH) @PathVariable("accountNumber") String accountNumber,
            @Parameter(name = "CreateTransactionRequest", description = "Create a new transaction", required = true) @Valid @RequestBody CreateTransactionRequest createTransactionRequest) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    transactionService.createTransaction(accountNumber, createTransactionRequest));
        }
        catch (OptimisticLockException e) {
            // e.g., return HTTP 409 or retry
            log.error("The account was modified concurrently. Please retry.");
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Concurrent update detected");
    }

    /**
     * GET /v1/accounts/{accountNumber}/transactions/{transactionId}
     * Fetch transaction by ID.
     *
     * @param accountNumber Account number of the bank account (required)
     * @param transactionId ID of the transaction (required)
     * @return The transaction details (status code 200)
     * or The request didn&#39;t supply all the necessary data (status code 400)
     * or Access token is missing or invalid (status code 401)
     * or The user is not allowed to access the transaction (status code 403)
     * or Bank account was not found (status code 404)
     * or An unexpected error occurred (status code 500)
     */
    @Operation(
            operationId = "fetchAccountTransactionByID",
            description = "Fetch transaction by ID.",
            tags = {"transaction"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "The transaction details", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))
                    }),
                    @ApiResponse(responseCode = "400", description = "The request didn't supply all the necessary data", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = BadRequestErrorResponse.class))
                    }),
                    @ApiResponse(responseCode = "401", description = "Access token is missing or invalid", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                    }),
                    @ApiResponse(responseCode = "403", description = "The user is not allowed to access the transaction", content = {
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
            value = "/v1/accounts/{accountNumber}/transactions/{transactionId}",
            produces = {"application/json"}
    )
    public ResponseEntity<TransactionResponse> _fetchAccountTransactionByID(
            @Pattern(regexp = ACCOUNT_NUMBER_REGEX) @Parameter(name = "accountNumber", description = "Account number of the bank account", required = true, in = ParameterIn.PATH) @PathVariable("accountNumber") String accountNumber,
            @Pattern(regexp = TRANSACTION_ID_REGEX) @Parameter(name = "transactionId", description = "ID of the transaction", required = true, in = ParameterIn.PATH) @PathVariable("transactionId") String transactionId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(transactionService.fetchTransactionById(accountNumber, transactionId));
    }
}
