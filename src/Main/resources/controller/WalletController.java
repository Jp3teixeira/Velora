package controller;

import Repository.PortfolioRepository;
import Repository.TransacaoRepository;
import Repository.WalletRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
    @FXML private TableView<Portfolio> cryptoTable;
    @FXML private TableColumn<Portfolio, String> colAtivo;
    @FXML private TableColumn<Portfolio, String> colTicker;
    @FXML private TableColumn<Portfolio, String> colQuantidade;
    @FXML private TableColumn<Portfolio, String> colPrecoMedio;
    @FXML private TableColumn<Portfolio, String> colValor;

    // “HISTÓRICO DE TRANSAÇÕES”
    @FXML private TableView<Transacao> transactionTable;
    @FXML private TableColumn<Transacao, String> colData;
    @FXML private TableColumn<Transacao, String> colTipo;
    @FXML private TableColumn<Transacao, String> colAtivoTx;
    @FXML private TableColumn<Transacao, String> colQuantidadeTx;
    @FXML private TableColumn<Transacao, String> colValorTx;

    private final WalletRepository    walletRepo    = WalletRepository.getInstance();
    private final PortfolioRepository portfolioRepo = new PortfolioRepository();
    private final TransacaoRepository transacaoRepo = new TransacaoRepository();

    @FXML
    public void initialize() {
        // 1) Atualiza saldo e total do portfólio
        atualizarSaldo();
        atualizarTotalPortfolio();

        // 2) Configura “Meus Ativos”
        configurarTabelaPortfolio();
        carregarPortfolio();

        // 3) Configura “Histórico de Transações”
        configurarTabelaTransacoes();
        carregarHistoricoTransacoes();

        // 4) Recarrega tudo quando a janela ganha foco
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

    // Ajusta colunas de “Meus Ativos”
    private void configurarTabelaPortfolio() {
        colAtivo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getMoeda().getNome()));
        colTicker.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getMoeda().getSimbolo()));
        colQuantidade.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getQuantidade().setScale(8, RoundingMode.HALF_UP).toPlainString()));
        colPrecoMedio.setCellValueFactory(cell -> {
            BigDecimal pm = cell.getValue().getPrecoMedioCompra();
            return new SimpleStringProperty(pm.setScale(2, RoundingMode.HALF_UP).toPlainString());
        });
        colValor.setCellValueFactory(cell -> {
            BigDecimal qtd   = cell.getValue().getQuantidade();
            BigDecimal preco = cell.getValue().getMoeda().getValorAtual();
            BigDecimal total = qtd.multiply(preco).setScale(2, RoundingMode.HALF_UP);
            return new SimpleStringProperty(total.toPlainString());
        });
    }

    // Popula dados na tabela “Meus Ativos”
    public void carregarPortfolio() {
        int userId = SessaoAtual.utilizadorId;
        List<Portfolio> lista = portfolioRepo.listarPorUtilizador(userId);
        cryptoTable.setItems(FXCollections.observableArrayList(lista));
    }

    // Atualiza label de saldo
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

    // Calcula e exibe valor total do portfólio
    public void atualizarTotalPortfolio() {
        int userId = SessaoAtual.utilizadorId;
        List<Portfolio> lista = portfolioRepo.listarPorUtilizador(userId);
        BigDecimal total = lista.stream()
                .map(p -> p.getQuantidade().multiply(p.getMoeda().getValorAtual()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        totalLabel.setText("Valor Total do Portfólio: € " + total.toPlainString());
    }

    // Ajusta colunas de “Histórico de Transações”
    private void configurarTabelaTransacoes() {
        colData.setCellValueFactory(cell -> {
            String dataStr = cell.getValue().getDataHora()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return new SimpleStringProperty(dataStr);
        });
        colTipo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getTipo().toUpperCase()));
        colAtivoTx.setCellValueFactory(cell -> {
            if (cell.getValue().getMoeda() != null) {
                return new SimpleStringProperty(cell.getValue().getMoeda().getSimbolo());
            } else {
                return new SimpleStringProperty("EUR");
            }
        });
        colQuantidadeTx.setCellValueFactory(cell -> {
            BigDecimal qtd = cell.getValue().getQuantidade();
            return new SimpleStringProperty(qtd.setScale(8, RoundingMode.HALF_UP).toPlainString());
        });
        colValorTx.setCellValueFactory(cell -> {
            BigDecimal total = cell.getValue().getTotalEur().setScale(2, RoundingMode.HALF_UP);
            return new SimpleStringProperty(total.toPlainString());
        });
    }

    // Popula dados na tabela “Histórico de Transações”
    public void carregarHistoricoTransacoes() {
        int userId = SessaoAtual.utilizadorId;
        List<Transacao> lista = transacaoRepo.listarPorUsuario(userId);
        transactionTable.setItems(FXCollections.observableArrayList(lista));
    }

    // ==================== MODAIS ====================

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
            // 1) Carrega o FXML normalmente
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Se quiser, passe dados ao controller:
            Object controller = loader.getController();
            if (controller instanceof ModalTransacaoController modalController) {
                modalController.setModoInicial(modo);
                modalController.setUserId(SessaoAtual.utilizadorId);
                modalController.setMainController(this);
            }

            // 2) Cria um novo Stage
            Stage modal = new Stage();
            modal.initOwner(depositButton.getScene().getWindow());
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.TRANSPARENT);

            // 3) Cria a Scene com fundo transparente
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            modal.setScene(scene);
            modal.setTitle(titulo);
            modal.setResizable(false);

            // 4) Mostra e espera fechar
            modal.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erro ao abrir janela: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ==================== AUXILIAR ====================
    private void showAlert(String mensagem, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
