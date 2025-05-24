package utils;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailSender {
    // Configurações do Gmail SMTP
    private static final String GMAIL_USER = "veloraapi@gmail.com"; // Substitua pelo seu e-mail
    private static final String GMAIL_PASSWORD = "lexp bvwk prcd hsli"; // Use senha de app se tiver 2FA ativado
    private static final String FROM_NAME = "Velora";

    /**
     * Configuração básica da sessão SMTP
     */
    private static Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(GMAIL_USER, GMAIL_PASSWORD);
            }
        });
    }

    /**
     * Envia código de verificação por e-mail
     */
    public static boolean sendVerificationCode(String toEmail, String codigo) {
        try {
            Message message = new MimeMessage(createSession());
            message.setFrom(new InternetAddress(GMAIL_USER, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Código de Verificação - Velora");

            String htmlContent = "<html><body style='font-family: Arial, sans-serif;'>"
                    + "<h2 style='color: #4B3F72;'>Verificação de E-mail</h2>"
                    + "<p>Olá!</p>"
                    + "<p>Seu código de verificação é: <strong>" + codigo + "</strong></p>"
                    + "<p>Use este código para completar seu cadastro no Velora.</p>"
                    + "<p>Velora © 2025</p>"
                    + "</body></html>";

            message.setContent(htmlContent, "text/html; charset=utf-8");
            Transport.send(message);
            return true;

        } catch (Exception e) {
            System.err.println("Erro ao enviar e-mail de verificação: " + e.getMessage());
            return false;
        }
    }

    /**
     * Envia código de recuperação de senha
     */
    public static boolean sendRecoveryCode(String toEmail, String codigo) {
        try {
            Message message = new MimeMessage(createSession());
            message.setFrom(new InternetAddress(GMAIL_USER, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Recuperação de Senha - Velora");

            String htmlContent = "<html><body style='font-family: Arial, sans-serif;'>"
                    + "<h2 style='color: #4B3F72;'>Recuperação de Senha</h2>"
                    + "<p>Olá!</p>"
                    + "<p>Você solicitou a recuperação de senha. Seu código é: <strong>" + codigo + "</strong></p>"
                    + "<p style='color: #666;'>Este código expirará em 1 hora.</p>"
                    + "<p>Se não foi você quem solicitou, por favor ignore este e-mail.</p>"
                    + "<p>Velora © 2025</p>"
                    + "</body></html>";

            message.setContent(htmlContent, "text/html; charset=utf-8");
            Transport.send(message);
            return true;

        } catch (Exception e) {
            System.err.println("Erro ao enviar e-mail de recuperação: " + e.getMessage());
            return false;
        }
    }
}