package com.matador.shared.security.staff;

import com.matador.shared.security.MatadorPrincipal;
import com.matador.shared.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import com.matador.shared.error.UnauthorizedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Session-cookie authentication for the admin API. */
@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "Admin-Settings")
public class AdminAuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository =
        new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy holderStrategy =
        SecurityContextHolder.getContextHolderStrategy();

    public AdminAuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record StaffProfileResponse(UUID id, String username, Role role) {}

    @PostMapping("/login")
    @Operation(summary = "Admin login", description = "Authenticate a staff member; sets a session cookie.")
    public StaffProfileResponse login(
        @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse) {
        try {
            Authentication authentication =
                authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                        request.email(), request.password()));
            SecurityContext context = holderStrategy.createEmptyContext();
            context.setAuthentication(authentication);
            holderStrategy.setContext(context);
            contextRepository.saveContext(context, httpRequest, httpResponse);
            MatadorPrincipal principal = (MatadorPrincipal) authentication.getPrincipal();
            return new StaffProfileResponse(
                principal.id(), authentication.getName(), principal.role());
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid email or password.");
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Admin logout", description = "Invalidate the current staff session.")
    public void logout(HttpServletRequest request) {
        holderStrategy.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Current staff", description = "Return the authenticated staff member.")
    public StaffProfileResponse me() {
        MatadorPrincipal principal = com.matador.shared.security.CurrentUser.require();
        return new StaffProfileResponse(principal.id(), "", principal.role());
    }
}
