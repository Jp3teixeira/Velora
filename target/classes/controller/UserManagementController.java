package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import model.Moeda;
import org.mindrot.jbcrypt.BCrypt;
import repository.UserRepository;
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

    @FXML private Button coinsButton;

    // ================= NAVEGAÇÃO CENTRALIZADA =================
    public void navegarPara(String fxmlPath, boolean fullscreen) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = getStageAtual();
            stage.setScene(new Scene(root));
            if (fullscreen) stage.setFullScreen(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarErro("Erro ao carregar: " + fxmlPath);
        }
    }

    public <T> void navegarComController(String fxmlPath, ControllerConsumer<T> controllerConsumer, boolean fullscreen) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            T controller = loader.getController();
            controllerConsumer.accept(controller);

            Stage stage = getStageAtual();
            stage.setScene(new Scene(root));
            if (fullscreen) stage.setFullScreen(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarErro("Erro ao carregar com dados: " + fxmlPath);
        }
    }

    private Stage getStageAtual() {
        return (Stage) (coinsButton != null ? coinsButton.getScene().getWindow() : new Stage());
    }

    @FunctionalInterface
    public interface ControllerConsumer<T> {
        void accept(T controller);
    }

    private void mostrarErro(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Erro de Navegação");
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    @FXML
    private void handleMenuNavigation(ActionEvent event) {
        Button clicked = (Button) event.getSource();

        switch (clicked.getId()) {
            case "marketButton" -> navegarPara("/view/market.fxml", true);
            case "coinsButton" -> navegarPara("/view/moeda.fxml", true);
            case "homeButton" -> navegarPara("/view/homepage.fxml", true);
            case "loginButton" -> navegarPara("/view/login.fxml", false);
            case "registerButton" -> navegarPara("/view/register.fxml", false);
            case "forgotButton" -> navegarPara("/view/forgot_password.fxml", false);
            case "resetButton" -> navegarPara("/view/reset_password.fxml", false);
            case "verifyButton" -> navegarPara("/view/verification.fxml", false);
            case "codeButton" -> navegarPara("/view/code_verification.fxml", false);
            default -> mostrarErro("Página não encontrada para: " + clicked.getId());
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

                if ("Cliente".equals(SessaoAtual.tipo)) {
                    BigDecimal saldo = new WalletRepository().getUserWalletBalance(id);
                    SessaoAtual.saldoCarteira = saldo;
                }

                navegarPara("/view/homepage.fxml", true);
            } else {
                showAlert("Password incorreta.", AlertType.ERROR);
            }
        } else {
            showAlert("Utilizador não encontrado.", AlertType.ERROR);
        }
    }

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
                navegarPara("/view/login.fxml", false);
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
            navegarPara("/view/verification.fxml", false);
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
            navegarPara("/view/homepage.fxml", true);
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
            navegarPara("/view/code_validation.fxml", false);
        } else {
            showAlert("Erro ao enviar e-mail. Tente novamente.", AlertType.ERROR);
        }
    }

    @FXML
    private void handleValidarCodigoRecuperacao() {
        String codigo = validationCodeField.getText().trim();

        if (codigo.isEmpty() || codigo.length() != 6) {
            validationStatusLabel.setText("Insira um código válido de 6 dígitos.");
            validationStatusLabel.setStyle("-fx-text-fill: red;");
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
            navegarPara("/view/reset_password.fxml", false);
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
            navegarPara("/view/login.fxml", false);
        } else {
            resetStatusLabel.setText("Erro ao redefinir senha.");
            resetStatusLabel.setStyle("-fx-text-fill: red;");
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
