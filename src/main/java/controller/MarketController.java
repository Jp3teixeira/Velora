package controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import model.Moeda;
import Repository.MarketRepository;
import utils.SessaoAtual;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MarketController implements Initializable {

    // ——————————————————————————————————————————————————————————
    // COMPONENTES PARA FILTROS & TRENDS
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterField;
    @FXML private ComboBox<String> filterOp;
    @FXML private TextField filterValue;
    @FXML private ListView<Moeda> trendingView;
    @FXML private ListView<Moeda> watchlistView;


    @FXML private ToggleButton btn1D, btn1W, btn1M, btn3M, btn1Y, btnMAX;
    @FXML private ImageView iconMoeda;
    @FXML private Label marketTitle, labelValorAtual, labelVariacao, labelVolume;
    @FXML private LineChart<String, Number> marketChart;

    // ——————————————————————————————————————————————————————————
    // OBSERVABLE LISTS
    private final ObservableList<Moeda> fullList  = FXCollections.observableArrayList();
    private final ObservableList<Moeda> trendList = FXCollections.observableArrayList();

    // SELEÇÃO ATUAL
    private Moeda moedaAtualSelecionada;

    // PAUSA PARA “LAZY SEARCH”
    private PauseTransition pause;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1) configurar pausa (debounce)
        pause = new PauseTransition(Duration.millis(300));
        pause.setOnFinished(e -> aplicarFiltros());

        // 2) inicializa trending
        trendList.setAll(MarketRepository.getTrendingMoedas(5));
        trendingView.setItems(trendList);
        trendingView.setCellFactory(param -> createCellFactory());
        trendingView.setOnMouseClicked(e -> selecionarMoeda(trendingView.getSelectionModel().getSelectedItem()));

        // 3) inicializa watchlist (filtrada)
        watchlistView.setItems(fullList);
        watchlistView.setCellFactory(param -> createCellFactory());
        watchlistView.setOnMouseClicked(e -> selecionarMoeda(watchlistView.getSelectionModel().getSelectedItem()));

        // 4) listeners de pesquisa e filtros
        searchField.textProperty().addListener((obs,o,n) -> pause.playFromStart());
        filterField.valueProperty().addListener((obs,o,n) -> pause.playFromStart());
        filterOp.valueProperty().addListener((obs,o,n) -> pause.playFromStart());
        filterValue.textProperty().addListener((obs,o,n) -> pause.playFromStart());

        // 5) carrega lista inicial (sem filtro)
        aplicarFiltros();

        // 6) configura botões de intervalo e gráfico
        setupToggleButtons();
    }

    /** Cria o mesmo ListCell usado para trending e watchlist. */
    private ListCell<Moeda> createCellFactory() {
        return new ListCell<>() {
            private final HBox hBox = new HBox(10);
            private final ImageView img = new ImageView();
            private final VBox vBox = new VBox(2);
            private final Label lblNome = new Label();
            private final Label lblValor= new Label();

            {
                img.setFitWidth(24);
                img.setFitHeight(24);
                lblNome.getStyleClass().add("nome-moeda");
                lblValor.getStyleClass().add("valor-moeda");
                vBox.getChildren().setAll(lblNome, lblValor);
                hBox.getChildren().setAll(img, vBox);
            }

            @Override
            protected void updateItem(Moeda m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) {
                    setGraphic(null);
                } else {
                    boolean sel = m.equals(moedaAtualSelecionada);
                    hBox.setStyle(sel
                            ? "-fx-background-color: #2a2a2a; -fx-border-color: #b892ff; -fx-border-radius:6; -fx-background-radius:6;"
                            : "");
                    lblNome.setText(m.getNome());
                    lblValor.setText(String.format("€ %.2f", m.getValorAtual()));
                    try {
                        String path = "/icons/" + m.getSimbolo().toLowerCase() + ".png";
                        img.setImage(new Image(getClass().getResourceAsStream(path)));
                    } catch (Exception ex) {
                        img.setImage(null);
                    }
                    setGraphic(hBox);
                }
            }
        };
    }

    /** Aplica os filtros (pesquisa + campo numérico) via Repository. */
    private void aplicarFiltros() {
        String termo = Optional.ofNullable(searchField.getText()).orElse("").trim();
        String campo = filterField.getValue();
        String op    = filterOp.getValue();
        BigDecimal val = null;
        try {
            if (filterValue.getText() != null && !filterValue.getText().isBlank()) {
                val = new BigDecimal(filterValue.getText().trim());
            }
        } catch (NumberFormatException ignored) { }
        List<Moeda> resultado = MarketRepository.getMoedasFiltradas(termo, campo, op, val);
        fullList.setAll(resultado);
    }

    /** Seleciona a moeda, atualiza detalhes e carrega gráfico “MAX”. */
    private void selecionarMoeda(Moeda m) {
        if (m != null && !m.equals(moedaAtualSelecionada)) {
            moedaAtualSelecionada = m;
            atualizarInformacoesMoeda();
            aplicarFiltro("MAX");
        }
    }

    private void setupToggleButtons() {
        ToggleGroup tg = new ToggleGroup();
        btn1D.setToggleGroup(tg); btn1W.setToggleGroup(tg);
        btn1M.setToggleGroup(tg); btn3M.setToggleGroup(tg);
        btn1Y.setToggleGroup(tg); btnMAX.setToggleGroup(tg);
        btnMAX.setSelected(true);
        btn1D.setOnAction(e -> aplicarFiltro("1D"));
        btn1W.setOnAction(e -> aplicarFiltro("1W"));
        btn1M.setOnAction(e -> aplicarFiltro("1M"));
        btn3M.setOnAction(e -> aplicarFiltro("3M"));
        btn1Y.setOnAction(e -> aplicarFiltro("1Y"));
        btnMAX.setOnAction(e -> aplicarFiltro("MAX"));
    }

    private void atualizarInformacoesMoeda() {
        marketTitle.setText(moedaAtualSelecionada.getNome()
                + " (" + moedaAtualSelecionada.getSimbolo() + ")");
        labelValorAtual.setText(String.format("€ %.2f", moedaAtualSelecionada.getValorAtual()));
        labelVariacao.setText(String.format("%.2f%%", moedaAtualSelecionada.getVariacao24h()));
        labelVariacao.getStyleClass().setAll(
                moedaAtualSelecionada.getVariacao24h().doubleValue() >= 0
                        ? "label-variacao-positiva"
                        : "label-variacao-negativa"
        );
        labelVolume.setText(String.format("€ %,.2f", moedaAtualSelecionada.getVolumeMercado()));
        try {
            String path = "/icons/" + moedaAtualSelecionada.getSimbolo().toLowerCase() + ".png";
            iconMoeda.setImage(new Image(getClass().getResourceAsStream(path)));
        } catch (Exception e) {
            iconMoeda.setImage(null);
        }
    }

    /** Mantém o comportamento original de carregar histórico no gráfico. */
    private void aplicarFiltro(String intervalo) {
        if (moedaAtualSelecionada == null) return;
        marketChart.getData().clear();

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName(intervalo);

        MarketRepository
                .getHistoricoPorMoedaFiltrado(moedaAtualSelecionada.getIdMoeda(), intervalo)
                .forEach(serie.getData()::add);

        marketChart.getData().add(serie);

        // tooltips & estilo dos pontos
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : serie.getData()) {
                Node node = d.getNode();
                if (node != null) {
                    Tooltip tp = new Tooltip(
                            String.format("%s: € %.2f", d.getXValue(), d.getYValue().doubleValue())
                    );
                    tp.setShowDelay(Duration.millis(50));
                    Tooltip.install(node, tp);
                    node.setStyle(
                            "-fx-background-color: white, #B892FF; " +
                                    "-fx-background-radius: 6px;"
                    );
                }
            }
        });
    }

    @FXML private void abrirModalCompra() { abrirModalOrdem("COMPRA"); }
    @FXML private void abrirModalVenda()  { abrirModalOrdem("VENDA"); }

    private void abrirModalOrdem(String tipo) {
        if (moedaAtualSelecionada == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/BuySell.fxml"));
            Parent root = loader.load();
            controller.OrdemController ctrl = loader.getController();
            ctrl.configurar(tipo,
                    moedaAtualSelecionada,
                    SessaoAtual.utilizadorId,
                    null);

            Stage modal = new Stage();
            modal.initOwner(btn1D.getScene().getWindow());
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.TRANSPARENT);
            Scene sc = new Scene(root);
            sc.setFill(Color.TRANSPARENT);
            modal.setScene(sc);
            modal.setTitle(tipo + " " + moedaAtualSelecionada.getNome());
            modal.setResizable(false);
            modal.showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
