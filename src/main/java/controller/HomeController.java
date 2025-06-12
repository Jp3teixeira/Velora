package controller;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import model.Portfolio;
import model.Transacao;
import model.Moeda;
import Repository.MarketRepository;
import Repository.PortfolioRepository;
import Repository.TransacaoRepository;
import utils.SessaoAtual;

import java.net.URL;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    /* -- SEÇÃO GRÁFICO -- */
    @FXML private VBox chartSection;

    /* -- SEÇÃO ORDENS -- */
    @FXML private ToggleGroup orderTypeGroup;
    @FXML private ToggleButton marketToggle;
    @FXML private ToggleButton limitToggle;
    @FXML private Label assetLabel;
    @FXML private TextField priceField;
    @FXML private TextField quantityField;
    @FXML private Button buyButton;
    @FXML private Button sellButton;

    /* -- SEÇÃO PORTFÓLIO (TABS) -- */
    @FXML private TabPane portfolioTabPane;
    @FXML private TableView<Portfolio> openPositionsTable;
    @FXML private TableView<?> openOrdersTable;
    @FXML private TableView<Transacao> historyTable;

    // Repositórios já existentes
    private final PortfolioRepository portfolioRepo = new PortfolioRepository();
    private final TransacaoRepository transacaoRepo = new TransacaoRepository();

    // Para exibir gráfico, usaremos o MarketRepository
    private final MarketRepository marketRepo = new MarketRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1) Configura o comportamento dos ToggleButtons (se “Limit” estiver ativo, habilita priceField)
        marketToggle.setOnAction(e -> priceField.setDisable(true));
        limitToggle.setOnAction(e -> priceField.setDisable(false));

        // 2) Monta o gráfico real para **alguma** moeda:

        List<Moeda> listaMoedas = marketRepo.getTodasAsMoedas();
        if (!listaMoedas.isEmpty()) {
            Moeda primeira = listaMoedas.get(0);
            assetLabel.setText("Ativo: " + primeira.getNome() + " (" + primeira.getSimbolo() + ")");
            LineChart<String, Number> chart = criarChartParaMoeda(primeira.getIdMoeda(), "MAX");
            chartSection.getChildren().add(chart);
            VBox.setVgrow(chart, Priority.ALWAYS);
        }

        // 3) Configura as colunas de “Posições Abertas” (Portfolio)
        configurarTabelaPortfolio();
        carregarPortfolio();

        // 4) Configura as colunas de “Histórico de Transações”
        configurarTabelaHistorico();
        carregarHistorico();

        // 5) Exemplo simples para a aba “Ordens Abertas”—por enquanto sem dados

        openOrdersTable.setPlaceholder(new Label("Funcionalidade de Ordens Abertas ainda a implementar"));

        // 6) “Comprar / Vender” (ainda só imprime no console; você pode chamar TransacaoRepository + PortfolioRepository)
        buyButton.setOnAction(e -> {
            System.out.println("Botão COMPRAR clicado para " + assetLabel.getText() +
                    ", quantidade = " + quantityField.getText() +
                    (limitToggle.isSelected() ? ", preço = " + priceField.getText() : " (Market)"));

        });
        sellButton.setOnAction(e -> {
            System.out.println("Botão VENDER clicado para " + assetLabel.getText() +
                    ", quantidade = " + quantityField.getText() +
                    (limitToggle.isSelected() ? ", preço = " + priceField.getText() : " (Market)"));

        });
    }

    // =====================  MÉTODOS PARA O GRÁFICO  =====================

    /**
     * Cria um LineChart para a moeda com ID = idMoeda, usando intervalo “intervalo”
     * e aplica fade-in (similar ao que você já faz no MarketController).
     */
    private LineChart<String, Number> criarChartParaMoeda(int idMoeda, String intervalo) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Data/Hora");
        yAxis.setLabel("Preço (€)");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Preço da Moeda");

        // Busca dados reais do banco:
        List<XYChart.Data<String, Number>> historico =
                MarketRepository.getHistoricoPorMoedaFiltrado(idMoeda, intervalo);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Variação");
        serie.getData().addAll(historico);

        chart.getData().add(serie);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.setAnimated(false);

        // Aplica fade-in:
        FadeTransition ft = new FadeTransition(Duration.millis(800), chart);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        return chart;
    }

    // =====================  MÉTODOS PARA A TABELA “Posições Abertas”  =====================

    /**
     * Configura as colunas da tabela openPositionsTable (Portfolio):
     * - Ativo
     * - Símbolo
     * - Quantidade
     * - Preço Médio (EUR)
     * - Valor Atual (EUR)
     * - Valor de Mercado (Quantidade × Preço Atual)
     */
    private void configurarTabelaPortfolio() {
        openPositionsTable.getColumns().clear();

        TableColumn<Portfolio, String> colAtivo = new TableColumn<>("Ativo");
        colAtivo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getMoeda().getNome()));

        TableColumn<Portfolio, String> colTicker = new TableColumn<>("Ticker");
        colTicker.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getMoeda().getSimbolo()));

        TableColumn<Portfolio, String> colQuantidade = new TableColumn<>("Quantidade");
        colQuantidade.setCellValueFactory(cell -> {
            BigDecimal q = cell.getValue().getQuantidade();
            return new SimpleStringProperty(q.setScale(8, RoundingMode.HALF_UP).toPlainString());
        });

        TableColumn<Portfolio, String> colPrecoMedio = new TableColumn<>("Preço Médio (€)");
        colPrecoMedio.setCellValueFactory(cell -> {
            BigDecimal pm = cell.getValue().getPrecoMedioCompra();
            return new SimpleStringProperty(pm.setScale(2, RoundingMode.HALF_UP).toPlainString());
        });

        TableColumn<Portfolio, String> colValorAtual = new TableColumn<>("Preço Atual (€)");
        colValorAtual.setCellValueFactory(cell -> {
            BigDecimal pa = cell.getValue().getMoeda().getValorAtual();
            return new SimpleStringProperty(pa.setScale(2, RoundingMode.HALF_UP).toPlainString());
        });

        TableColumn<Portfolio, String> colValorMercado = new TableColumn<>("Valor de Mercado (€)");
        colValorMercado.setCellValueFactory(cell -> {
            BigDecimal qtd = cell.getValue().getQuantidade();
            BigDecimal preco = cell.getValue().getMoeda().getValorAtual();
            BigDecimal total = qtd.multiply(preco).setScale(2, RoundingMode.HALF_UP);
            return new SimpleStringProperty(total.toPlainString());
        });

        openPositionsTable.getColumns().addAll(
                colAtivo, colTicker, colQuantidade, colPrecoMedio, colValorAtual, colValorMercado
        );
    }

    /** Carrega dados reais de PortfolioRepository e popula openPositionsTable. */
    private void carregarPortfolio() {
        int userId = SessaoAtual.utilizadorId;
        List<Portfolio> lista = portfolioRepo.listarPorUtilizador(userId);
        openPositionsTable.setItems(FXCollections.observableArrayList(lista));
    }

    // =====================  MÉTODOS PARA A TABELA “Histórico de Transações”  =====================

    /**
     * Configura as colunas da tabela historyTable (Transações):
     * - Data/Hora
     * - Tipo (compra / venda)
     * - Ativo
     * - Quantidade
     * - Preço Unitário (€)
     * - Total (€)
     */
    private void configurarTabelaHistorico() {
        historyTable.getColumns().clear();

        TableColumn<Transacao, String> colData = new TableColumn<>("Data/Hora");
        colData.setCellValueFactory(cell -> {
            String dataStr = cell.getValue().getDataHora()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return new SimpleStringProperty(dataStr);
        });

        TableColumn<Transacao, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(cell -> {
            String tipo = cell.getValue().getTipo();
            String texto = (tipo != null) ? tipo.toUpperCase() : "";
            return new SimpleStringProperty(texto);
        });

        TableColumn<Transacao, String> colAtivoTx = new TableColumn<>("Ativo");
        colAtivoTx.setCellValueFactory(cell -> {
            if (cell.getValue().getMoeda() != null) {
                return new SimpleStringProperty(cell.getValue().getMoeda().getSimbolo());
            } else {
                return new SimpleStringProperty("EUR");
            }
        });

        TableColumn<Transacao, String> colQuantidadeTx = new TableColumn<>("Quantidade");
        colQuantidadeTx.setCellValueFactory(cell -> {
            BigDecimal q = cell.getValue().getQuantidade();
            return new SimpleStringProperty(q.setScale(8, RoundingMode.HALF_UP).toPlainString());
        });

        TableColumn<Transacao, String> colPrecoUnitario = new TableColumn<>("Preço Unit. (€)");
        colPrecoUnitario.setCellValueFactory(cell -> {
            BigDecimal pu = cell.getValue().getPrecoUnitarioEur();
            return new SimpleStringProperty(pu.setScale(2, RoundingMode.HALF_UP).toPlainString());
        });

        TableColumn<Transacao, String> colTotal = new TableColumn<>("Total (€)");
        colTotal.setCellValueFactory(cell -> {
            BigDecimal t = cell.getValue().getTotalEur().setScale(2, RoundingMode.HALF_UP);
            return new SimpleStringProperty(t.toPlainString());
        });

        historyTable.getColumns().addAll(
                colData, colTipo, colAtivoTx, colQuantidadeTx, colPrecoUnitario, colTotal
        );
    }


    /** Carrega dados reais de TransacaoRepository e popula historyTable. */
    private void carregarHistorico() {
        int userId = SessaoAtual.utilizadorId;
        List<Transacao> lista = transacaoRepo.listarPorUsuario(userId);
        historyTable.setItems(FXCollections.observableArrayList(lista));
    }
}
