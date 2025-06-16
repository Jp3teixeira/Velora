package controller;

import Repository.MarketRepository;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import model.Moeda;
import utils.SessaoAtual;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MarketController implements Initializable {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterField;
    @FXML private ListView<Moeda> watchlistView;

    @FXML private ToggleButton btn1D, btn1W, btn1M, btn3M, btn1Y, btnMAX;
    @FXML private ImageView iconMoeda;
    @FXML private Label marketTitle, labelValorAtual, labelVariacao, labelVolume;
    @FXML private LineChart<String, Number> marketChart;

    private final ObservableList<Moeda> fullList = FXCollections.observableArrayList();
    private Moeda moedaAtual;
    private PauseTransition pause;

    private boolean valorAtualAsc = false;
    private boolean variacao24hAsc = false;
    private String currentIntervalo = "MAX";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1) debounce de pesquisa
        pause = new PauseTransition(Duration.millis(300));
        pause.setOnFinished(e -> aplicarOrdenacao());
        searchField.textProperty().addListener((o, a, b) -> pause.playFromStart());

        // 2) filtro de coluna
        filterField.setItems(FXCollections.observableArrayList("Valor Atual", "Variação 24h"));
        filterField.setPromptText("Ordenar por…");
        filterField.setOnAction(e -> pause.playFromStart());

        // 3) lista de moedas
        watchlistView.setItems(fullList);
        watchlistView.setCellFactory(lv -> createCell());
        watchlistView.setOnMouseClicked(e -> selecionarMoeda(watchlistView.getSelectionModel().getSelectedItem()));

        // 4) botões de intervalo de tempo
        setupToggleButtons();

        // 5) carregamento inicial
        aplicarOrdenacao();
        ScheduledExecutorService uiUpdater = Executors.newSingleThreadScheduledExecutor();
        uiUpdater.scheduleAtFixedRate(() -> Platform.runLater(() -> {
            aplicarOrdenacao();
            watchlistView.refresh();
            refreshDetail();
            aplicarFiltro(currentIntervalo);
        }), 0, 15, TimeUnit.SECONDS);
    }

    private void aplicarOrdenacao() {
        String termo = Optional.ofNullable(searchField.getText()).orElse("").trim();
        String campo = Optional.ofNullable(filterField.getValue()).orElse("Valor Atual");
        boolean asc = campo.equals("Valor Atual") ? (valorAtualAsc = !valorAtualAsc)
                : (variacao24hAsc = !variacao24hAsc);

        List<Moeda> lista = MarketRepository.getMoedasOrdenadas(termo, campo, asc);
        fullList.setAll(lista);
    }

    private void refreshDetail() {
        if (moedaAtual == null) return;
        labelValorAtual.setText(String.format("€ %.2f", moedaAtual.getValorAtual()));
        double var24 = moedaAtual.getVariacao24h().doubleValue();
        labelVariacao.setText(String.format("%.2f%%", var24));
        labelVariacao.getStyleClass().setAll(var24 >= 0
                ? "label-variacao-positiva" : "label-variacao-negativa");
        labelVolume.setText(String.format("€ %, .2f", moedaAtual.getVolume24h()));
    }

    private ListCell<Moeda> createCell() {
        return new ListCell<>() {
            private final HBox   hBox    = new HBox(10);
            private final ImageView img  = new ImageView();
            private final VBox    vBox   = new VBox(2);
            private final Label   nome   = new Label();
            private final Label   valor  = new Label();
            private final Label   vari   = new Label();

            {
                img.setFitWidth(24); img.setFitHeight(24);
                vBox.getChildren().setAll(nome, valor, vari);
                hBox.getChildren().setAll(img, vBox);
            }

            @Override
            protected void updateItem(Moeda m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) {
                    setGraphic(null);
                } else {
                    nome.setText(m.getNome());
                    valor.setText(String.format("€ %.2f", m.getValorAtual()));
                    double v = m.getVariacao24h().doubleValue();
                    vari.setText(String.format("%.2f%%", v));
                    vari.getStyleClass().setAll(v >= 0
                            ? "label-variacao-positiva"
                            : "label-variacao-negativa");
                    try {
                        img.setImage(new Image(
                                getClass().getResourceAsStream("/icons/" + m.getSimbolo().toLowerCase() + ".png")
                        ));
                    } catch (Exception ex) {
                        img.setImage(null);
                    }
                    setGraphic(hBox);
                }
            }
        };
    }

    private void selecionarMoeda(Moeda m) {
        if (m == null || m.equals(moedaAtual)) return;
        moedaAtual = m;
        marketTitle.setText(m.getNome() + " (" + m.getSimbolo() + ")");
        refreshDetail();
        aplicarFiltro(currentIntervalo);
    }

    private void setupToggleButtons() {
        ToggleButton[] btns = {btn1D, btn1W, btn1M, btn3M, btn1Y, btnMAX};
        for (ToggleButton b : btns) {
            b.setOnAction(e -> aplicarFiltro(b.getText().startsWith("Últimas") ? btn1D.getText().substring(7) : b.getText()));
        }
        btnMAX.setSelected(true);
    }

    private void aplicarFiltro(String intervalo) {
        currentIntervalo = intervalo;
        if (moedaAtual == null) return;
        marketChart.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName(intervalo);
        MarketRepository
                .getHistoricoPorMoedaFiltrado(moedaAtual.getIdMoeda(), intervalo)
                .forEach(serie.getData()::add);
        marketChart.getData().add(serie);

        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : serie.getData()) {
                Node node = d.getNode();
                if (node != null) {
                    Tooltip tp = new Tooltip(
                            d.getXValue() + ": € " + d.getYValue().doubleValue()
                                    + "\nVariação 24h: "
                                    + String.format("%.2f%%", moedaAtual.getVariacao24h())
                    );
                    tp.setShowDelay(Duration.millis(50));
                    Tooltip.install(node, tp);
                    node.setStyle(
                            "-fx-background-color: white, #b892ff; " +
                                    "-fx-background-radius: 6px;"
                    );
                }
            }
        });
        if ("1D".equals(intervalo)) {
            refreshDetail();
        }
    }

    @FXML private void abrirModalCompra() { abrirModalOrdem("COMPRA"); }
    @FXML private void abrirModalVenda()  { abrirModalOrdem("VENDA"); }

    private void abrirModalOrdem(String tipo) {
        if (moedaAtual == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/BuySell.fxml"));
            Parent root = loader.load();
            controller.OrdemController ctrl = loader.getController();
            ctrl.configurar(tipo, moedaAtual, SessaoAtual.utilizadorId, null);
            Stage modal = new Stage();
            modal.initOwner(marketChart.getScene().getWindow());
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.TRANSPARENT);
            modal.setScene(new Scene(root));
            modal.showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
