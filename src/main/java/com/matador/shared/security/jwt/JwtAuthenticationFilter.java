package com.matador.shared.security.jwt;

import com.matador.shared.error.UnauthorizedException;
import com.matador.shared.security.AuthenticatedUser;
import com.matador.shared.security.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates customer requests carrying a {@code Authorization: Bearer <jwt>} header.
 * Sets an {@link AuthenticatedUser} principal (role {@code CUSTOMER}) on success.
 * Missing/blank header is left unauthenticated for downstream authorization to reject.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                UUID customerId = jwtService.verifyAccessToken(token);
                AuthenticatedUser principal =
                    new AuthenticatedUser(customerId, customerId.toString(), Role.CUSTOMER);
                var authentication =
                    new UsernamePasswordAuthenticationToken(
                        principal, null, principal.authorities());
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (UnauthorizedException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
