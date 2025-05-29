package controller;

import Repository.WalletRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utils.SessaoAtual;

import java.math.BigDecimal;

public class WithdrawController {

    @FXML
    private TextField amountField;

    @FXML
    private Label statusLabel;

    private int userId;
    private UserManagementController mainController;

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setMainController(UserManagementController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void handleWithdraw() {
        try {
            BigDecimal amount = new BigDecimal(amountField.getText());

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                statusLabel.setText("Valor deve ser positivo!");
                return;
            }

            // Buscar saldo ATUALIZADO do banco
            BigDecimal saldoAtual = WalletRepository.getSaldo(userId);
            if (saldoAtual.compareTo(amount) < 0) {
                statusLabel.setText("Saldo insuficiente!");
                return;
            }

            WalletRepository walletRepo = new WalletRepository();
            boolean success = walletRepo.withdraw(userId, amount);

            if (success) {
                statusLabel.setText("Retirada efetuada com sucesso!");

                // Atualizar a sessão com o novo saldo
                SessaoAtual.saldoCarteira = WalletRepository.getSaldo(userId);

                // Notificar o controlador principal para atualizar a interface
                if (mainController != null) {
                    mainController.atualizarSaldo();
                }

                // Fechar janela após 1 segundo
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                javafx.application.Platform.runLater(() -> {
                                    Stage stage = (Stage) amountField.getScene().getWindow();
                                    stage.close();
                                });
                            }
                        },
                        1000
                );
            } else {
                statusLabel.setText("Erro ao processar retirada.");
            }

        } catch (NumberFormatException e) {
            statusLabel.setText("Formato inválido! Use números ex: 100.50");
        } catch (Exception e) {
            statusLabel.setText("Erro inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }
}