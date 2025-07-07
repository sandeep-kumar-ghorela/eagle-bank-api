package com.eaglebank.filter;

import com.eaglebank.service.impl.AuthServiceImpl;
import com.eaglebank.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

import static com.eaglebank.constants.EagleBankApiConstants.ERROR;
import static com.eaglebank.constants.EagleBankApiConstants.NOT_A_VALID_TOKEN;

/**
 * A custom filter that intercepts HTTP requests to validate JWT tokens.
 * <p>
 * If a valid JWT is present in the Authorization header, the user is authenticated
 * and the security context is set. Otherwise, the request continues without authentication.
 * <p>
 * This filter only runs once per request.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AuthServiceImpl loginService;

    /**
     * Intercepts the request to check for a valid JWT token in the Authorization header.
     * <p>
     * If valid, sets the authenticated user in the security context.
     * If invalid, returns a 403 Forbidden response with error details.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain to pass the request along
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwtToken = null;
        try {

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwtToken = authHeader.substring(7);
                username = jwtUtil.extractUsername(jwtToken);
            }


            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = loginService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwtToken, userDetails)) {
                    UsernamePasswordAuthenticationToken token =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(token);
                }
            }

            filterChain.doFilter(request, response);
        }//ExpiredJwtException | SignatureException
        catch (Exception e) {
            handleJwtException(response, e);
        }
    }

    /**
     * Handles exceptions related to JWT validation.
     * <p>
     * Sends a 403 Forbidden response with a JSON error message.
     *
     * @param response the HTTP response
     * @param ex       the exception to handle
     * @throws IOException if writing to the response fails
     */
    private void handleJwtException(HttpServletResponse response, Exception ex) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        String json = new ObjectMapper().writeValueAsString(
                Map.of(ERROR, NOT_A_VALID_TOKEN + ex.getMessage())
        );
        response.getWriter().write(json);
    }

}
