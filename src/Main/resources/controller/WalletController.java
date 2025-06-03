package controller;

import Repository.PortfolioRepository;
import Repository.WalletRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import model.Portfolio;
import utils.SessaoAtual;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class WalletController {

    @FXML private Label balanceLabel;
    @FXML private Button depositButton;
    @FXML private Button withdrawButton;

    // TableView para os “MEUS ATIVOS”
    @FXML private TableView<Portfolio> cryptoTable;
    @FXML private TableColumn<Portfolio, String> colAtivo;
    @FXML private TableColumn<Portfolio, String> colTicker;
    @FXML private TableColumn<Portfolio, String> colQuantidade;
    @FXML private TableColumn<Portfolio, String> colValor;

    private final WalletRepository       walletRepo    = WalletRepository.getInstance();
    private final PortfolioRepository     portfolioRepo = new PortfolioRepository();

    @FXML
    public void initialize() {
        // 1) Atualiza saldo EUR
        atualizarSaldo();

        // 2) Configura as colunas da cryptoTable
        configurarTabelaPortfolio();

        // 3) Carrega os dados do portfólio na tabela
        carregarPortfolio();

        // 4) Toda vez que a janela for exibida ou ganhar foco, recarregar:
        if (balanceLabel != null) {
            balanceLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.windowProperty().addListener((obs2, oldWindow, newWindow) -> {
                        if (newWindow != null && newWindow.isShowing()) {
                            atualizarSaldo();
                            carregarPortfolio();
                        }
                    });
                }
            });
        }
    }

    private void configurarTabelaPortfolio() {
        colAtivo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getMoeda().getNome()));

        colTicker.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getMoeda().getSimbolo()));

        colQuantidade.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getQuantidade()
                                .setScale(8, RoundingMode.HALF_UP)
                                .toPlainString()
                ));

        colValor.setCellValueFactory(cell -> {
            BigDecimal qtd   = cell.getValue().getQuantidade();
            BigDecimal preco = cell.getValue().getMoeda().getValorAtual();
            BigDecimal total = qtd.multiply(preco).setScale(2, RoundingMode.HALF_UP);
            return new SimpleStringProperty(total.toPlainString());
        });
    }

    private void carregarPortfolio() {
        int userId = SessaoAtual.utilizadorId;
        List<Portfolio> lista = portfolioRepo.listarPorUtilizador(userId);
        cryptoTable.setItems(FXCollections.observableArrayList(lista));
    }

    public void atualizarSaldo() {
        try {
            BigDecimal novoSaldo = walletRepo.getSaldo(SessaoAtual.utilizadorId);
            SessaoAtual.saldoCarteira = novoSaldo;
            String textoFormatado = String.format("€ %.2f", novoSaldo);
            balanceLabel.setText(textoFormatado);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erro ao atualizar saldo", Alert.AlertType.ERROR);
        }
    }



    // ================= POP-UPS =================

    @FXML
    public void abrirTelaDeposito() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/depositar.fxml"));
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
            showAlert("Erro ao abrir tela de depósito: " + e.getMessage(), Alert.AlertType.ERROR);
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
            showAlert("Erro ao abrir tela de retirada: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ================= AUXILIAR =================

    private void showAlert(String mensagem, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }


}
