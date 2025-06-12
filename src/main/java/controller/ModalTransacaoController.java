package controller;

import Repository.WalletRepository;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.SessaoAtual;

import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;

public class ModalTransacaoController {

    // PARA DEPÓSITO
    @FXML private ToggleGroup metodoGroup;
    @FXML private TextField depositAmountField;
    @FXML private Label depositStatusLabel;

    // PARA LEVANTAMENTO
    @FXML private ComboBox<String> contaDestinoCombo;
    @FXML private TextField withdrawAmountField;
    @FXML private Label withdrawStatusLabel;

    private int userId;
    private WalletController mainController;
    private String modoInicial; // "depositar" ou "levantar"

    public void setModoInicial(String modo) {
        this.modoInicial = modo;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setMainController(WalletController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        // Se o FXML carregado for withdraw_modal.fxml, povoamos o ComboBox
        if ("levantar".equalsIgnoreCase(modoInicial)) {
            contaDestinoCombo.getItems().addAll(
                    "Conta Principal (€) - Banco A",
                    "Conta Poupança (€) - Banco B"
            );
            contaDestinoCombo.getSelectionModel().selectFirst();
        }

    }

    // ================= MÉTODO PARA FECHAR A JANELA =================
    @FXML
    private void fecharJanela(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    // ================= DEPÓSITO =================
    @FXML
    public void confirmarDeposito() {
        try {
            String input = depositAmountField.getText().trim();
            BigDecimal amount = new BigDecimal(input);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                depositStatusLabel.setText("Valor deve ser positivo!");
                return;
            }

            WalletRepository repo = WalletRepository.getInstance();
            boolean success = repo.deposit(userId, amount);
            if (success) {
                SessaoAtual.saldoCarteira = repo.getSaldo(userId);
                depositStatusLabel.setText("Depósito efetuado com sucesso!");
                atualizarWalletPrincipal();
                fecharJanelaAposDelay();
            } else {
                depositStatusLabel.setText("Erro ao processar depósito.");
            }

        } catch (NumberFormatException e) {
            depositStatusLabel.setText("Valor inválido. Ex: 100.00");
        } catch (Exception e) {
            depositStatusLabel.setText("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= LEVANTAMENTO =================
    @FXML
    public void confirmarLevantamento() {
        try {
            String input = withdrawAmountField.getText().trim();
            BigDecimal amount = new BigDecimal(input);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                withdrawStatusLabel.setText("Valor deve ser positivo!");
                return;
            }

            WalletRepository repo = WalletRepository.getInstance();
            BigDecimal saldoAtual = repo.getSaldo(userId);
            if (saldoAtual.compareTo(amount) < 0) {
                withdrawStatusLabel.setText("Saldo insuficiente!");
                return;
            }

            boolean success = repo.withdraw(userId, amount);
            if (success) {
                SessaoAtual.saldoCarteira = repo.getSaldo(userId);
                withdrawStatusLabel.setText("Retirada efetuada com sucesso!");
                atualizarWalletPrincipal();
                fecharJanelaAposDelay();
            } else {
                withdrawStatusLabel.setText("Erro ao processar retirada.");
            }

        } catch (NumberFormatException e) {
            withdrawStatusLabel.setText("Valor inválido. Ex: 50.00");
        } catch (Exception e) {
            withdrawStatusLabel.setText("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void atualizarWalletPrincipal() {
        if (mainController != null) {
            mainController.atualizarSaldo();
            mainController.atualizarTotalPortfolio();
            mainController.carregarPortfolio();
            mainController.carregarHistoricoTransacoes();
        }
    }

    private void fecharJanelaAposDelay() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    Stage stage;
                    if (depositAmountField != null && depositAmountField.getScene() != null) {
                        stage = (Stage) depositAmountField.getScene().getWindow();
                    } else {
                        stage = (Stage) withdrawAmountField.getScene().getWindow();
                    }
                    stage.close();
                });
            }
        }, 1000);
    }
}
