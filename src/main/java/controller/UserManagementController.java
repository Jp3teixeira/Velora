// src/controller/UserManagementController.java
package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;
import Repository.UserRepository;
import Repository.WalletRepository;
import model.Utilizador;
import model.Carteira;
import utils.EmailSender;
import utils.NavigationHelper;
import utils.NotificationUtil;
import utils.Routes;
import utils.SessaoAtual;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;



import static utils.NavigationHelper.goTo;

public class UserManagementController {

    private final UserRepository     userRepository = new UserRepository();
    private final WalletRepository   walletRepo     = WalletRepository.getInstance();

    // ===== FXML fields =====
    @FXML private TextField loginEmailField;
    @FXML private PasswordField loginPasswordField;

    @FXML private TextField registerUsernameField;
    @FXML private TextField registerEmailField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField registerConfirmPasswordField;

    @FXML private TextField codigoField;

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

    // ===== Helpers =====
    private void showNotification(String msg, boolean isSuccess) {
        Stage stage = null;
        if (loginEmailField != null && loginEmailField.getScene() != null) {
            stage = (Stage) loginEmailField.getScene().getWindow();
        } else if (registerEmailField != null && registerEmailField.getScene() != null) {
            stage = (Stage) registerEmailField.getScene().getWindow();
        } else if (forgotEmailField != null && forgotEmailField.getScene() != null) {
            stage = (Stage) forgotEmailField.getScene().getWindow();
        }
        if (stage != null) {
            NotificationUtil.show(msg, stage, isSuccess);
        }
    }

    private void mostrarPainel(VBox painelAtivo) {
        painelEmail.setVisible(false);
        painelEmail.setManaged(false);
        painelCodigo.setVisible(false);
        painelCodigo.setManaged(false);
        painelReset.setVisible(false);
        painelReset.setManaged(false);

        painelAtivo.setVisible(true);
        painelAtivo.setManaged(true);
    }

    private boolean isPasswordStrong(String pwd) {
        return pwd.length() >= 10
                && pwd.matches(".*[A-Z].*")
                && pwd.matches(".*\\d.*")
                && pwd.matches(".*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/\\\\|].*");
    }

    // ===== LOGIN =====
    @FXML
    private void handleLogin() {
        String input = loginEmailField.getText().trim();
        String password = loginPasswordField.getText();

        if (input.isEmpty() || password.isEmpty()) {
            showNotification("Por favor, preencha todos os campos.", false);
            return;
        }

        Optional<Utilizador> userOpt = userRepository.findByEmailOrUsername(input);
        if (userOpt.isEmpty()) {
            showNotification("Utilizador não encontrado.", false);
            return;
        }

        Utilizador user = userOpt.get();
        if (!user.isAtivo()) {
            showNotification("Conta inativa ou não verificada.", false);
            return;
        }

        if (!BCrypt.checkpw(password, user.getHashPwd())) {
            showNotification("Password incorreta.", false);
            return;
        }

        SessaoAtual.utilizadorId    = user.getId();
        SessaoAtual.nome            = user.getNome();
        SessaoAtual.email           = user.getEmail();
        SessaoAtual.tipo            = user.getPerfil().name();
        SessaoAtual.saldoCarteira   = walletRepo.getSaldoPorUtilizador(user.getId());

        NavigationHelper.goTo(Routes.HOMEPAGE, true);
    }

    // ===== REGISTO =====
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
            showNotification(
                    "A password deve ter pelo menos 10 caracteres, 1 maiúscula, 1 número e 1 especial.",
                    false
            );
            return;
        }

        if (userRepository.existsByEmail(email)) {
            showNotification("E-mail já registado.", false);
            return;
        }
        if (userRepository.existsByUsername(username)) {
            showNotification("Username já registado.", false);
            return;
        }

        Utilizador u = new Utilizador();
        u.setNome(username);
        u.setEmail(email);
        u.setHashPwd(BCrypt.hashpw(pwd, BCrypt.gensalt()));

        if (!userRepository.save(u)) {
            showNotification("Erro ao criar conta.", false);
            return;
        }

        // criar carteira
        Carteira c = new Carteira();
        c.setUtilizador(u);
        c.setSaldoEur(BigDecimal.ZERO);
        if (!walletRepo.save(c)) {
            showNotification("Conta criada, mas falha ao criar carteira.", false);
            return;
        }

        // enviar código de verificação
        String codigo = String.format("%06d", new Random().nextInt(999_999));
        boolean ok = userRepository.inserirCodigoVerificacao(
                u.getId(), codigo, LocalDateTime.now().plusDays(1), "REGISTO"
        ) && EmailSender.sendVerificationCode(email, codigo);

        if (ok) {
            showNotification("Verifique o seu e-mail para ativar a conta.", true);
            goTo(Routes.VERIFICATION, false);
        } else {
            showNotification("Erro ao enviar código de verificação.", false);
        }
    }

    @FXML private void GoToRegister()    { goTo(Routes.REGISTER, false); }
    @FXML private void GoToRecuperacao() { goTo(Routes.RECOVER, false); }
    @FXML private void GoToLogin()       { goTo(Routes.LOGIN, false); }

    // ===== RECUPERAÇÃO DE SENHA =====
    @FXML
    private void handleEmailDigitado() {
        btnEnviarCodigo.setDisable(forgotEmailField.getText().trim().isEmpty());
    }

    @FXML
    private void handleEnviarLink(ActionEvent event) {
        String email = forgotEmailField.getText().trim();
        if (email.isEmpty()) {
            statusLabel.setText("Insira o seu e-mail.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        Optional<Utilizador> userOpt = userRepository.findByEmailOrUsername(email);
        if (userOpt.isEmpty()) {
            statusLabel.setText("E-mail não registado.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        Utilizador u = userOpt.get();
        String codigo = String.format("%06d", new Random().nextInt(999_999));
        boolean okDb   = userRepository.inserirCodigoVerificacao(
                u.getId(), codigo, LocalDateTime.now().plusHours(1), "RECUPERACAO"
        );
        boolean okMail = EmailSender.sendRecoveryCode(email, codigo);

        if (okDb && okMail) {
            statusLabel.setText("Código enviado para o e-mail.");
            statusLabel.setStyle("-fx-text-fill: green;");
            mostrarPainel(painelCodigo);
        } else {
            statusLabel.setText("Falha ao enviar código.");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleValidarCodigoRecuperacao(ActionEvent event) {
        String email = forgotEmailField.getText().trim();
        String codigo = validationCodeField.getText().trim();
        if (codigo.isEmpty()) {
            validationStatusLabel.setText("Insira o código.");
            validationStatusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // extrai o Utilizador
        Optional<Utilizador> userOpt = userRepository.findByEmailOrUsername(email);
        if (userOpt.isEmpty()) {
            validationStatusLabel.setText("E-mail não registado.");
            validationStatusLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        Utilizador user = userOpt.get();
        int userId = user.getId();    // <— aqui

        // chama o método correto
        boolean valido = userRepository.validarCodigo(userId, codigo);
        if (valido) {
            validationStatusLabel.setText("Código válido!");
            validationStatusLabel.setStyle("-fx-text-fill: green;");
            mostrarPainel(painelReset);
        } else {
            validationStatusLabel.setText("Código inválido ou expirado.");
            validationStatusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleRedefinirSenha(ActionEvent event) {
        String nova = newPasswordField.getText();
        String conf = confirmPasswordField.getText();

        Optional<Utilizador> userOpt = userRepository.findByEmailOrUsername(forgotEmailField.getText().trim());
        Utilizador user = userOpt.get();
        int userId = user.getId();
        String hashed = BCrypt.hashpw(nova, BCrypt.gensalt());
        // chama o método novo
        boolean atualizado = userRepository.atualizarSenha(userId, hashed);
        if (atualizado) {
            resetStatusLabel.setText("Senha redefinida com sucesso!");
            resetStatusLabel.setStyle("-fx-text-fill: green;");
            goTo(Routes.LOGIN, false);
        } else {
            resetStatusLabel.setText("Erro ao redefinir senha.");
            resetStatusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // ===== ADMIN USER MANAGEMENT HELPERS =====
    public void desativarUtilizadorPorId(int id) {
        boolean ok = userRepository.delete(id);
        showNotification(ok ? "Utilizador desativado." : "Falha ao desativar.", ok);
    }

    public void ativarUtilizador(int id) {
        boolean ok = userRepository.ativarUtilizador(id);
        showNotification(ok ? "Utilizador ativado." : "Falha ao ativar.", ok);
    }

    public void editarUtilizadorSemPassword(int id, String nome, String email, int idPerfil) {
        Utilizador u = new Utilizador();
        u.setId(id);
        u.setNome(nome);
        u.setEmail(email);
        u.setIdPerfil(idPerfil);
        boolean ok = userRepository.update(u);
        showNotification(ok ? "Dados atualizados." : "Falha na atualização.", ok);
    }

    public void atribuirPerfilAdminSeValido(int id) {
        if (userRepository.podeSerAdmin(id)) {
            userRepository.getPerfilId("ADMIN").ifPresent(perfilId ->
                    editarUtilizadorSemPassword(id, "Admin", "admin@exemplo.com", perfilId)
            );
        } else {
            showNotification("Não é possível promover este utilizador.", false);
        }
    }
    // Chamado ao clicar em “Verificar” na tela de verificação de registo
    @FXML
    private void handleVerify(ActionEvent event) {
        String code = codigoField.getText().trim();
        if (code.isEmpty()) {
            NotificationUtil.show("Por favor, insira o código.",
                    (Stage) codigoField.getScene().getWindow(), false);
            return;
        }

        int userId = SessaoAtual.utilizadorId;
        boolean ok = userRepository.validarCodigo(userId, code);
        if (!ok) {
            NotificationUtil.show("Código inválido ou expirado.",
                    (Stage) codigoField.getScene().getWindow(), false);
            return;
        }

        // marca conta como ativa
        if (userRepository.ativarUtilizador(userId)) {
            NotificationUtil.show("Conta verificada com sucesso!",
                    (Stage) codigoField.getScene().getWindow(), true);
            NavigationHelper.goTo(Routes.LOGIN, false);
        } else {
            NotificationUtil.show("Não foi possível ativar a conta.",
                    (Stage) codigoField.getScene().getWindow(), false);
        }
    }
}
