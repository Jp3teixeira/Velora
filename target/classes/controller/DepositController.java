package controller;

import Repository.WalletRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utils.SessaoAtual;

import java.math.BigDecimal;

public class DepositController {

    @FXML
    private TextField amountField;

    @FXML
    private Label statusLabel;

    private int userId; // será definido pelo controlador principal

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @FXML
    public void handleDeposit() {
        try {
            BigDecimal amount = new BigDecimal(amountField.getText());

            // Validação adicional de valor positivo
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                statusLabel.setText("Valor deve ser positivo!");
                return;
            }

            WalletRepository walletRepo = new WalletRepository();
            boolean success = walletRepo.deposit(userId, amount);

            if (success) {
                // ATUALIZA A SESSÃO
                SessaoAtual.saldoCarteira = SessaoAtual.saldoCarteira.add(amount);
                statusLabel.setText("Depósito efetuado com sucesso!");

                // FECHA A JANELA APÓS 1 SEGUNDO
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
                        1000 // 1 segundo
                );
            } else {
                statusLabel.setText("Erro ao depositar. Tente novamente.");
            }

        } catch (NumberFormatException e) {
            statusLabel.setText("Formato inválido! Use números ex: 100.50");
        } catch (Exception e) {
            statusLabel.setText("Erro inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
