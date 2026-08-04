package com.familytree.services.email;

import com.familytree.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Real email sending via AWS SES over SMTP -- only active when
 * app.email.enabled=true (set in the external, git-ignored
 * /srv/config/familytree/application.properties in production, never in
 * a committed profile file; see docs/09-security-threat-model.md item 11
 * for why a @Profile("prod") gate would be wrong here -- production
 * actually runs the "dev" Spring profile).
 */
@Service
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "true")
public class SesSmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final String fromAddress;
    private final String frontendBaseUrl;

    public SesSmtpEmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine, AppProperties appProperties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = appProperties.getEmail().getFromAddress();
        this.frontendBaseUrl = appProperties.getFrontendBaseUrl();
    }

    @Override
    public void sendVerificationOtpEmail(String toEmail, String code, String preferredLanguage) {
        String subject = "ne".equals(preferredLanguage) ? "आफ्नो इमेल पुष्टि गर्नुहोस्" : "Verify your email";
        sendOtp(toEmail, subject, "email/verify-email-otp", code, preferredLanguage);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String rawToken, String preferredLanguage) {
        String link = buildLink("/reset-password", rawToken);
        String subject = "ne".equals(preferredLanguage) ? "पासवर्ड रिसेट अनुरोध" : "Reset your password";
        send(toEmail, subject, "email/password-reset", link, preferredLanguage);
    }

    @Override
    public void sendAdminAccessOtpEmail(String toEmail, String code, String preferredLanguage) {
        String subject = "ne".equals(preferredLanguage) ? "प्रशासक पहुँच अनुरोध पुष्टि" : "Confirm your admin access request";
        sendOtp(toEmail, subject, "email/admin-access-otp", code, preferredLanguage);
    }

    private void send(String toEmail, String subject, String templateName, String actionLink, String preferredLanguage) {
        Context context = new Context();
        context.setVariable("actionLink", actionLink);
        context.setVariable("locale", preferredLanguage);
        String html = templateEngine.process(templateName, context);
        deliver(toEmail, subject, html);
    }

    private void sendOtp(String toEmail, String subject, String templateName, String code, String preferredLanguage) {
        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("locale", preferredLanguage);
        String html = templateEngine.process(templateName, context);
        deliver(toEmail, subject, html);
    }

    private void deliver(String toEmail, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (jakarta.mail.MessagingException e) {
            throw new IllegalStateException("Failed to build/send email to " + toEmail, e);
        }
    }

    private String buildLink(String path, String rawToken) {
        return frontendBaseUrl + path + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
}
