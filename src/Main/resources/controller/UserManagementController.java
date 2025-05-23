package controller;

import Database.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;
import utils.EmailSender;
import utils.SessaoAtual;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.regex.Pattern;

public class UserManagementController {

    // Login
    @FXML private TextField loginEmailField;
    @FXML private PasswordField loginPasswordField;

    // Registo
    @FXML private TextField registerUsernameField;
    @FXML private TextField registerEmailField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField registerConfirmPasswordField;



    // Recuperação de senha
    @FXML private TextField forgotEmailField;
    @FXML private Label forgotStatusLabel;

    // ================= LOGIN =================
    @FXML
    private void handleLogin() {
        String input = loginEmailField.getText().trim();
        String password = loginPasswordField.getText();

        if (input.isEmpty() || password.isEmpty()) {
            showAlert("Por favor, preencha todos os campos.", AlertType.ERROR);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM utilizadores WHERE email = ? OR nome = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, input);
            stmt.setString(2, input);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hashed = rs.getString("senha");
                boolean verificado = isContaVerificada(conn, rs.getInt("id"));

                if (!verificado) {
                    showAlert("A conta ainda não foi verificada. Verifique o seu email.", AlertType.WARNING);
                    return;
                }

                if (BCrypt.checkpw(password, hashed)) {
                    SessaoAtual.utilizadorId = rs.getInt("id");
                    SessaoAtual.nome = rs.getString("nome");
                    SessaoAtual.email = rs.getString("email");
                    SessaoAtual.tipo = rs.getString("tipo");

                    Parent root = FXMLLoader.load(getClass().getResource("/view/homepage.fxml"));
                    Stage stage = (Stage) loginEmailField.getScene().getWindow();
                    stage.setScene(new Scene(root, 800, 600));
                    stage.setFullScreen(true);
                    stage.setTitle("Velora - Gestão de Criptomoedas");
                    stage.show();
                } else {
                    showAlert("Password incorreta.", AlertType.ERROR);
                }
            } else {
                showAlert("Utilizador não encontrado.", AlertType.ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erro: " + e.getMessage(), AlertType.ERROR);
        }
    }
    public void goToRegister(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/register.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Erro ao abrir a tela de registo.", AlertType.ERROR);
            e.printStackTrace();
        }
    }
    @FXML
    private void abrirTelaRecuperacao() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/forgot_password.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Recuperar Senha");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            showAlert("Erro ao abrir a tela de recuperação de senha.", AlertType.ERROR);
            e.printStackTrace();
        }
    }


    // ================= REGISTO =================
    @FXML
    private void handleRegister(ActionEvent event) {
        String username = registerUsernameField.getText().trim();
        String email = registerEmailField.getText().trim();
        String password = registerPasswordField.getText();
        String confirmPassword = registerConfirmPasswordField.getText();

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

                    String codigo = String.format("%06d", new Random().nextInt(999999));
                    LocalDateTime expira = LocalDateTime.now().plusDays(1);

                    String insertVerificacao = """
                    INSERT INTO verificacoes_email (utilizador_id, codigo, criado_em, expira_em, verificado)
                     VALUES (?, ?, CURRENT_TIMESTAMP, ?, FALSE)
                      ON DUPLICATE KEY UPDATE
                    codigo = VALUES(codigo),
                     criado_em = CURRENT_TIMESTAMP,
                       expira_em = VALUES(expira_em),
                        verificado = FALSE,
                      verificado_em = NULL
""";

                    PreparedStatement verStmt = conn.prepareStatement(insertVerificacao);
                    verStmt.setInt(1, utilizadorId);
                    verStmt.setString(2, codigo);
                    verStmt.setTimestamp(3, Timestamp.valueOf(expira));
                    verStmt.executeUpdate();


                    System.out.println("[DEBUG] A enviar email com código: " + codigo + " para " + email);
                    boolean enviado = EmailSender.sendVerificationCode(email, codigo);


                    if (enviado) {
                        showAlert("Conta registada com sucesso! Verifique o seu email.");
                        Parent root = FXMLLoader.load(getClass().getResource("/view/verification.fxml"));
                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.show();
                    } else {
                        showAlert("Erro ao enviar email de verificação.");
                    }
                }
            } else {
                showAlert("Erro ao criar conta.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erro: " + e.getMessage());
        }
    }
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

    // ================= VERIFICAÇÃO =================
    @FXML
    private TextField codigoField;

    @FXML
    private void handleVerify(ActionEvent event) {
        String codigoInserido = codigoField.getText().trim();

        if (codigoInserido.isEmpty()) {
            showAlert("Por favor, insira o código.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT codigo, expira_em FROM verificacoes_email WHERE utilizador_id = ? AND verificado = FALSE";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, SessaoAtual.utilizadorId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String codigoBD = rs.getString("codigo");
                Timestamp expira = rs.getTimestamp("expira_em");

                if (LocalDateTime.now().isAfter(expira.toLocalDateTime())) {
                    showAlert("O código expirou. Solicite um novo.");
                    return;
                }

                if (codigoInserido.equals(codigoBD)) {
                    // Atualiza verificação
                    String update = """
                    UPDATE verificacoes_email 
                    SET verificado = TRUE, verificado_em = CURRENT_TIMESTAMP 
                    WHERE utilizador_id = ?
                """;
                    PreparedStatement updateStmt = conn.prepareStatement(update);
                    updateStmt.setInt(1, SessaoAtual.utilizadorId);
                    updateStmt.executeUpdate();

                    // Redireciona para a homepage
                    Parent root = FXMLLoader.load(getClass().getResource("/view/homepage.fxml"));
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    Scene scene = new Scene(root);
                    stage.setScene(scene);
                    stage.setFullScreen(true);
                    stage.setTitle("Velora - Gestão de Criptomoedas");
                    stage.show();

                } else {
                    showAlert("Código incorreto.");
                }
            } else {
                showAlert("Nenhum código encontrado ou já foi verificado.");
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            showAlert("Erro: " + e.getMessage());
        }
    }


    // ================= RECUPERAÇÃO DE SENHA =================
    @FXML
    private void handleEnviarLink() {
        String email = forgotEmailField.getText();

        if (email == null || email.isEmpty()) {
            showAlert("Por favor, insira o seu e-mail.", AlertType.WARNING);
            return;
        }

        // Simulando envio
        showAlert("Um link de recuperação seria enviado para: " + email, AlertType.INFORMATION);
        forgotStatusLabel.setText("Link enviado para o email: " + email);
    }

    // ================= UTILS =================
    private boolean isPasswordStrong(String password) {
        if (password.length() < 10) return false;
        return Pattern.matches(".*[A-Z].*", password)
                && Pattern.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*", password)
                && Pattern.matches(".*\\d.*", password);
    }

    private boolean isContaVerificada(Connection conn, int utilizadorId) throws SQLException {
        String sql = "SELECT verificado FROM verificacoes_email WHERE utilizador_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, utilizadorId);
        ResultSet rs = stmt.executeQuery();
        return rs.next() && rs.getBoolean("verificado");
    }


    private void showAlert(String mensagem) {
        showAlert(mensagem, AlertType.INFORMATION);
    }

    private void showAlert(String mensagem, AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Mensagem");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
