// UserManagementController.java
package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.mindrot.jbcrypt.BCrypt;
import Repository.UserRepository;
import Repository.WalletRepository;
import utils.EmailSender;
import utils.NavigationHelper;
import utils.Routes;
import utils.SessaoAtual;

import java.time.LocalDateTime;
import java.util.Optional;
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

    // Verificação (registro)
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
            showAlert("Por favor, preencha todos os campos.", Alert.AlertType.ERROR);
            return;
        }

        var userOpt = userRepository.findUserByEmailOrUsername(input);
        if (userOpt.isEmpty()) {
            showAlert("Utilizador não encontrado.", Alert.AlertType.ERROR);
            return;
        }

        var user = userOpt.get();
        int id = Integer.parseInt(user.get("id_utilizador"));
        String hashed = user.get("password");

        if (!userRepository.isContaVerificada(id)) {
            showAlert("A conta ainda não foi verificada.", Alert.AlertType.WARNING);
            return;
        }

        if (!BCrypt.checkpw(password, hashed)) {
            showAlert("Password incorreta.", Alert.AlertType.ERROR);
            return;
        }

        SessaoAtual.utilizadorId    = id;
        SessaoAtual.nome            = user.get("nome");
        SessaoAtual.email           = user.get("email");
        SessaoAtual.tipo = user.get("tipoPerfil");  // "user" ou "admin"
        SessaoAtual.saldoCarteira   = WalletRepository.getInstance().getSaldo(id);


        NavigationHelper.goTo(Routes.HOMEPAGE, true);
    }

    // ================= REGISTO =================
    @FXML
    private void handleRegister(ActionEvent event) {
        String username = registerUsernameField.getText().trim();
        String email    = registerEmailField.getText().trim();
        String pwd      = registerPasswordField.getText();
        String pwd2     = registerConfirmPasswordField.getText();

        // 1) Validação básica de campos
        if (username.isEmpty() || email.isEmpty() || pwd.isEmpty() || pwd2.isEmpty()) {
            showAlert("Todos os campos são obrigatórios.", Alert.AlertType.ERROR);
            return;
        }
        if (!pwd.equals(pwd2)) {
            showAlert("As passwords não coincidem.", Alert.AlertType.ERROR);
            return;
        }
        if (!isPasswordStrong(pwd)) {
            showAlert("""
                A password deve ter pelo menos:
                - 10 caracteres
                - 1 maiúscula
                - 1 número
                - 1 especial.
                """, Alert.AlertType.ERROR);
            return;
        }


        // 2) Verificar duplicados antes de inserir
        if (userRepository.existsByEmail(email)) {
            showAlert("Esse e-mail já foi registado.", Alert.AlertType.ERROR);
            return;
        }
        if (userRepository.existsByUsername(username)) {
            showAlert("Esse nome de utilizador já foi registado.", Alert.AlertType.ERROR);
            return;
        }

        // 3) Efetuar o INSERT
        String hashed = BCrypt.hashpw(pwd, BCrypt.gensalt());
        Optional<Integer> userIdOpt = userRepository.registarNovoUtilizador(username, email, hashed);

        if (userIdOpt.isEmpty()) {
            // Se retornou Optional.empty(), pode ter sido duplicado detectado no INSERT
            showAlert("Erro ao criar conta: e-mail ou username já registado.", Alert.AlertType.ERROR);
            return;
        }

        int userId = userIdOpt.get();
        SessaoAtual.utilizadorId = userId;

        // 4) Criar carteira para o utilizador
        if (!WalletRepository.getInstance().createWalletForUser(userId)) {
            showAlert("Conta criada, mas erro ao criar carteira.", Alert.AlertType.ERROR);
            return;
        }

        // 5) Enviar código de verificação
        String codigo = String.format("%06d", new Random().nextInt(999_999));
        boolean ok = userRepository.inserirCodigoVerificacao(
                userId,
                codigo,
                LocalDateTime.now().plusDays(1),
                "REGISTO"
        ) && EmailSender.sendVerificationCode(email, codigo);

        if (ok) {
            showAlert("Verifique o seu e-mail para ativar a conta.", Alert.AlertType.INFORMATION);
            goTo("/view/verification.fxml", false);
        } else {
            showAlert("Erro ao enviar código.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleVerify(ActionEvent event) {
        String codigo = codigoField.getText().trim();
        if (codigo.isEmpty()) {
            showAlert("Insira o código.", Alert.AlertType.ERROR);
            return;
        }

        boolean valid = userRepository.validarCodigo(
                SessaoAtual.utilizadorId,
                "REGISTO",
                codigo
        );
        if (valid) {
            goTo(Routes.HOMEPAGE, true);
        } else {
            showAlert("Código incorreto ou expirado.", Alert.AlertType.ERROR);
        }
    }

    // ================= RECUPERAÇÃO DE SENHA =================
    @FXML
    private void handleEnviarLink() {
        String email = forgotEmailField.getText().trim();
        if (email.isEmpty()) {
            showAlert("Insira o e-mail.", Alert.AlertType.WARNING);
            return;
        }

        var userIdOpt = userRepository.getUserIdByEmail(email);
        if (userIdOpt.isEmpty()) {
            showAlert("E-mail não encontrado.", Alert.AlertType.ERROR);
            return;
        }

        int userId = userIdOpt.get();
        SessaoAtual.emailRecuperacao = email;

        String codigo = String.format("%06d", new Random().nextInt(999_999));
        boolean ok = userRepository.inserirCodigoVerificacao(
                userId,
                codigo,
                LocalDateTime.now().plusHours(1),
                "RECUPERACAO_SENHA"
        ) && EmailSender.sendRecoveryCode(email, codigo);

        if (ok) {
            statusLabel.setText("Código enviado para: " + email);
            statusLabel.setStyle("-fx-text-fill: green;");
            mostrarPainel(painelCodigo);
        } else {
            showAlert("Erro ao enviar código.", Alert.AlertType.ERROR);
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
        boolean valid = userRepository.validarCodigo(
                userId,
                "RECUPERACAO_SENHA",
                codigo
        );
        if (valid) {
            SessaoAtual.utilizadorRecuperacao = userId;
            mostrarPainel(painelReset);
        } else {
            validationStatusLabel.setText("Código incorreto ou expirado.");
            validationStatusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleRedefinirSenha() {
        String nova    = newPasswordField.getText();
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
        boolean ok = userRepository.atualizarSenha(
                SessaoAtual.utilizadorRecuperacao,
                hashed
        );
        if (ok) {
            showAlert("Senha redefinida com sucesso!", Alert.AlertType.INFORMATION);
            SessaoAtual.utilizadorRecuperacao = 0;
            SessaoAtual.emailRecuperacao = null;
            goTo("/view/login.fxml", false);
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

    private void showAlert(String msg, Alert.AlertType type) {
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

    private void mostrarPainel(VBox ativo) {
        painelEmail.setVisible(false);
        painelEmail.setManaged(false);
        painelCodigo.setVisible(false);
        painelCodigo.setManaged(false);
        painelReset.setVisible(false);
        painelReset.setManaged(false);

        ativo.setVisible(true);
        ativo.setManaged(true);
    }
}
