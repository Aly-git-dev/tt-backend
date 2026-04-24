package com.upiiz.platform_api.services;

import io.micrometer.common.lang.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Nullable
    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    public MailService(@Nullable JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String token) {

        String link = frontendUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Recuperación de contraseña");
        message.setText(
                "Solicitaste restablecer tu contraseña.\n\n" +
                        "Haz clic aquí:\n" + link + "\n\n" +
                        "Expira en 30 minutos."
        );

        if (mailSender != null) {
            mailSender.send(message);
        } else {
            System.out.println("[DEV][MAIL] " + link);
        }
    }
}