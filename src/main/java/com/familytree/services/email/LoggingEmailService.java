package com.familytree.services.email;

import com.familytree.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * The default EmailService everywhere app.email.enabled isn't explicitly
 * set to true -- local dev, CI, and (until the external config is set up)
 * even production ship with this and never need real SES/SMTP
 * credentials. Just logs the link a real email would have contained.
 */
@Service
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    private final String frontendBaseUrl;

    public LoggingEmailService(AppProperties appProperties) {
        this.frontendBaseUrl = appProperties.getFrontendBaseUrl();
    }

    @Override
    public void sendVerificationOtpEmail(String toEmail, String code, String preferredLanguage) {
        log.info("[DEV] Email verification OTP for {}: {}", toEmail, code);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String rawToken, String preferredLanguage) {
        log.info("[DEV] Password reset email for {}: {}", toEmail, buildLink("/reset-password", rawToken));
    }

    @Override
    public void sendAdminAccessOtpEmail(String toEmail, String code, String preferredLanguage) {
        log.info("[DEV] Admin access request OTP for {}: {}", toEmail, code);
    }

    private String buildLink(String path, String rawToken) {
        return frontendBaseUrl + path + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
}
