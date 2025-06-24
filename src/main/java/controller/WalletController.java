// src/controller/WalletController.java
package controller;

import Database.DBConnection;
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
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import model.Ordem;
import model.OrdemStatus;
import model.Portfolio;
import model.Transacao;
import utils.SessaoAtual;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class WalletController {

    @FXML private javafx.scene.control.Label balanceLabel;
    @FXML private javafx.scene.control.Label totalLabel;

    @FXML private javafx.scene.control.TextField depositAmountField;
    @FXML private javafx.scene.control.Label depositStatusLabel;
    @FXML private javafx.scene.control.TextField withdrawAmountField;
    @FXML private javafx.scene.control.Label withdrawStatusLabel;
    @FXML private javafx.scene.control.Button btnExportarCSV;

    @FXML private LineChart<String, Number> balanceChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    @FXML private ToggleGroup tableToggleGroup;
    @FXML private ToggleButton btnPosicoes;
    @FXML private ToggleButton btnOrdens;
    @FXML private ToggleButton btnHistorico;

    @FXML private JFXTreeTableView<Portfolio> cryptoTable;
    @FXML private JFXTreeTableColumn<Portfolio, String> colAtivo;
    @FXML private JFXTreeTableColumn<Portfolio, String> colQuantidade;
    @FXML private JFXTreeTableColumn<Portfolio, String> colPrecoMedio;
    @FXML private JFXTreeTableColumn<Portfolio, String> colValorAtual;
    @FXML private JFXTreeTableColumn<Portfolio, String> colRetorno;

    @FXML private JFXTreeTableView<Ordem> ordersTable;
    @FXML private JFXTreeTableColumn<Ordem, String> colNomeOrdem;
    @FXML private JFXTreeTableColumn<Ordem, String> colTipoOrdem;
    @FXML private JFXTreeTableColumn<Ordem, String> colQuantidadeOrdem;
    @FXML private JFXTreeTableColumn<Ordem, String> colPrecoLimiteOrdem;
    @FXML private JFXTreeTableColumn<Ordem, String> colDataOrdem;

    @FXML private JFXTreeTableView<Transacao> transactionTable;
    @FXML private JFXTreeTableColumn<Transacao, String> colNomeTx;
    @FXML private JFXTreeTableColumn<Transacao, String> colTipoTx;
    @FXML private JFXTreeTableColumn<Transacao, String> colQuantidadeTx;
    @FXML private JFXTreeTableColumn<Transacao, String> colValorTx;
    @FXML private JFXTreeTableColumn<Transacao, String> colDataTx;

    private final WalletRepository       walletRepo    = WalletRepository.getInstance();
    private final PortfolioRepository    portfolioRepo = new PortfolioRepository();
    private final TransacaoRepository    transacaoRepo = new TransacaoRepository();

    @FXML
    public void initialize() {
        configurarBalanceChart();
        atualizarTudo();
        tableToggleGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            cryptoTable.setVisible(sel == btnPosicoes);
            ordersTable.setVisible(sel == btnOrdens);
            transactionTable.setVisible(sel == btnHistorico);
        });
        balanceLabel.sceneProperty().addListener((o, oldS, newS) -> {
            if (newS != null) {
                newS.windowProperty().addListener((w, oldW, newW) -> {
                    if (newW != null && newW.isShowing()) atualizarTudo();
                });
            }
        });
    }

    @FXML
    public void exportarTransacoesParaCSV() {
        List<Transacao> transacoes = transacaoRepo.listarPorUsuario(SessaoAtual.utilizadorId);

        if (transacoes.isEmpty()) {
            showAlert("Sem transações para exportar.", Alert.AlertType.INFORMATION);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Histórico de Transações");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File ficheiro = fileChooser.showSaveDialog(btnExportarCSV.getScene().getWindow());

        if (ficheiro != null) {
            try (PrintWriter writer = new PrintWriter(ficheiro)) {
                writer.println("Data,Moeda,Tipo,Quantidade,Total (€)");
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                for (Transacao tx : transacoes) {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            tx.getDataHora().format(fmt),
                            tx.getMoeda().getNome(),
                            tx.getTipo(),
                            tx.getQuantidade().setScale(8, RoundingMode.HALF_UP),
                            tx.getTotalEur().setScale(2, RoundingMode.HALF_UP)
                    );
                }
                showAlert("Exportado para: " + ficheiro.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Erro ao exportar transações.", Alert.AlertType.ERROR);
            }
        }
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
        List<Object[]> hist = walletRepo.getSaldoHistorico(SessaoAtual.utilizadorId);
        for (Object[] row : hist) {
            LocalDateTime dt = (LocalDateTime) row[0];
            BigDecimal s     = (BigDecimal)    row[1];
            series.getData().add(new XYChart.Data<>(dt.format(fmt), s));
        }
        balanceChart.getData().add(series);
    }

    private void configurarTabelaPortfolio() {
        cryptoTable.getColumns().forEach(c -> c.setSortable(false));
        colAtivo.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getValue().getMoeda().getNome())
        );
        colQuantidade.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getQuantidade()
                                .setScale(8, RoundingMode.HALF_UP)
                                .toPlainString()
                )
        );
        colPrecoMedio.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getPrecoMedioCompra()
                                .setScale(2, RoundingMode.HALF_UP)
                                .toPlainString()
                )
        );
        colValorAtual.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getMoeda().getValorAtual()
                                .setScale(2, RoundingMode.HALF_UP)
                                .toPlainString()
                )
        );
        colRetorno.setCellValueFactory(c -> {
            BigDecimal pm = c.getValue().getValue().getPrecoMedioCompra();
            BigDecimal va = c.getValue().getValue().getMoeda().getValorAtual();
            BigDecimal ret = BigDecimal.ZERO;
            if (pm.compareTo(BigDecimal.ZERO) > 0) {
                ret = va.subtract(pm)
                        .divide(pm, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            return new SimpleStringProperty(ret.toPlainString() + "%");
        });
    }

    private void configurarTabelaOrdens() {
        ordersTable.getColumns().forEach(c -> c.setSortable(false));
        colNomeOrdem.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getValue().getMoeda().getNome())
        );
        colTipoOrdem.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getValue().getTipoOrdem().name())
        );
        colQuantidadeOrdem.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getQuantidade()
                                .setScale(8, RoundingMode.HALF_UP)
                                .toPlainString()
                )
        );
        colPrecoLimiteOrdem.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getPrecoUnitarioEur()
                                .setScale(2, RoundingMode.HALF_UP)
                                .toPlainString()
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
        transactionTable.getColumns().forEach(c -> c.setSortable(false));
        colNomeTx.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getValue().getMoeda().getNome())
        );
        colTipoTx.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getValue().getTipo().toUpperCase())
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
        colDataTx.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValue().getDataHora()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
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
            Connection conn = DBConnection.getConnection();
            OrdemRepository repo = new OrdemRepository(conn);
            List<Ordem> lista = repo.getAll().stream()
                    .filter(o -> o.getUtilizador().getId() == SessaoAtual.utilizadorId)
                    .filter(o -> o.getStatus() == OrdemStatus.ATIVA)
                    .collect(Collectors.toList());
            ObservableList<Ordem> obs = FXCollections.observableArrayList(lista);
            ordersTable.setRoot(new RecursiveTreeItem<>(obs, RecursiveTreeObject::getChildren));
            ordersTable.setShowRoot(false);
        } catch (Exception e) {
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
            BigDecimal s = walletRepo.getSaldoPorUtilizador(SessaoAtual.utilizadorId);
            SessaoAtual.saldoCarteira = s;
            balanceLabel.setText(String.format("€ %.2f", s));
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Erro ao atualizar saldo", Alert.AlertType.ERROR);
        }
    }

    private void atualizarTotalPortfolio() {
        List<Portfolio> lista = portfolioRepo.listarPorUtilizador(SessaoAtual.utilizadorId);
        BigDecimal total = lista.stream()
                .map(p -> p.getQuantidade().multiply(p.getMoeda().getValorAtual()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        totalLabel.setText("Valor Total: € " + total.toPlainString());
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
        try {
            BigDecimal amount = new BigDecimal(depositAmountField.getText());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                depositStatusLabel.setText("Insira valor positivo");
                return;
            }
            boolean ok = walletRepo.deposit(SessaoAtual.utilizadorId, amount);
            depositStatusLabel.setText(ok ? "Depositado com sucesso" : "Falha no depósito");
            atualizarTudo();
        } catch (NumberFormatException e) {
            depositStatusLabel.setText("Valor inválido");
        }
    }

    @FXML
    public void confirmarLevantamento() {
        try {
            BigDecimal amount = new BigDecimal(withdrawAmountField.getText());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                withdrawStatusLabel.setText("Insira valor positivo");
                return;
            }
            boolean ok = walletRepo.withdraw(SessaoAtual.utilizadorId, amount);
            withdrawStatusLabel.setText(ok ? "Levantado com sucesso" : "Saldo insuficiente");
            atualizarTudo();
        } catch (NumberFormatException e) {
            withdrawStatusLabel.setText("Valor inválido");
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
