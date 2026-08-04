package com.familytree.config;

import com.familytree.entity.UserAccountStatus;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

/**
 * Redirects a failed login to /login?error, same as Spring Security's
 * default SimpleUrlAuthenticationFailureHandler -- except for
 * AccountNotActiveException (the password was actually correct, but the
 * account isn't ACTIVE), where it appends a specific reason
 * (?error=pending_review, ?error=disabled, ?error=locked) the Next.js
 * login page uses to show a helpful message instead of "invalid
 * credentials". Every other failure (wrong password, unknown email)
 * still gets the bare, undifferentiated ?error -- see
 * UserAccountPrincipal for why only the correct-password case is safe to
 * be specific about.
 */
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String target = "/login?error";
        if (exception instanceof AccountNotActiveException notActive) {
            String reason = reasonFor(notActive.getStatus());
            if (reason != null) {
                target = "/login?error=" + reason;
            }
        }
        redirectStrategy.sendRedirect(request, response, target);
    }

    private String reasonFor(UserAccountStatus status) {
        return switch (status) {
            case PENDING_EMAIL_VERIFICATION -> "pending_review";
            case DISABLED -> "disabled";
            case LOCKED -> "locked";
            case ACTIVE -> null;
        };
    }
}
