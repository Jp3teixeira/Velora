package controller;

import Database.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import utils.EmailSender;
import utils.SessaoAtual;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.regex.Pattern;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    public void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert("Todos os campos são obrigatórios.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert("As passwords não coincidem.");
            return;
        }

        if (!isPasswordStrong(password)) {
            showAlert("A password deve ter pelo menos:\n- 10 caracteres\n- 1 letra maiúscula\n- 1 número\n- 1 caractere especial.");
            return;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        try (Connection conn = DBConnection.getConnection()) {
            String insertUser = "INSERT INTO utilizadores (nome, email, senha, tipo) VALUES (?, ?, ?, 'Cliente')";
            PreparedStatement stmt = conn.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, hashedPassword);

            int linhas = stmt.executeUpdate();

            if (linhas > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int utilizadorId = rs.getInt(1);
                    SessaoAtual.utilizadorId = utilizadorId;

                    // Gerar código de verificação
                    String codigo = String.format("%06d", new Random().nextInt(999999));

                    String insertVerificacao = "INSERT INTO verificacoes_email (id, codigo, criado_em, expira_em, verificado) " +
                            "VALUES (?, ?, CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 1 DAY), FALSE)";
                    PreparedStatement verStmt = conn.prepareStatement(insertVerificacao);
                    verStmt.setInt(1, utilizadorId);
                    verStmt.setString(2, codigo);
                    verStmt.executeUpdate();

                    // Enviar email
                    if (EmailSender.sendVerificationCode(email, codigo)) {
                        showAlert("Conta registada com sucesso! Verifique o seu email.");

                        // Ir para página de verificação
                        Parent root = FXMLLoader.load(getClass().getResource("/view/verification.fxml"));
                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.show();
                        return;
                    } else {
                        showAlert("Erro ao enviar email de verificação.");
                        return;
                    }
                }
            } else {
                showAlert("Erro ao criar conta.");
            }
        } catch (SQLException | IOException e) {
            showAlert("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void goToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isPasswordStrong(String password) {
        if (password.length() < 10) return false;

        String uppercase = ".*[A-Z].*";
        String special = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*";
        String number = ".*\\d.*";

        return Pattern.matches(uppercase, password) &&
                Pattern.matches(special, password) &&
                Pattern.matches(number, password);
    }

    private void showAlert(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    private void limparCampos() {
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }
}