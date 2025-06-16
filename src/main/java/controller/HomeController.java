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

    // Repositórios
    private final PortfolioRepository portfolioRepo = new PortfolioRepository();
    private final TransacaoRepository transacaoRepo = new TransacaoRepository();
    private final MarketRepository marketRepo = new MarketRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ToggleButtons
        marketToggle.setOnAction(e -> priceField.setDisable(true));
        limitToggle.setOnAction(e -> priceField.setDisable(false));

        // Gráfico inicial
        List<Moeda> listaMoedas = MarketRepository.getMoedasOrdenadas("", "Valor Atual", false);
        if (!listaMoedas.isEmpty()) {
            Moeda primeira = listaMoedas.get(0);
            assetLabel.setText("Ativo: " + primeira.getNome() + " (" + primeira.getSimbolo() + ")");
            LineChart<String, Number> chart = criarChartParaMoeda(primeira.getIdMoeda(), "MAX");
            chartSection.getChildren().add(chart);
            VBox.setVgrow(chart, Priority.ALWAYS);
        }

        // Tabela Portfolio
        configurarTabelaPortfolio();
        carregarPortfolio();

        // Tabela Histórico
        configurarTabelaHistorico();
        carregarHistorico();

        // Aba Ordens Abertas placeholder
        openOrdersTable.setPlaceholder(new Label("Funcionalidade de Ordens Abertas ainda a implementar"));

        // Comprar / Vender (a implementar)
        buyButton.setOnAction(e -> System.out.println("COMPRAR: " + quantityField.getText()));
        sellButton.setOnAction(e -> System.out.println("VENDER: " + quantityField.getText()));
    }

    private LineChart<String, Number> criarChartParaMoeda(int idMoeda, String intervalo) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Data/Hora");
        yAxis.setLabel("Preço (€)");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Preço da Moeda");

        List<XYChart.Data<String, Number>> historico =
                MarketRepository.getHistoricoPorMoedaFiltrado(idMoeda, intervalo);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Preço");
        serie.getData().addAll(historico);

        chart.getData().add(serie);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.setAnimated(false);

        FadeTransition ft = new FadeTransition(Duration.millis(800), chart);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        return chart;
    }

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

    private void carregarPortfolio() {
        int userId = SessaoAtual.utilizadorId;
        List<Portfolio> lista = portfolioRepo.listarPorUtilizador(userId);
        openPositionsTable.setItems(FXCollections.observableArrayList(lista));
    }

    private void configurarTabelaHistorico() {
        historyTable.getColumns().clear();

        TableColumn<Transacao, String> colData = new TableColumn<>("Data/Hora");
        colData.setCellValueFactory(cell -> {
            String dataStr = cell.getValue().getDataHora()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return new SimpleStringProperty(dataStr);
        });

        TableColumn<Transacao, String> colAtivoTx = new TableColumn<>("Ativo");
        colAtivoTx.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getMoeda().getSimbolo())
        );

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
                colData, colAtivoTx, colQuantidadeTx, colPrecoUnitario, colTotal
        );
    }

    private void carregarHistorico() {
        int userId = SessaoAtual.utilizadorId;
        List<Transacao> lista = transacaoRepo.listarPorUsuario(userId);
        historyTable.setItems(FXCollections.observableArrayList(lista));
    }
}