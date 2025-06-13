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

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterField;
    @FXML private ComboBox<String> filterOp;
    @FXML private TextField filterValue;
    @FXML private ComboBox<String> sortOrder; // Novo: crescente/decrescente
    @FXML private ListView<Moeda> watchlistView;

    @FXML private ToggleButton btn1D, btn1W, btn1M, btn3M, btn1Y, btnMAX;
    @FXML private ImageView iconMoeda;
    @FXML private Label marketTitle, labelValorAtual, labelVariacao, labelVolume;
    @FXML private LineChart<String, Number> marketChart;

    private final ObservableList<Moeda> fullList = FXCollections.observableArrayList();
    private Moeda moedaAtual;
    private PauseTransition pause;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        pause = new PauseTransition(Duration.millis(300));
        pause.setOnFinished(e -> aplicarFiltros());

        // Inicializar ListView
        watchlistView.setItems(fullList);
        watchlistView.setCellFactory(lv -> createCell());
        watchlistView.setOnMouseClicked(e ->
                selecionarMoeda(watchlistView.getSelectionModel().getSelectedItem())
        );

        // Listeners
        searchField.textProperty().addListener((o, a, b) -> pause.playFromStart());
        filterField.valueProperty().addListener((obs, old, novo) -> aplicarFiltros());
        filterOp.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        filterValue.textProperty().addListener((o, a, b) -> pause.playFromStart());

        if (sortOrder != null) {
            sortOrder.setItems(FXCollections.observableArrayList("Crescente", "Decrescente"));
            sortOrder.setValue("Decrescente");
            sortOrder.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        }

        setupToggleButtons();
        aplicarFiltros();
    }

    private void aplicarFiltros() {
        String termo = Optional.ofNullable(searchField.getText()).orElse("").trim();
        String campo = Optional.ofNullable(filterField.getValue()).orElse(null);
        String operador = Optional.ofNullable(filterOp.getValue()).orElse(null);
        BigDecimal valor = null;

        try {
            String raw = filterValue.getText();
            if (raw != null && !raw.trim().isEmpty()) {
                valor = new BigDecimal(raw.trim());
            }
        } catch (NumberFormatException e) {
            valor = null; // ignora se inválido
        }

        String sortBy = campo != null ? campo : "Volume 24h";
        String ordem = (sortOrder != null && "Crescente".equals(sortOrder.getValue())) ? "ASC" : "DESC";

        List<Moeda> lista = MarketRepository.getMoedasFiltradas(
                termo,
                campo,
                operador,
                valor,
                sortBy + " " + ordem
        );

        fullList.setAll(lista);
    }

    private ListCell<Moeda> createCell() {
        return new ListCell<>() {
            private final HBox hBox = new HBox(10);
            private final ImageView img = new ImageView();
            private final VBox vBox = new VBox(2);
            private final Label nome = new Label();
            private final Label valor = new Label();

            {
                img.setFitWidth(24);
                img.setFitHeight(24);
                nome.getStyleClass().add("nome-moeda");
                valor.getStyleClass().add("valor-moeda");
                vBox.getChildren().setAll(nome, valor);
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
                    try {
                        img.setImage(new Image(
                                getClass().getResourceAsStream(
                                        "/icons/" + m.getSimbolo().toLowerCase() + ".png"
                                )
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
        labelValorAtual.setText(String.format("€ %.2f", m.getValorAtual()));
        labelVariacao.setText(String.format("%.2f%%", m.getVariacao24h()));
        labelVariacao.getStyleClass().setAll(
                m.getVariacao24h().doubleValue() >= 0
                        ? "label-variacao-positiva"
                        : "label-variacao-negativa"
        );
        labelVolume.setText(String.format("€ %,.2f", m.getVolumeMercado()));

        try {
            iconMoeda.setImage(new Image(
                    getClass().getResourceAsStream(
                            "/icons/" + m.getSimbolo().toLowerCase() + ".png"
                    )
            ));
        } catch (Exception ex) {
            iconMoeda.setImage(null);
        }

        aplicarFiltro("MAX");
    }

    private void setupToggleButtons() {
        ToggleGroup tg = new ToggleGroup();
        for (ToggleButton b : List.of(btn1D, btn1W, btn1M, btn3M, btn1Y, btnMAX)) {
            b.setToggleGroup(tg);
        }
        btnMAX.setSelected(true);
        btn1D.setOnAction(e -> aplicarFiltro("1D"));
        btn1W.setOnAction(e -> aplicarFiltro("1W"));
        btn1M.setOnAction(e -> aplicarFiltro("1M"));
        btn3M.setOnAction(e -> aplicarFiltro("3M"));
        btn1Y.setOnAction(e -> aplicarFiltro("1Y"));
        btnMAX.setOnAction(e -> aplicarFiltro("MAX"));
    }

    private void aplicarFiltro(String intervalo) {
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
    }

    @FXML private void abrirModalCompra() { abrirModalOrdem("COMPRA"); }
    @FXML private void abrirModalVenda()  { abrirModalOrdem("VENDA"); }

    private void abrirModalOrdem(String tipo) {
        if (moedaAtual == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/BuySell.fxml")
            );
            Parent root = loader.load();
            controller.OrdemController ctrl = loader.getController();
            ctrl.configurar(tipo, moedaAtual, SessaoAtual.utilizadorId, null);

            Stage modal = new Stage();
            modal.initOwner(marketChart.getScene().getWindow());
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.TRANSPARENT);
            Scene sc = new Scene(root);
            sc.setFill(Color.TRANSPARENT);
            modal.setScene(sc);
            modal.setTitle(tipo + " " + moedaAtual.getNome());
            modal.setResizable(false);
            modal.showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
