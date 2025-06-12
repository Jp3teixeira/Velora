package controller;

import Repository.OrdemRepository;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.Ordem;
import model.Portfolio;
import model.Transacao;
import utils.SessaoAtual;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class WalletController {

    @FXML private Label balanceLabel;
    @FXML private Label totalLabel;

    @FXML private TextField depositAmountField;
    @FXML private Label depositStatusLabel;
    @FXML private TextField withdrawAmountField;
    @FXML private Label withdrawStatusLabel;

    @FXML private JFXTreeTableView<Portfolio> cryptoTable;
    @FXML private JFXTreeTableColumn<Portfolio, String> colAtivo;
    @FXML private JFXTreeTableColumn<Portfolio, String> colTicker;
    @FXML private JFXTreeTableColumn<Portfolio, String> colQuantidade;
    @FXML private JFXTreeTableColumn<Portfolio, String> colPrecoMedio;
    @FXML private JFXTreeTableColumn<Portfolio, String> colValor;

    @FXML private JFXTreeTableView<Ordem> ordersTable;
    @FXML private JFXTreeTableColumn<Ordem, String> colTipoOrdem;
    @FXML private JFXTreeTableColumn<Ordem, String> colMoedaOrdem;
    @FXML private JFXTreeTableColumn<Ordem, String> colQuantidadeOrdem;
    @FXML private JFXTreeTableColumn<Ordem, String> colPrecoLimiteOrdem;
    @FXML private JFXTreeTableColumn<Ordem, String> colDataOrdem;

    @FXML private JFXTreeTableView<Transacao> transactionTable;
    @FXML private JFXTreeTableColumn<Transacao, String> colData;
    @FXML private JFXTreeTableColumn<Transacao, String> colTipo;
    @FXML private JFXTreeTableColumn<Transacao, String> colAtivoTx;
    @FXML private JFXTreeTableColumn<Transacao, String> colQuantidadeTx;
    @FXML private JFXTreeTableColumn<Transacao, String> colValorTx;

    private final WalletRepository walletRepo       = WalletRepository.getInstance();
    private final PortfolioRepository portfolioRepo = new PortfolioRepository();
    private final TransacaoRepository transacaoRepo = new TransacaoRepository();

    @FXML
    public void initialize() {
        System.out.println("CSS URL: " + getClass().getResource("/view/css/wallet.css"));
        atualizarTudo();
        balanceLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((o, oldW, newW) -> {
                    if (newW != null && newW.isShowing()) atualizarTudo();
                });
            }
        });
    }

    private void configurarTabelaPortfolio() {
        colAtivo.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getValue().getMoeda().getNome())
        );
        colTicker.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getValue().getMoeda().getSimbolo())
        );
        colQuantidade.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getQuantidade().setScale(8, RoundingMode.HALF_UP).toPlainString()
                )
        );
        colPrecoMedio.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getPrecoMedioCompra().setScale(2, RoundingMode.HALF_UP).toPlainString()
                )
        );
        colValor.setCellValueFactory(c -> {
            BigDecimal total = c.getValue().getValue().getQuantidade()
                    .multiply(c.getValue().getValue().getMoeda().getValorAtual())
                    .setScale(2, RoundingMode.HALF_UP);
            return new SimpleStringProperty(total.toPlainString());
        });
    }

    private void configurarTabelaOrdens() {
        colTipoOrdem.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getValue().getTipoOrdem().toUpperCase())
        );
        colMoedaOrdem.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getValue().getMoeda().getSimbolo())
        );
        colQuantidadeOrdem.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getQuantidade().setScale(8, RoundingMode.HALF_UP).toPlainString()
                )
        );
        colPrecoLimiteOrdem.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getPrecoUnitarioEur().setScale(2, RoundingMode.HALF_UP).toPlainString()
                )
        );
        colDataOrdem.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getDataCriacao()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )
        );
    }

    private void configurarTabelaTransacoes() {
        colData.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getDataHora()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )
        );

        colTipo.setCellValueFactory(c -> {
            String tipo = c.getValue().getValue().getTipo();
            String texto = (tipo != null) ? tipo.toUpperCase() : "";
            return new SimpleStringProperty(texto);
        });

        colAtivoTx.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getMoeda() != null
                                ? c.getValue().getValue().getMoeda().getSimbolo()
                                : "EUR"
                )
        );

        colQuantidadeTx.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getQuantidade()
                                .setScale(8, RoundingMode.HALF_UP)
                                .toPlainString()
                )
        );

        colValorTx.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getTotalEur()
                                .setScale(2, RoundingMode.HALF_UP)
                                .toPlainString()
                )
        );
    }


    public void carregarPortfolio() {
        List<Portfolio> lista = portfolioRepo.listarPorUtilizador(SessaoAtual.utilizadorId);
        ObservableList<Portfolio> obs = FXCollections.observableArrayList(lista);
        cryptoTable.setRoot(new RecursiveTreeItem<>(obs, RecursiveTreeObject::getChildren));
        cryptoTable.setShowRoot(false);
    }

    public void carregarOrdensPendentes() {
        try {
            OrdemRepository repo = new OrdemRepository(WalletRepository.getInstance().getConnection());
            List<Ordem> lista = repo.listarOrdensPendentesPorUsuario(SessaoAtual.utilizadorId);
            ObservableList<Ordem> obs = FXCollections.observableArrayList(lista);
            ordersTable.setRoot(new RecursiveTreeItem<>(obs, RecursiveTreeObject::getChildren));
            ordersTable.setShowRoot(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void carregarHistoricoTransacoes() {
        List<Transacao> lista = transacaoRepo.listarPorUsuario(SessaoAtual.utilizadorId);
        ObservableList<Transacao> obs = FXCollections.observableArrayList(lista);
        transactionTable.setRoot(new RecursiveTreeItem<>(obs, RecursiveTreeObject::getChildren));
        transactionTable.setShowRoot(false);
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
        List<Portfolio> lista = portfolioRepo.listarPorUtilizador(SessaoAtual.utilizadorId);
        BigDecimal total = lista.stream()
                .map(p -> p.getQuantidade().multiply(p.getMoeda().getValorAtual()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        totalLabel.setText("Valor Total do Portfólio: € " + total.toPlainString());
    }

    private void atualizarTudo() {
        atualizarSaldo();
        atualizarTotalPortfolio();
        configurarTabelaPortfolio();
        carregarPortfolio();
        configurarTabelaOrdens();
        carregarOrdensPendentes();
        configurarTabelaTransacoes();
        carregarHistoricoTransacoes();
    }

    @FXML
    public void confirmarDeposito() { /* ... */ }

    @FXML
    public void confirmarLevantamento() { /* ... */ }

    private void showAlert(String mensagem, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
