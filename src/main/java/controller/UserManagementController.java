// UserManagementController.java
package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;
import Repository.UserRepository;
import Repository.WalletRepository;
import utils.EmailSender;
import utils.NavigationHelper;
import utils.NotificationUtil;
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

    private void showNotification(String msg, boolean isSuccess) {
        Stage stage;
        if (loginEmailField != null && loginEmailField.getScene() != null) {
            stage = (Stage) loginEmailField.getScene().getWindow();
        } else if (registerEmailField != null && registerEmailField.getScene() != null) {
            stage = (Stage) registerEmailField.getScene().getWindow();
        } else if (forgotEmailField != null && forgotEmailField.getScene() != null) {
            stage = (Stage) forgotEmailField.getScene().getWindow();
        } else {
            return;
        }
        NotificationUtil.show(msg, stage, isSuccess);
    }

    // ================= LOGIN =================
    @FXML
    private void handleLogin() {
        String input = loginEmailField.getText().trim();
        String password = loginPasswordField.getText();

        if (input.isEmpty() || password.isEmpty()) {
            showNotification("Por favor, preencha todos os campos.", false);
            return;
        }

        var userOpt = userRepository.findUserByEmailOrUsername(input);
        if (userOpt.isEmpty()) {
            showNotification("Utilizador não encontrado.", false);
            return;
        }

        var user = userOpt.get();
        int id = Integer.parseInt(user.get("id_utilizador"));
        String hashed = user.get("password");

        if (!userRepository.isContaVerificada(id)) {
            showNotification("A conta ainda não foi verificada.", false);
            return;
        }

        if (!BCrypt.checkpw(password, hashed)) {
            showNotification("Password incorreta.", false);
            return;
        }

        SessaoAtual.utilizadorId    = id;
        SessaoAtual.nome            = user.get("nome");
        SessaoAtual.email           = user.get("email");
        SessaoAtual.tipo            = user.get("tipoPerfil");
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

        if (username.isEmpty() || email.isEmpty() || pwd.isEmpty() || pwd2.isEmpty()) {
            showNotification("Todos os campos são obrigatórios.", false);
            return;
        }
        if (!pwd.equals(pwd2)) {
            showNotification("As passwords não coincidem.", false);
            return;
        }
        if (!isPasswordStrong(pwd)) {
            showNotification("""
                A password deve ter pelo menos:
                - 10 caracteres
                - 1 maiúscula
                - 1 número
                - 1 especial.
                """, false);
            return;
        }

        if (userRepository.existsByEmail(email)) {
            showNotification("Esse e-mail já foi registado.", false);
            return;
        }
        if (userRepository.existsByUsername(username)) {
            showNotification("Esse nome de utilizador já foi registado.", false);
            return;
        }

        String hashed = BCrypt.hashpw(pwd, BCrypt.gensalt());
        Optional<Integer> userIdOpt = userRepository.registarNovoUtilizador(username, email, hashed);

        if (userIdOpt.isEmpty()) {
            showNotification("Erro ao criar conta: e-mail ou username já registado.", false);
            return;
        }

        int userId = userIdOpt.get();
        SessaoAtual.utilizadorId = userId;

        if (!WalletRepository.getInstance().createWalletForUser(userId)) {
            showNotification("Conta criada, mas erro ao criar carteira.", false);
            return;
        }

        String codigo = String.format("%06d", new Random().nextInt(999_999));
        boolean ok = userRepository.inserirCodigoVerificacao(
                userId,
                codigo,
                LocalDateTime.now().plusDays(1),
                "REGISTO"
        ) && EmailSender.sendVerificationCode(email, codigo);

        if (ok) {
            showNotification("Verifique o seu e-mail para ativar a conta.", true);
            goTo("/view/verification.fxml", false);
        } else {
            showNotification("Erro ao enviar código.", false);
        }
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

    private boolean isPasswordStrong(String password) {
        return password.length() >= 10
                && password.matches(".*[A-Z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/\\\\|].*");
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

    public void desativarUtilizadorPorId(int id) {
        boolean sucesso = userRepository.desativarUtilizador(id);
        showNotification(sucesso ? "Utilizador desativado com sucesso." : "Erro ao desativar utilizador.", sucesso);
    }

    public void ativarUtilizador(int id) {
        boolean sucesso = userRepository.ativarUtilizador(id);
        showNotification(sucesso ? "Utilizador ativado com sucesso." : "Erro ao ativar utilizador.", sucesso);
    }

    public void editarUtilizadorSemPassword(int id, String nome, String email, int idPerfil) {
        boolean sucesso = userRepository.atualizarUtilizadorSemPassword(id, nome, email, idPerfil);
        showNotification(sucesso ? "Dados atualizados com sucesso." : "Erro ao atualizar utilizador.", sucesso);
    }

    public void atribuirPerfilAdminSeValido(int id) {
        if (userRepository.podeSerAdmin(id)) {
            Optional<Integer> perfilAdmin = userRepository.getPerfilId("admin");
            if (perfilAdmin.isPresent()) {
                boolean sucesso = userRepository.atualizarUtilizadorSemPassword(id, "Admin", "admin@exemplo.com", perfilAdmin.get());
                showNotification(sucesso ? "Utilizador promovido a admin com sucesso." : "Erro ao promover utilizador.", sucesso);
            } else {
                showNotification("Perfil 'admin' não encontrado.", false);
            }
        } else {
            showNotification("O utilizador tem carteira com saldo ou moedas e não pode ser admin.", false);
        }
    }
}
