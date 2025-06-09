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
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.Portfolio;
import model.Transacao;
import utils.SessaoAtual;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        atualizarTudo();
        // recarrega sempre que a janela aparece
        if (balanceLabel != null) {
            balanceLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.windowProperty().addListener((obs2, oldWindow, newWindow) -> {
                        if (newWindow != null && newWindow.isShowing()) {
                            atualizarTudo();
                        }
                    });
                }
            });
        }
    }

    private void configurarTabelaPortfolio() {
        colAtivo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getValue().getMoeda().getNome())
        );
        colTicker.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getValue().getMoeda().getSimbolo())
        );
        colQuantidade.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getValue()
                                .getQuantidade()
                                .setScale(8, RoundingMode.HALF_UP)
                                .toPlainString()
                )
        );
        colPrecoMedio.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getValue()
                                .getPrecoMedioCompra()
                                .setScale(2, RoundingMode.HALF_UP)
                                .toPlainString()
                )
        );
        colValor.setCellValueFactory(cell -> {
            Portfolio p = cell.getValue().getValue();
            BigDecimal total = p.getQuantidade()
                    .multiply(p.getMoeda().getValorAtual())
                    .setScale(2, RoundingMode.HALF_UP);
            return new SimpleStringProperty(total.toPlainString());
        });
    }

    private void configurarTabelaTransacoes() {
        colData.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getValue()
                                .getDataHora()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )
        );
        // aqui alterámos getTipo() para getTipoOrdem()
        colTipo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getValue().getTipo().toUpperCase())
        );
        colAtivoTx.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getValue().getMoeda() != null
                                ? cell.getValue().getValue().getMoeda().getSimbolo()
                                : "EUR"
                )
        );
        colQuantidadeTx.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getValue()
                                .getQuantidade()
                                .setScale(8, RoundingMode.HALF_UP)
                                .toPlainString()
                )
        );
        colValorTx.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getValue()
                                .getTotalEur()
                                .setScale(2, RoundingMode.HALF_UP)
                                .toPlainString()
                )
        );
    }

    public void carregarPortfolio() {
        List<Portfolio> lista = portfolioRepo.listarPorUtilizador(SessaoAtual.utilizadorId);
        ObservableList<Portfolio> obsList = FXCollections.observableArrayList(lista);
        cryptoTable.setRoot(new RecursiveTreeItem<>(obsList, RecursiveTreeObject::getChildren));
        cryptoTable.setShowRoot(false);
    }

    public void carregarHistoricoTransacoes() {
        List<Transacao> lista = transacaoRepo.listarPorUsuario(SessaoAtual.utilizadorId);
        ObservableList<Transacao> obsList = FXCollections.observableArrayList(lista);
        transactionTable.setRoot(new RecursiveTreeItem<>(obsList, RecursiveTreeObject::getChildren));
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
        configurarTabelaTransacoes();
        carregarHistoricoTransacoes();
    }

    @FXML
    public void confirmarDeposito() {
        try {
            BigDecimal amount = new BigDecimal(depositAmountField.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                depositStatusLabel.setText("Valor deve ser positivo!");
                return;
            }
            if (walletRepo.deposit(SessaoAtual.utilizadorId, amount)) {
                SessaoAtual.saldoCarteira = walletRepo.getSaldo(SessaoAtual.utilizadorId);
                depositStatusLabel.setText("Depósito efetuado com sucesso!");
                atualizarTudo();
            } else {
                depositStatusLabel.setText("Erro ao processar depósito.");
            }
        } catch (NumberFormatException e) {
            depositStatusLabel.setText("Valor inválido. Ex: 100.00");
        } catch (Exception e) {
            depositStatusLabel.setText("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void confirmarLevantamento() {
        try {
            BigDecimal amount = new BigDecimal(withdrawAmountField.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                withdrawStatusLabel.setText("Valor deve ser positivo!");
                return;
            }
            BigDecimal saldoAtual = walletRepo.getSaldo(SessaoAtual.utilizadorId);
            if (saldoAtual.compareTo(amount) < 0) {
                withdrawStatusLabel.setText("Saldo insuficiente!");
                return;
            }
            if (walletRepo.withdraw(SessaoAtual.utilizadorId, amount)) {
                SessaoAtual.saldoCarteira = walletRepo.getSaldo(SessaoAtual.utilizadorId);
                withdrawStatusLabel.setText("Levantamento efetuado com sucesso!");
                atualizarTudo();
            } else {
                withdrawStatusLabel.setText("Erro ao processar levantamento.");
            }
        } catch (NumberFormatException e) {
            withdrawStatusLabel.setText("Valor inválido. Ex: 50.00");
        } catch (Exception e) {
            withdrawStatusLabel.setText("Erro: " + e.getMessage());
            e.printStackTrace();
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
