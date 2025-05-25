package controller;

import Database.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;
import utils.EmailSender;
import utils.SessaoAtual;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
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
    @FXML private Label statusLabel;  // Mude para corresponder ao fx:id no FXML



    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label resetStatusLabel;

    @FXML
    private void handleRedefinirSenha() {
        String novaSenha = newPasswordField.getText();
        String confirmacao = confirmPasswordField.getText();

        if (novaSenha.isEmpty() || confirmacao.isEmpty()) {
            resetStatusLabel.setText("Por favor, preencha ambos os campos.");
            resetStatusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (!novaSenha.equals(confirmacao)) {
            resetStatusLabel.setText("As senhas não coincidem.");
            resetStatusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (!isPasswordStrong(novaSenha)) {
            resetStatusLabel.setText("A senha deve ter:\n- 10+ caracteres\n- 1 maiúscula\n- 1 número\n- 1 especial");
            resetStatusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Verifica se o código foi validado
            String checkSql = """
            SELECT verificado 
            FROM verificacoes_email 
            WHERE utilizador_id = ? 
            AND tipo = 'RECUPERACAO_SENHA'
            AND verificado = TRUE
            """;

            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, SessaoAtual.utilizadorRecuperacao);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // Atualiza a senha
                String hashed = BCrypt.hashpw(novaSenha, BCrypt.gensalt());
                String updateSql = "UPDATE utilizadores SET senha = ? WHERE id = ?";

                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, hashed);
                updateStmt.setInt(2, SessaoAtual.utilizadorRecuperacao);
                int linhas = updateStmt.executeUpdate();

                if (linhas > 0) {
                    showAlert("Senha redefinida com sucesso!", AlertType.INFORMATION);

                    // Limpa a sessão
                    SessaoAtual.utilizadorRecuperacao = 0;
                    SessaoAtual.emailRecuperacao = null;

                    // Volta para a tela de login
                    Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
                    Stage stage = (Stage) newPasswordField.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle("Login");
                }
            } else {
                resetStatusLabel.setText("Validação não encontrada. Solicite um novo código.");
                resetStatusLabel.setStyle("-fx-text-fill: red;");
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            resetStatusLabel.setText("Erro ao redefinir senha.");
            resetStatusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private TextField validationCodeField;
    @FXML
    private Label validationStatusLabel;



    @FXML
    private void handleValidarCodigoRecuperacao() {
        String codigo = validationCodeField.getText().trim();

        if (codigo.isEmpty() || codigo.length() != 6) {
            validationStatusLabel.setText("Por favor, insira um código válido de 6 dígitos.");
            validationStatusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Busca o usuário pelo email armazenado na sessão
            String email = SessaoAtual.emailRecuperacao;
            String getUserSql = "SELECT id FROM utilizadores WHERE email = ?";
            PreparedStatement getUserStmt = conn.prepareStatement(getUserSql);
            getUserStmt.setString(1, email);
            ResultSet userRs = getUserStmt.executeQuery();

            if (userRs.next()) {
                int userId = userRs.getInt("id");

                // Verifica o código
                String checkCodeSql = """
                SELECT codigo, expira_em 
                FROM verificacoes_email 
                WHERE utilizador_id = ? 
                AND tipo = 'RECUPERACAO_SENHA'
                AND verificado = FALSE
                """;

                PreparedStatement checkStmt = conn.prepareStatement(checkCodeSql);
                checkStmt.setInt(1, userId);
                ResultSet codeRs = checkStmt.executeQuery();

                if (codeRs.next()) {
                    String codigoBD = codeRs.getString("codigo");
                    Timestamp expira = codeRs.getTimestamp("expira_em");

                    if (LocalDateTime.now().isAfter(expira.toLocalDateTime())) {
                        validationStatusLabel.setText("Código expirado. Solicite um novo.");
                        validationStatusLabel.setStyle("-fx-text-fill: red;");
                        return;
                    }

                    if (codigo.equals(codigoBD)) {
                        // Marca o código como verificado
                        String updateSql = """
                        UPDATE verificacoes_email 
                        SET verificado = TRUE, verificado_em = CURRENT_TIMESTAMP 
                        WHERE utilizador_id = ? AND tipo = 'RECUPERACAO_SENHA'
                        """;

                        PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                        updateStmt.setInt(1, userId);
                        updateStmt.executeUpdate();

                        // Armazena o ID do usuário para redefinição de senha
                        SessaoAtual.utilizadorRecuperacao = userId;

                        // Abre a tela de redefinição de senha
                        abrirTelaRedefinicaoSenha();
                    } else {
                        validationStatusLabel.setText("Código incorreto.");
                        validationStatusLabel.setStyle("-fx-text-fill: red;");
                    }
                } else {
                    validationStatusLabel.setText("Nenhum código pendente encontrado.");
                    validationStatusLabel.setStyle("-fx-text-fill: red;");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            validationStatusLabel.setText("Erro ao validar código.");
            validationStatusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void abrirTelaRedefinicaoSenha() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/reset_password.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) validationCodeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Redefinir Senha");
            stage.setResizable(false);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erro ao abrir tela de redefinição.", AlertType.ERROR);
        }
    }

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

                    // Carregar saldo da carteira se for cliente
                    if ("Cliente".equals(SessaoAtual.tipo)) {
                        util.WalletRepository walletRepo = new util.WalletRepository();
                        BigDecimal saldo = walletRepo.getUserWalletBalance(SessaoAtual.utilizadorId);
                        SessaoAtual.saldoCarteira = saldo;
                    }

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

    @FXML
    private void handleLogOut(javafx.event.ActionEvent event) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmação de Logout");
        alert.setHeaderText(null);
        alert.setContentText("Tem certeza que deseja sair?");

        // Carrega o ícone para o alerta
        try (InputStream iconStream = getClass().getResourceAsStream("/moedas.png")) {
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            alertStage.getIcons().add(new Image(iconStream));
        } catch (Exception e) {
            System.err.println("Erro ao carregar ícone para alerta: " + e.getMessage());
        }

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Node source = (Node) event.getSource();
                    Stage currentStage = (Stage) source.getScene().getWindow();

                    // Carrega a tela de login
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
                    Parent root = loader.load();

                    // Configura a nova cena
                    Stage loginStage = new Stage();
                    loginStage.setTitle("Velora - Gestão de Criptomoedas");
                    loginStage.setScene(new Scene(root, 800, 600));
                    loginStage.setResizable(false);

                    // Adiciona o ícone à nova janela
                    try (InputStream iconStream = getClass().getResourceAsStream("/moedas.png")) {
                        loginStage.getIcons().add(new Image(iconStream));
                    } catch (Exception e) {
                        System.err.println("Erro ao carregar ícone: " + e.getMessage());
                    }

                    // Fecha a janela atual e abre a nova
                    currentStage.close();
                    loginStage.show();

                } catch (Exception e) {
                    e.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Erro");
                    errorAlert.setHeaderText("Erro ao carregar tela de login");
                    errorAlert.setContentText(e.getMessage());
                    errorAlert.showAndWait();
                }
            }
        });
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

                    // CRIAR CARTEIRA AUTOMATICAMENTE PARA CLIENTES
                    if ("Cliente".equals("Cliente")) { // Ou qualquer lógica que determine se é cliente
                        util.WalletRepository walletRepo = new util.WalletRepository();
                        boolean walletCreated = walletRepo.createWalletForUser(utilizadorId);

                        if (!walletCreated) {
                            showAlert("Conta criada, mas falha ao criar carteira.");
                            return;
                        }
                    }

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
        String email = forgotEmailField.getText().trim();

        if (email.isEmpty()) {
            showAlert("Por favor, insira o seu e-mail.", AlertType.WARNING);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Verifica se o email existe
            String checkUserSql = "SELECT id FROM utilizadores WHERE email = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkUserSql);
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");

                // Gera um código de 6 dígitos
                String codigo = String.format("%06d", new Random().nextInt(999999));
                LocalDateTime expira = LocalDateTime.now().plusHours(1); // Expira em 1 hora

                // Insere ou atualiza o registro na tabela verificacoes_email
                String sql = """
                INSERT INTO verificacoes_email 
                (utilizador_id, codigo, criado_em, expira_em, verificado, tipo) 
                VALUES (?, ?, CURRENT_TIMESTAMP, ?, FALSE, 'RECUPERACAO_SENHA')
                ON DUPLICATE KEY UPDATE
                codigo = VALUES(codigo),
                criado_em = CURRENT_TIMESTAMP,
                expira_em = VALUES(expira_em),
                verificado = FALSE,
                verificado_em = NULL,
                tipo = VALUES(tipo)
                """;

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, userId);
                stmt.setString(2, codigo);
                stmt.setTimestamp(3, Timestamp.valueOf(expira));
                stmt.executeUpdate();

                // Envia o e-mail (simulado ou real)
                System.out.println("[DEBUG] Código de recuperação: " + codigo);
                boolean enviado = EmailSender.sendRecoveryCode(email, codigo);

                if (enviado) {
                    statusLabel.setText("Código enviado para: " + email);
                    statusLabel.setStyle("-fx-text-fill: green;");

                    // Armazena o email na sessão para uso posterior
                    SessaoAtual.emailRecuperacao = email;

                    // Abre a tela de inserção do código
                    abrirTelaValidacaoCodigo();
                } else {
                    showAlert("Falha ao enviar e-mail. Tente novamente.", AlertType.ERROR);
                }
            } else {
                showAlert("E-mail não encontrado.", AlertType.ERROR);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erro ao processar solicitação: " + e.getMessage(), AlertType.ERROR);
        }
    }

    private void abrirTelaValidacaoCodigo() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/code_validation.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) forgotEmailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Validar Código");
            stage.setResizable(false);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erro ao abrir tela de validação.", AlertType.ERROR);
        }
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


