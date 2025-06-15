package controller;

import Repository.OrdemRepository;
import Repository.PortfolioRepository;
import Repository.TransacaoRepository;
import Repository.WalletRepository;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import model.Ordem;
import model.Portfolio;
import model.Transacao;
import utils.SessaoAtual;

public class WalletController {

    @FXML private javafx.scene.control.Label balanceLabel;
    @FXML private javafx.scene.control.Label totalLabel;

    @FXML private javafx.scene.control.TextField depositAmountField;
    @FXML private javafx.scene.control.Label depositStatusLabel;
    @FXML private javafx.scene.control.TextField withdrawAmountField;
    @FXML private javafx.scene.control.Label withdrawStatusLabel;

    @FXML private LineChart<String, Number> balanceChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    @FXML private javafx.scene.control.ToggleGroup tableToggleGroup;
    @FXML private javafx.scene.control.ToggleButton btnPosicoes;
    @FXML private javafx.scene.control.ToggleButton btnOrdens;
    @FXML private javafx.scene.control.ToggleButton btnHistorico;

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
        configurarBalanceChart();
        atualizarTudo();
        tableToggleGroup.selectedToggleProperty().addListener((obs, old, selected) -> {
            cryptoTable.setVisible(selected == btnPosicoes);
            ordersTable.setVisible(selected == btnOrdens);
            transactionTable.setVisible(selected == btnHistorico);
        });
        balanceLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((o, oldW, newW) -> {
                    if (newW != null && newW.isShowing()) atualizarTudo();
                });
            }
        });
    }

    private void configurarBalanceChart() {
        balanceChart.getData().clear();
        xAxis.setLabel("Data");
        yAxis.setLabel("€");
    }

    private void carregarBalanceChart() {
        balanceChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Saldo €");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Object[]> historico = walletRepo.getSaldoHistorico(SessaoAtual.utilizadorId);
        for (Object[] row : historico) {
            LocalDateTime dt = (LocalDateTime) row[0];
            BigDecimal saldo = (BigDecimal) row[1];
            series.getData().add(new XYChart.Data<>(dt.format(fmt), saldo));
        }
        balanceChart.getData().add(series);
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
                        c.getValue().getValue().getDataCriacao().
                                format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )
        );
    }

    private void configurarTabelaTransacoes() {
        colData.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getDataHora().
                                format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )
        );
        colTipo.setCellValueFactory(c ->
                new SimpleStringProperty(
                        (c.getValue().getValue().getTipo() != null)
                                ? c.getValue().getValue().getTipo().toUpperCase() : ""
                )
        );
        colAtivoTx.setCellValueFactory(c ->
                new SimpleStringProperty(
                        (c.getValue().getValue().getMoeda() != null)
                                ? c.getValue().getValue().getMoeda().getSimbolo() : "EUR"
                )
        );
        colQuantidadeTx.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getQuantidade().
                                setScale(8, RoundingMode.HALF_UP).toPlainString()
                )
        );
        colValorTx.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getTotalEur().
                                setScale(2, RoundingMode.HALF_UP).toPlainString()
                )
        );
    }

    private void carregarPortfolio() {
        List<Portfolio> lista = portfolioRepo.listarPorUtilizador(SessaoAtual.utilizadorId);
        ObservableList<Portfolio> obs = FXCollections.observableArrayList(lista);
        cryptoTable.setRoot(new RecursiveTreeItem<>(obs, RecursiveTreeObject::getChildren));
        cryptoTable.setShowRoot(false);
    }

    private void carregarOrdensPendentes() {
        try {
            OrdemRepository repo = new OrdemRepository(walletRepo.getConnection());
            List<Ordem> lista = repo.listarOrdensPendentesPorUsuario(SessaoAtual.utilizadorId);
            ObservableList<Ordem> obs = FXCollections.observableArrayList(lista);
            ordersTable.setRoot(new RecursiveTreeItem<>(obs, RecursiveTreeObject::getChildren));
            ordersTable.setShowRoot(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void carregarHistoricoTransacoes() {
        List<Transacao> lista = transacaoRepo.listarPorUsuario(SessaoAtual.utilizadorId);
        ObservableList<Transacao> obs = FXCollections.observableArrayList(lista);
        transactionTable.setRoot(new RecursiveTreeItem<>(obs, RecursiveTreeObject::getChildren));
        transactionTable.setShowRoot(false);
    }

    private void atualizarSaldo() {
        try {
            BigDecimal novoSaldo = walletRepo.getSaldo(SessaoAtual.utilizadorId);
            SessaoAtual.saldoCarteira = novoSaldo;
            balanceLabel.setText(String.format("€ %.2f", novoSaldo));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erro ao atualizar saldo", Alert.AlertType.ERROR);
        }
    }

    private void atualizarTotalPortfolio() {
        List<Portfolio> lista = portfolioRepo.listarPorUtilizador(SessaoAtual.utilizadorId);
        BigDecimal total = lista.stream()
                .map(p -> p.getQuantidade().multiply(p.getMoeda().getValorAtual()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        totalLabel.setText(String.format("Valor Total: € %s", total.toPlainString()));
    }

    private void atualizarTudo() {
        atualizarSaldo();
        carregarBalanceChart();
        configurarTabelaPortfolio();
        carregarPortfolio();
        configurarTabelaOrdens();
        carregarOrdensPendentes();
        configurarTabelaTransacoes();
        carregarHistoricoTransacoes();
        atualizarTotalPortfolio();
    }

    @FXML
    public void confirmarDeposito() {
        String txt = depositAmountField.getText();
        try {
            BigDecimal amount = new BigDecimal(txt);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                depositStatusLabel.setText("Insira valor positivo");
                depositStatusLabel.setStyle("-fx-text-fill: #ff4d4d;");
                return;
            }
            boolean ok = walletRepo.deposit(SessaoAtual.utilizadorId, amount);
            if (ok) {
                depositStatusLabel.setText("Depositado com sucesso");
                depositStatusLabel.setStyle("-fx-text-fill: #b892ff;");
                depositAmountField.clear();
                atualizarTudo();
            } else {
                depositStatusLabel.setText("Falha no depósito");
                depositStatusLabel.setStyle("-fx-text-fill: #ff4d4d;");
            }
        } catch (NumberFormatException e) {
            depositStatusLabel.setText("Valor inválido");
            depositStatusLabel.setStyle("-fx-text-fill: #ff4d4d;");
        }
    }

    @FXML
    public void confirmarLevantamento() {
        String txt = withdrawAmountField.getText();
        try {
            BigDecimal amount = new BigDecimal(txt);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                withdrawStatusLabel.setText("Insira valor positivo");
                withdrawStatusLabel.setStyle("-fx-text-fill: #ff4d4d;");
                return;
            }
            boolean ok = walletRepo.withdraw(SessaoAtual.utilizadorId, amount);
            if (ok) {
                withdrawStatusLabel.setText("Levantado com sucesso");
                withdrawStatusLabel.setStyle("-fx-text-fill: #b892ff;");
                withdrawAmountField.clear();
                atualizarTudo();
            } else {
                withdrawStatusLabel.setText("Saldo insuficiente");
                withdrawStatusLabel.setStyle("-fx-text-fill: #ff4d4d;");
            }
        } catch (NumberFormatException e) {
            withdrawStatusLabel.setText("Valor inválido");
            withdrawStatusLabel.setStyle("-fx-text-fill: #ff4d4d;");
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