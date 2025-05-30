package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;
import Repository.UserRepository;
import Repository.WalletRepository;
import utils.EmailSender;
import utils.SessaoAtual;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Random;
import static utils.NavigationHelper.goTo;

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

    // Verificação
    @FXML private TextField codigoField;

    // Recuperação de senha
    @FXML private TextField forgotEmailField;
    @FXML private Label statusLabel;
    @FXML private TextField validationCodeField;
    @FXML private Label validationStatusLabel;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label resetStatusLabel;
    @FXML private VBox painelEmail;
    @FXML private VBox painelCodigo;
    @FXML private VBox painelReset;
    @FXML private Button btnEnviarCodigo;



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
        if (userOpt.isEmpty()) {
            showAlert("Utilizador não encontrado.", AlertType.ERROR);
            return;
        }

        var user = userOpt.get();
        int id = Integer.parseInt(user.get("id"));
        String hashed = user.get("senha");

        if (!userRepository.isContaVerificada(id)) {
            showAlert("A conta ainda não foi verificada.", AlertType.WARNING);
            return;
        }

        if (!BCrypt.checkpw(password, hashed)) {
            showAlert("Password incorreta.", AlertType.ERROR);
            return;
        }

        SessaoAtual.utilizadorId = id;
        SessaoAtual.nome = user.get("nome");
        SessaoAtual.email = user.get("email");
        SessaoAtual.tipo = user.get("tipo");
        SessaoAtual.saldoCarteira = WalletRepository.getInstance().getSaldo(id);


        goTo("/view/homepage.fxml", true);
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
            showAlert("A password deve ter pelo menos:\n- 10 caracteres\n- 1 maiúscula\n- 1 número\n- 1 especial.");
            return;
        }

        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        var userIdOpt = userRepository.registarNovoUtilizador(username, email, hashed);
        if (userIdOpt.isEmpty()) {
            showAlert("Erro ao criar conta.");
            return;
        }

        int userId = userIdOpt.get();
        SessaoAtual.utilizadorId = userId;

        if (!WalletRepository.getInstance().createWalletForUser(userId)) {
            showAlert("Conta criada, mas erro ao criar carteira.");
            return;
        }

        String codigo = String.format("%06d", new Random().nextInt(999999));
        if (userRepository.inserirCodigoVerificacao(userId, codigo, LocalDateTime.now().plusDays(1), "REGISTO")
                && EmailSender.sendVerificationCode(email, codigo)) {
            showAlert("Verifique o seu email.");
            goTo("/view/verification.fxml", false);
        } else {
            showAlert("Erro ao enviar código.");
        }
    }

    @FXML
    private void handleVerify(ActionEvent event) {
        String codigo = codigoField.getText().trim();
        if (codigo.isEmpty()) {
            showAlert("Insira o código.");
            return;
        }

        if (userRepository.validarCodigo(SessaoAtual.utilizadorId, "REGISTO", codigo)) {
            goTo("/view/homepage.fxml", true);
        } else {
            showAlert("Código incorreto ou expirado.");
        }
    }

    // ================= RECUPERAÇÃO DE SENHA =================
    @FXML
    private void handleEnviarLink() {
        String email = forgotEmailField.getText().trim();
        if (email.isEmpty()) {
            showAlert("Insira o e-mail.", AlertType.WARNING);
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
        if (userRepository.inserirCodigoVerificacao(userId, codigo, LocalDateTime.now().plusHours(1), "RECUPERACAO_SENHA")
                && EmailSender.sendRecoveryCode(email, codigo)) {
            statusLabel.setText("Código enviado para: " + email);
            statusLabel.setStyle("-fx-text-fill: green;");
            painelEmail.setVisible(false);
            painelCodigo.setVisible(true);
        } else {
            showAlert("Erro ao enviar código.");
        }
    }

    @FXML
    private void handleValidarCodigoRecuperacao() {
        String codigo = validationCodeField.getText().trim();
        if (codigo.length() != 6) {
            validationStatusLabel.setText("Código inválido.");
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
            painelCodigo.setVisible(false);
            painelReset.setVisible(true);
        } else {
            validationStatusLabel.setText("Código incorreto ou expirado.");
            validationStatusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleRedefinirSenha() {
        String nova = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (nova.isEmpty() || confirm.isEmpty()) {
            resetStatusLabel.setText("Preencha os campos.");
            return;
        }

        if (!nova.equals(confirm)) {
            resetStatusLabel.setText("Senhas não coincidem.");
            return;
        }

        if (!isPasswordStrong(nova)) {
            resetStatusLabel.setText("Senha fraca.");
            return;
        }

        String hashed = BCrypt.hashpw(nova, BCrypt.gensalt());
        if (userRepository.atualizarSenha(SessaoAtual.utilizadorRecuperacao, hashed)) {
            showAlert("Senha redefinida com sucesso!");
            SessaoAtual.utilizadorRecuperacao = 0;
            SessaoAtual.emailRecuperacao = null;
            GoToLogin();
        } else {
            resetStatusLabel.setText("Erro ao atualizar senha.");
        }
    }
    @FXML
    private void handleEmailDigitado() {
        String email = forgotEmailField.getText().trim();

        btnEnviarCodigo.setDisable(!email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$"));
    }



    // ================= UTILS =================
    private boolean isPasswordStrong(String password) {
        return password.length() >= 10
                && password.matches(".*[A-Z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/\\\\|].*");
    }

    private void showAlert(String msg) {
        showAlert(msg, AlertType.INFORMATION);
    }

    private void showAlert(String msg, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }


    @FXML
    private void GoToRegister() {
        goTo("/view/register.fxml", false);
    }

    @FXML
    private void GoToRecuperacao() {
        goTo("/view/recover_account.fxml", false);
    }
    @FXML
    private void GoToLogin() {
        goTo("/view/login.fxml", false);
    }


}
