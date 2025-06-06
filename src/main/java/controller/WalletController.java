package controller;

import Repository.PortfolioRepository;
import Repository.TransacaoRepository;
import Repository.WalletRepository;
import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.Portfolio;
import model.Transacao;
import utils.SessaoAtual;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class WalletController {

    @FXML private Label balanceLabel;
    @FXML private Label totalLabel;
    @FXML private Button depositButton;
    @FXML private Button withdrawButton;

    // “MEUS ATIVOS”
    @FXML private JFXTreeTableView<Portfolio> cryptoTable;
    @FXML private JFXTreeTableColumn<Portfolio, String> colAtivo;
    @FXML private JFXTreeTableColumn<Portfolio, String> colTicker;
    @FXML private JFXTreeTableColumn<Portfolio, String> colQuantidade;
    @FXML private JFXTreeTableColumn<Portfolio, String> colPrecoMedio;
    @FXML private JFXTreeTableColumn<Portfolio, String> colValor;

    // “HISTÓRICO DE TRANSAÇÕES”
    @FXML private JFXTreeTableView<Transacao> transactionTable;
    @FXML private JFXTreeTableColumn<Transacao, String> colData;
    @FXML private JFXTreeTableColumn<Transacao, String> colTipo;
    @FXML private JFXTreeTableColumn<Transacao, String> colAtivoTx;
    @FXML private JFXTreeTableColumn<Transacao, String> colQuantidadeTx;
    @FXML private JFXTreeTableColumn<Transacao, String> colValorTx;

    private final WalletRepository walletRepo = WalletRepository.getInstance();
    private final PortfolioRepository portfolioRepo = new PortfolioRepository();
    private final TransacaoRepository transacaoRepo = new TransacaoRepository();

    @FXML
    public void initialize() {
        atualizarSaldo();
        atualizarTotalPortfolio();
        configurarTabelaPortfolio();
        carregarPortfolio();
        configurarTabelaTransacoes();
        carregarHistoricoTransacoes();

        if (balanceLabel != null) {
            balanceLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.windowProperty().addListener((obs2, oldWindow, newWindow) -> {
                        if (newWindow != null && newWindow.isShowing()) {
                            atualizarSaldo();
                            atualizarTotalPortfolio();
                            carregarPortfolio();
                            carregarHistoricoTransacoes();
                        }
                    });
                }
            });
        }
    }

    private void configurarTabelaPortfolio() {
        colAtivo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getValue().getMoeda().getNome()));
        colTicker.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getValue().getMoeda().getSimbolo()));
        colQuantidade.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getValue().getQuantidade().setScale(8, RoundingMode.HALF_UP).toPlainString()));
        colPrecoMedio.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getValue().getPrecoMedioCompra().setScale(2, RoundingMode.HALF_UP).toPlainString()));
        colValor.setCellValueFactory(cell -> {
            Portfolio p = cell.getValue().getValue();
            BigDecimal total = p.getQuantidade().multiply(p.getMoeda().getValorAtual()).setScale(2, RoundingMode.HALF_UP);
            return new SimpleStringProperty(total.toPlainString());
        });
    }

    public void carregarPortfolio() {
        int userId = SessaoAtual.utilizadorId;
        List<Portfolio> lista = portfolioRepo.listarPorUtilizador(userId);
        ObservableList<Portfolio> observableList = FXCollections.observableArrayList(lista);
        RecursiveTreeItem<Portfolio> root = new RecursiveTreeItem<>(observableList, RecursiveTreeObject::getChildren);
        cryptoTable.setRoot(root);
        cryptoTable.setShowRoot(false);
    }

    public void atualizarSaldo() {
        try {
            BigDecimal novoSaldo = walletRepo.getSaldo(SessaoAtual.utilizadorId);
            SessaoAtual.saldoCarteira = novoSaldo;
            balanceLabel.setText(String.format("€ %.2f", novoSaldo));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erro ao atualizar saldo", Alert.AlertType.ERROR);
        }
    }

    public void atualizarTotalPortfolio() {
        int userId = SessaoAtual.utilizadorId;
        List<Portfolio> lista = portfolioRepo.listarPorUtilizador(userId);
        BigDecimal total = lista.stream()
                .map(p -> p.getQuantidade().multiply(p.getMoeda().getValorAtual()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        totalLabel.setText("Valor Total do Portfólio: € " + total.toPlainString());
    }

    private void configurarTabelaTransacoes() {
        colData.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getValue().getDataHora().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        ));
        colTipo.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getValue().getTipo().toUpperCase()
        ));
        colAtivoTx.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getValue().getMoeda() != null
                        ? cell.getValue().getValue().getMoeda().getSimbolo()
                        : "EUR"
        ));
        colQuantidadeTx.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getValue().getQuantidade().setScale(8, RoundingMode.HALF_UP).toPlainString()
        ));
        colValorTx.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getValue().getTotalEur().setScale(2, RoundingMode.HALF_UP).toPlainString()
        ));
    }

    public void carregarHistoricoTransacoes() {
        int userId = SessaoAtual.utilizadorId;
        List<Transacao> lista = transacaoRepo.listarPorUsuario(userId);
        ObservableList<Transacao> observableList = FXCollections.observableArrayList(lista);
        RecursiveTreeItem<Transacao> root = new RecursiveTreeItem<>(observableList, RecursiveTreeObject::getChildren);
        transactionTable.setRoot(root);
        transactionTable.setShowRoot(false);
    }

    @FXML
    public void abrirTelaDeposito() {
        abrirModal("/view/deposit_modal.fxml", "Depositar Fundos", "depositar");
    }

    @FXML
    public void abrirTelaRetirada() {
        abrirModal("/view/withdraw_modal.fxml", "Levantar Fundos", "levantar");
    }

    private void abrirModal(String fxmlPath, String titulo, String modo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ModalTransacaoController modalController) {
                modalController.setModoInicial(modo);
                modalController.setUserId(SessaoAtual.utilizadorId);
                modalController.setMainController(this);
            }

            Stage modal = new Stage();
            modal.initOwner(depositButton.getScene().getWindow());
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            modal.setScene(scene);
            modal.setTitle(titulo);
            modal.setResizable(false);
            modal.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erro ao abrir janela: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String mensagem, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
