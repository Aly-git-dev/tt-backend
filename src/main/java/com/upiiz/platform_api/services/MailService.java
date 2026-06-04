package com.upiiz.platform_api.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Nullable
    private final JavaMailSender mailSender;
    private final String mailUsername;
    private final String mailPassword;
    private final String frontendUrl;

    @Autowired
    public MailService(
            @Nullable JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${app.frontend.url}") String frontendUrl
    ) {
        this.mailSender = mailSender;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
        this.frontendUrl = frontendUrl;
    }

    public void sendPasswordResetEmail(String to, String token) {
        String link = normalizeBaseUrl(frontendUrl) + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Recuperacion de contrasena");
        message.setText(
                "Solicitaste restablecer tu contrasena.\n\n" +
                        "Haz clic aqui:\n" + link + "\n\n" +
                        "Expira en 30 minutos."
        );

        sendOrLogDev(message, "[DEV][MAIL][PASSWORD_RESET] To: " + to + " | Link: " + link);
    }

    public void sendVerificationEmail(String to, String link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Confirma tu correo - Plataforma UPIIZ");
        message.setText(
                "Hola,\n\nPor favor confirma tu correo haciendo clic en el siguiente enlace:\n" +
                        link +
                        "\n\nSi no fuiste tu, ignora este mensaje."
        );

        sendOrLogDev(message, "[DEV][MAIL][VERIFY] To: " + to + " | Link: " + link);
    }

    private void sendOrLogDev(SimpleMailMessage message, String devMessage) {
        if (!isSmtpConfigured()) {
            log.info(devMessage);
            return;
        }

        message.setFrom(mailUsername);
        mailSender.send(message);
    }

    private boolean isSmtpConfigured() {
        return mailSender != null
                && StringUtils.hasText(mailUsername)
                && StringUtils.hasText(mailPassword);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "http://localhost:4200";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
