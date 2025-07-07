package com.eaglebank.controller;

import com.eaglebank.model.ErrorResponse;
import com.eaglebank.model.auth.AuthRequest;
import com.eaglebank.model.auth.AuthResponse;
import com.eaglebank.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.web.bind.annotation.*;

/**
 * Controller that handles user authentication-related endpoints.
 * Currently supports login and JWT generation.
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Authenticates the user and generates a JWT token.
     *
     * @param request
     * @return
     */
    @Operation(
            operationId = "authenticateUser",
            description = "Authenticate user and generate JWT token.",
            tags = {"auth"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "JWT token generated successfully", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))
                    }),
                    @ApiResponse(responseCode = "401", description = "Invalid username or password", content = {
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
            value = "/login",
            produces = {"application/json"},
            consumes = {"application/json"}
    )
    public AuthResponse login(@RequestBody AuthRequest request) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            String token = jwtUtil.generateToken(request.getUsername());
            return new AuthResponse(token);
        }
        catch (BadCredentialsException | InternalAuthenticationServiceException e) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }
}
