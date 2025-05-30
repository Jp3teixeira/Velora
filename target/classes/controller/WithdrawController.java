package controller;

import Repository.WalletRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utils.SessaoAtual;

import java.math.BigDecimal;

public class WithdrawController {

    @FXML private TextField amountField;
    @FXML private Label statusLabel;

    private int userId;
    private WalletController mainController;

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setMainController(WalletController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void handleWithdraw() {
        try {
            String input = amountField.getText().trim();
            BigDecimal amount = new BigDecimal(input);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                statusLabel.setText("Valor deve ser positivo!");
                return;
            }

            WalletRepository repo = WalletRepository.getInstance();

            BigDecimal saldoAtual = repo.getSaldo(userId);
            if (saldoAtual.compareTo(amount) < 0) {
                statusLabel.setText("Saldo insuficiente!");
                return;
            }

            boolean success = repo.withdraw(userId, amount);
            if (success) {
                SessaoAtual.saldoCarteira = repo.getSaldo(userId);
                statusLabel.setText("Retirada efetuada com sucesso!");

                if (mainController != null) mainController.atualizarSaldo();

                fecharJanelaAposDelay();
            } else {
                statusLabel.setText("Erro ao processar retirada.");
            }

        } catch (NumberFormatException e) {
            statusLabel.setText("Valor inválido. Ex: 100.50");
        } catch (Exception e) {
            statusLabel.setText("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void fecharJanelaAposDelay() {
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> {
                    Stage stage = (Stage) amountField.getScene().getWindow();
                    stage.close();
                });
            }
        }, 1000);
    }
}
