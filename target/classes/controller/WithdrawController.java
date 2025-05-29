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

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @FXML
    public void handleWithdraw() {
        try {
            BigDecimal amount = new BigDecimal(amountField.getText());

            // Validação de valor positivo
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                statusLabel.setText("Valor deve ser positivo!");
                return;
            }

            // Verifica saldo suficiente
            if (SessaoAtual.saldoCarteira.compareTo(amount) < 0) {
                statusLabel.setText("Saldo insuficiente!");
                return;
            }

            WalletRepository walletRepo = new WalletRepository();
            boolean success = walletRepo.withdraw(userId, amount);

            if (success) {
                // Atualiza a sessão
                SessaoAtual.saldoCarteira = SessaoAtual.saldoCarteira.subtract(amount);
                statusLabel.setText("Retirada efetuada com sucesso!");

                // Fecha a janela após 1 segundo
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