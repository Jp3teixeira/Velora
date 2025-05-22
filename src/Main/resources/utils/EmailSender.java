
package utils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class EmailSender {

    public static boolean sendVerificationCode(String toEmail, String codigo) {
        try {
            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // Substitui abaixo pela tua API key do Brevo
            String apiKey = " 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";
            conn.setRequestProperty("api-key", apiKey);
            conn.setRequestProperty("Content-Type", "application/json");

            String body = "{"
                    + "\"sender\": {\"name\": \"Velora\", \"email\": \"VeloraAPI@gmail.com\"},"
                    + "\"to\": [{\"email\": \"" + toEmail + "\"}],"
                    + "\"subject\": \"Código de Verificação - Velora\","
                    + "\"htmlContent\": \"<p>Olá!</p><p>O seu código de verificação é: <strong>" + codigo + "</strong></p><p>Velora © 2025</p>\""
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            return code >= 200 && code < 300;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

}