package com.eaglebank.controller;

import com.eaglebank.model.BadRequestErrorResponse;
import com.eaglebank.model.CreateUserRequest;
import com.eaglebank.model.ErrorResponse;
import com.eaglebank.model.UserResponse;
import com.eaglebank.service.UserService;
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

import static com.eaglebank.constants.EagleBankApiConstants.USER_ID_REGEX;

@RestController
@RequiredArgsConstructor
@Validated
public class UserController {

    final private UserService userService;

    /**
     * POST /v1/users
     * Create a new user
     *
     * @param createUserRequest Create a new user (required)
     * @return User has been created successfully (status code 201)
     * or Invalid details supplied (status code 400)
     * or An unexpected error occurred (status code 500)
     */
    @Operation(
            operationId = "createUser",
            description = "Create a new user",
            tags = {"user"},
            responses = {
                    @ApiResponse(responseCode = "201", description = "User has been created successfully", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))
                    }),
                    @ApiResponse(responseCode = "400", description = "Invalid details supplied", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = BadRequestErrorResponse.class))
                    }),
                    @ApiResponse(responseCode = "500", description = "An unexpected error occurred", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                    })
            }
    )
    @RequestMapping(
            method = RequestMethod.POST,
            value = "/v1/users",
            produces = {"application/json"},
            consumes = {"application/json"}
    )
    public ResponseEntity<UserResponse> createUser(
            @Parameter(name = "CreateUserRequest", description = "Create a new user", required = true) @Valid @RequestBody CreateUserRequest createUserRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(createUserRequest));
    }

    /**
     * GET /v1/users/{userId}
     * Fetch user by ID.
     *
     * @param userId ID of the user (required)
     * @return The user details (status code 200)
     * or The request didn&#39;t supply all the necessary data (status code 400)
     * or Access token is missing or invalid (status code 401)
     * or The user is not allowed to access the transaction (status code 403)
     * or User was not found (status code 404)
     * or An unexpected error occurred (status code 500)
     */
    @Operation(
            operationId = "fetchUserByID",
            description = "Fetch user by ID.",
            tags = {"user"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "The user details", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))
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
                    @ApiResponse(responseCode = "404", description = "User was not found", content = {
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
            value = "/v1/users/{userId}",
            produces = {"application/json"}
    )
    public ResponseEntity<UserResponse> _fetchUserByID(
            @Pattern(regexp = USER_ID_REGEX) @Parameter(name = "userId", description = "ID of the user", required = true, in = ParameterIn.PATH) @PathVariable("userId") String userId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(userService.fetchUserByID(userId));
    }

}
