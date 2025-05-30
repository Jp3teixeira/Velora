package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import Repository.WalletRepository;
import utils.SessaoAtual;
import java.io.IOException;
import java.math.BigDecimal;

public class WalletController {

    @FXML private Label balanceLabel;
    @FXML private Button depositButton;
    @FXML private Button withdrawButton;

    @FXML private TableView<?> cryptoTable;
    @FXML private TableView<?> transactionTable;

    // ================= INICIALIZAÇÃO =================
    @FXML
    public void initialize() {
        atualizarSaldo();

        if (balanceLabel != null) {
            balanceLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.windowProperty().addListener((obs2, oldWindow, newWindow) -> {
                        if (newWindow != null && newWindow.isShowing()) {
                            atualizarSaldo();
                        }
                    });
                }
            });
        }
    }

    public void atualizarSaldo() {
        try {
            BigDecimal novoSaldo = WalletRepository.getInstance().getSaldo(SessaoAtual.utilizadorId);
            SessaoAtual.saldoCarteira = novoSaldo;
            balanceLabel.setText("€ " + novoSaldo);
        } catch (Exception e) {
            System.err.println("Erro ao atualizar saldo: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erro ao atualizar saldo", AlertType.ERROR);
        }
    }

    // ================= POP-UPS =================
    @FXML
    public void abrirTelaDeposito() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/deposit.fxml"));
            Parent root = loader.load();

            DepositController controller = loader.getController();
            controller.setUserId(SessaoAtual.utilizadorId);
            controller.setMainController(this);

            Stage stage = new Stage();
            stage.setTitle("Depósito");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erro ao abrir tela de depósito: " + e.getMessage(), AlertType.ERROR);
        }
    }

    @FXML
    public void abrirTelaRetirada() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/withdraw.fxml"));
            Parent root = loader.load();

            WithdrawController controller = loader.getController();
            controller.setUserId(SessaoAtual.utilizadorId);
            controller.setMainController(this);

            Stage stage = new Stage();
            stage.setTitle("Retirada");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erro ao abrir tela de retirada: " + e.getMessage(), AlertType.ERROR);
        }
    }

    // ================= UTILS =================
    private void showAlert(String mensagem, AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
