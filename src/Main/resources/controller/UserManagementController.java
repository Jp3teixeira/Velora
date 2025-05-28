package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;
import Repository.UserRepository;
import utils.EmailSender;
import utils.SessaoAtual;
import Repository.WalletRepository;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.regex.Pattern;

public class UserManagementController {

    private final UserRepository userRepository = new UserRepository();

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
    @FXML private Label statusLabel;

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label resetStatusLabel;

    @FXML private TextField validationCodeField;
    @FXML private Label validationStatusLabel;

    @FXML private TextField codigoField;

    // Botão Admin
    @FXML private Button adminButton;
    @FXML private StackPane contentArea;  // vinculado ao fx:id do center


    @FXML
    public void initialize() {
        // Controle de visibilidade do botão admin
        if (adminButton != null) {
            boolean isAdmin = "Admin".equals(SessaoAtual.tipo);
            adminButton.setVisible(isAdmin);
            adminButton.setManaged(isAdmin);
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

        var userOpt = userRepository.findUserByEmailOrUsername(input);
        if (userOpt.isPresent()) {
            var user = userOpt.get();
            int id = Integer.parseInt(user.get("id"));
            String hashed = user.get("senha");

            if (!userRepository.isContaVerificada(id)) {
                showAlert("A conta ainda não foi verificada.", AlertType.WARNING);
                return;
            }

            if (BCrypt.checkpw(password, hashed)) {
                SessaoAtual.utilizadorId = id;
                SessaoAtual.nome = user.get("nome");
                SessaoAtual.email = user.get("email");
                SessaoAtual.tipo = user.get("tipo");
                SessaoAtual.isSuperAdmin = Boolean.parseBoolean(user.getOrDefault("is_super_admin", "false"));

                // Atualiza visibilidade do botão admin
                if (adminButton != null) {
                    boolean isAdmin = "Admin".equals(SessaoAtual.tipo);
                    adminButton.setVisible(isAdmin);
                    adminButton.setManaged(isAdmin);
                }

                BigDecimal saldo = new WalletRepository().getUserWalletBalance(id);
                SessaoAtual.saldoCarteira = saldo;

                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/view/homepage.fxml"));
                    Stage stage = (Stage) loginEmailField.getScene().getWindow();
                    stage.setScene(new Scene(root, 1200, 700));
                    stage.setFullScreen(true);
                    stage.setTitle("Velora - Gestão de Criptomoedas");

                    try (InputStream iconStream = getClass().getResourceAsStream("/icons/moedas.png")) {
                        if (iconStream != null) {
                            stage.getIcons().add(new Image(iconStream));
                        }
                    } catch (Exception e) {
                        System.err.println("Erro ao carregar ícone: " + e.getMessage());
                    }

                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Erro ao carregar a página inicial.", AlertType.ERROR);
                }
            } else {
                showAlert("Password incorreta.", AlertType.ERROR);
            }
        } else {
            showAlert("Utilizador não encontrado.", AlertType.ERROR);
        }
    }

    @FXML
    private void goToAdminPanel(ActionEvent event) {
        if (!"Admin".equals(SessaoAtual.tipo)) {
            showAlert("Acesso não autorizado!", AlertType.ERROR);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin_dashboard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) adminButton.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 700));
            stage.setTitle("Painel de Administração");
            stage.setFullScreen(true);


        } catch (IOException e) {
            showAlert("Erro ao carregar o painel admin: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    // ================= LOGOUT =================
    @FXML
    private void handleLogOut(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmação de Logout");
        alert.setHeaderText(null);
        alert.setContentText("Tem certeza que deseja sair?");

        try (InputStream iconStream = getClass().getResourceAsStream("/icons/moedas.png")) {
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

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
                    Parent root = loader.load();

                    Stage loginStage = new Stage();
                    loginStage.setTitle("Velora - Gestão de Criptomoedas");
                    loginStage.setScene(new Scene(root, 800, 600));
                    loginStage.setResizable(false);

                    try (InputStream iconStream = getClass().getResourceAsStream("/icons/moedas.png")) {
                        loginStage.getIcons().add(new Image(iconStream));
                    } catch (Exception e) {
                        System.err.println("Erro ao carregar ícone: " + e.getMessage());
                    }

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

        var userIdOpt = userRepository.registarNovoUtilizador(username, email, hashedPassword);
        if (userIdOpt.isEmpty()) {
            showAlert("Erro ao criar conta.");
            return;
        }

        int userId = userIdOpt.get();
        SessaoAtual.utilizadorId = userId;

        if (!new WalletRepository().createWalletForUser(userId)) {
            showAlert("Conta criada, mas falha ao criar carteira.");
            return;
        }

        String codigo = String.format("%06d", new Random().nextInt(999999));
        boolean codigoCriado = userRepository.inserirCodigoVerificacao(userId, codigo, LocalDateTime.now().plusDays(1), "REGISTO");

        if (codigoCriado && EmailSender.sendVerificationCode(email, codigo)) {
            showAlert("Conta registada com sucesso! Verifique o seu email.");
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/view/verification.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Erro ao abrir página de verificação.");
            }
        } else {
            showAlert("Erro ao enviar código de verificação.");
        }
    }

    @FXML
    private void handleVerify(ActionEvent event) {
        String codigo = codigoField.getText().trim();

        if (codigo.isEmpty()) {
            showAlert("Por favor, insira o código.");
            return;
        }

        if (userRepository.validarCodigo(SessaoAtual.utilizadorId, "REGISTO", codigo)) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/view/homepage.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setFullScreen(true);
                stage.setTitle("Velora - Gestão de Criptomoedas");
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Erro ao carregar homepage.");
            }
        } else {
            showAlert("Código incorreto ou expirado.");
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

        var userIdOpt = userRepository.getUserIdByEmail(email);
        if (userIdOpt.isEmpty()) {
            showAlert("E-mail não encontrado.", AlertType.ERROR);
            return;
        }

        int userId = userIdOpt.get();
        SessaoAtual.emailRecuperacao = email;

        String codigo = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime expira = LocalDateTime.now().plusHours(1);

        boolean criado = userRepository.inserirCodigoVerificacao(userId, codigo, expira, "RECUPERACAO_SENHA");
        if (criado && EmailSender.sendRecoveryCode(email, codigo)) {
            statusLabel.setText("Código enviado para: " + email);
            statusLabel.setStyle("-fx-text-fill: green;");
            abrirTelaValidacaoCodigo();
        } else {
            showAlert("Erro ao enviar e-mail. Tente novamente.", AlertType.ERROR);
        }
    }

    @FXML
    private void handleValidarCodigoRecuperacao() {
        String codigo = validationCodeField.getText().trim();

        if (codigo.isEmpty() || codigo.length() != 6) {
            validationStatusLabel.setText("Insira um código válido de 6 dígitos.");
            validationStatusLabel.setStyle("-fx-text-fill: #ffffff;");
            return;
        }

        var userIdOpt = userRepository.getUserIdByEmail(SessaoAtual.emailRecuperacao);
        if (userIdOpt.isEmpty()) {
            validationStatusLabel.setText("Utilizador não encontrado.");
            return;
        }

        int userId = userIdOpt.get();
        if (userRepository.validarCodigo(userId, "RECUPERACAO_SENHA", codigo)) {
            SessaoAtual.utilizadorRecuperacao = userId;
            abrirTelaRedefinicaoSenha();
        } else {
            validationStatusLabel.setText("Código incorreto ou expirado.");
            validationStatusLabel.setStyle("-fx-text-fill: red;");
        }
    }

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

        String hashed = BCrypt.hashpw(novaSenha, BCrypt.gensalt());
        boolean atualizada = userRepository.atualizarSenha(SessaoAtual.utilizadorRecuperacao, hashed);

        if (atualizada) {
            showAlert("Senha redefinida com sucesso!");
            SessaoAtual.utilizadorRecuperacao = 0;
            SessaoAtual.emailRecuperacao = null;
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
                Stage stage = (Stage) newPasswordField.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Login");
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Erro ao carregar tela de login.");
            }
        } else {
            resetStatusLabel.setText("Erro ao redefinir senha.");
            resetStatusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // ================= NAVEGAÇÃO =================
    public void goToRegister(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/register.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
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

    @FXML
    private void goToMarket(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/market.fxml"));
            Parent marketRoot = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(marketRoot);
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= UTILS =================
    private boolean isPasswordStrong(String password) {
        if (password.length() < 10) return false;
        return Pattern.matches(".*[A-Z].*", password)
                && Pattern.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*", password)
                && Pattern.matches(".*\\d.*", password);
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