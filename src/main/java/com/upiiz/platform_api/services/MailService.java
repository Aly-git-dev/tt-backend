package com.upiiz.platform_api.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Nullable
    private final JavaMailSender mailSender; // será null si no configuras spring.mail.*

    @Autowired
    public MailService(@Nullable JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    public void sendPasswordResetEmail(String to, String token) {

        String link = "http://localhost:4200/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Recuperación de contraseña");
        message.setText(
                "Solicitaste restablecer tu contraseña.\n\n" +
                        "Haz clic aquí:\n" + link + "\n\n" +
                        "Expira en 30 minutos."
        );

        mailSender.send(message);
    }
    public void sendVerificationEmail(String to, String link) {
        if (mailSender == null) {
            // Modo DEV: imprime en consola si no hay SMTP configurado
            System.out.println("[DEV][MAIL] To: " + to + " | Link: " + link);
            return;
        }
        var msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Confirma tu correo - Plataforma UPIIZ");
        msg.setText("Hola,\n\nPor favor confirma tu correo haciendo clic en el siguiente enlace:\n" + link + "\n\nSi no fuiste tú, ignora este mensaje.");
        mailSender.send(msg);
    }
}
