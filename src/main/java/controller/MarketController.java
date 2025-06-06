package controller;

import javafx.animation.FadeTransition;
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
import javafx.scene.effect.DropShadow;
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
import utils.MarketSimulator;
import utils.SessaoAtual;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MarketController implements Initializable {

    @FXML private ToggleButton btn1D, btn1W, btn1M, btn3M, btn1Y, btnMAX;
    @FXML private ImageView iconMoeda;
    @FXML private Label labelValorAtual, labelVariacao, labelVolume, marketTitle;
    @FXML private ListView<Moeda> watchlistView;
    @FXML private LineChart<String, Number> marketChart;

    private final ObservableList<Moeda> listaMoedas = FXCollections.observableArrayList();
    private Moeda moedaAtualSelecionada = null;

    // Adiciona campo para conexão
    private Connection connection;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listaMoedas.addAll(MarketRepository.getTodasAsMoedas());
        watchlistView.setItems(listaMoedas);

        watchlistView.setCellFactory(param -> new ListCell<>() {
            private final HBox hBox = new HBox(10);
            private final ImageView imageView = new ImageView();
            private final VBox vBox = new VBox(2);
            private final Label labelNome = new Label();
            private final Label labelValor = new Label();

            {
                imageView.setFitHeight(24);
                imageView.setFitWidth(24);
                labelNome.getStyleClass().add("nome-moeda");
                labelValor.getStyleClass().add("valor-moeda");
                vBox.getChildren().addAll(labelNome, labelValor);
                hBox.getChildren().addAll(imageView, vBox);
            }

            @Override
            protected void updateItem(Moeda moeda, boolean empty) {
                super.updateItem(moeda, empty);

                if (empty || moeda == null) {
                    setGraphic(null);
                } else {
                    hBox.setStyle(moeda.equals(moedaAtualSelecionada)
                            ? "-fx-background-color: #2a2a2a; -fx-border-color: #b892ff; -fx-border-radius: 6; -fx-background-radius: 6;"
                            : "");
                    labelNome.setText(moeda.getNome());
                    labelValor.setText(String.format("€ %.2f", moeda.getValorAtual()));
                    try {
                        String path = "/icons/" + moeda.getSimbolo().toLowerCase() + ".png";
                        imageView.setImage(new Image(getClass().getResourceAsStream(path)));
                    } catch (Exception e) {
                        imageView.setImage(null);
                    }
                    setGraphic(hBox);
                }
            }
        });

        setupToggleButtons();

        watchlistView.setOnMouseClicked(event -> {
            Moeda selecionada = watchlistView.getSelectionModel().getSelectedItem();
            if (selecionada != null && !selecionada.equals(moedaAtualSelecionada)) {
                moedaAtualSelecionada = selecionada;
                atualizarInformacoesMoeda();
                aplicarFiltro("MAX");
            }
        });

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                listaMoedas.clear();
                listaMoedas.addAll(MarketSimulator.getMoedasAtuais().values());
                watchlistView.refresh();

                if (moedaAtualSelecionada != null) {
                    Moeda moedaAtual = MarketSimulator.getMoedasAtuais()
                            .get(moedaAtualSelecionada.getIdMoeda());
                    if (moedaAtual != null) {
                        moedaAtualSelecionada.setValorAtual(moedaAtual.getValorAtual());
                        moedaAtualSelecionada.setVolumeMercado(moedaAtual.getVolumeMercado());
                        atualizarInformacoesMoeda();
                    }
                }
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void setupToggleButtons() {
        ToggleGroup toggleGroup = new ToggleGroup();
        btn1D.setToggleGroup(toggleGroup);
        btn1W.setToggleGroup(toggleGroup);
        btn1M.setToggleGroup(toggleGroup);
        btn3M.setToggleGroup(toggleGroup);
        btn1Y.setToggleGroup(toggleGroup);
        btnMAX.setToggleGroup(toggleGroup);
        btnMAX.setSelected(true);

        btn1D.setOnAction(e -> aplicarFiltro("1D"));
        btn1W.setOnAction(e -> aplicarFiltro("1W"));
        btn1M.setOnAction(e -> aplicarFiltro("1M"));
        btn3M.setOnAction(e -> aplicarFiltro("3M"));
        btn1Y.setOnAction(e -> aplicarFiltro("1Y"));
        btnMAX.setOnAction(e -> aplicarFiltro("MAX"));
    }

    private void atualizarInformacoesMoeda() {
        marketTitle.setText(moedaAtualSelecionada.getNome() + " (" + moedaAtualSelecionada.getSimbolo() + ")");
        labelValorAtual.setText(String.format("€ %.2f", moedaAtualSelecionada.getValorAtual()));
        labelVariacao.setText(String.format("%.2f%%", moedaAtualSelecionada.getVariacao24h()));
        labelVolume.setText(String.format("€ %,.2f", moedaAtualSelecionada.getVolumeMercado()));

        labelVariacao.getStyleClass().setAll(
                moedaAtualSelecionada.getVariacao24h().doubleValue() >= 0
                        ? "label-variacao-positiva"
                        : "label-variacao-negativa"
        );

        try {
            String path = "/icons/" + moedaAtualSelecionada.getSimbolo().toLowerCase() + ".png";
            iconMoeda.setImage(new Image(getClass().getResourceAsStream(path)));
        } catch (Exception e) {
            iconMoeda.setImage(null);
        }
    }

    private void aplicarFiltro(String intervalo) {
        if (moedaAtualSelecionada != null) {
            atualizarGraficoComTooltip(moedaAtualSelecionada.getIdMoeda(), intervalo);
        }
    }

    private void atualizarGraficoComTooltip(int idMoeda, String intervalo) {
        marketChart.getData().clear();
        List<XYChart.Data<String, Number>> historico =
                MarketRepository.getHistoricoPorMoedaFiltrado(idMoeda, intervalo);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Variação");

        for (XYChart.Data<String, Number> ponto : historico) {
            serie.getData().add(ponto);

            Tooltip tooltip = new Tooltip(String.format("Hora: %s\nValor: € %.2f",
                    ponto.getXValue(), ponto.getYValue().doubleValue()));
            tooltip.setShowDelay(Duration.millis(50));
            tooltip.setStyle("""
                    -fx-background-color: #1f1f1f;
                    -fx-text-fill: #f0f0f0;
                    -fx-font-size: 12px;
                    -fx-padding: 10;
                    -fx-border-color: #b892ff;
                    -fx-border-width: 1;
                    -fx-border-radius: 6;
                    -fx-background-radius: 6;
                    """);

            ponto.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    Tooltip.install(newNode, tooltip);
                    newNode.setStyle("-fx-background-color: white, #B892FF; -fx-background-radius: 6px;");
                    newNode.setEffect(new DropShadow(5, Color.web("#4B3F72")));
                    newNode.setOnMouseEntered(e -> {
                        newNode.setScaleX(1.5);
                        newNode.setScaleY(1.5);
                    });
                    newNode.setOnMouseExited(e -> {
                        newNode.setScaleX(1.0);
                        newNode.setScaleY(1.0);
                    });
                }
            });
        }

        marketChart.setAnimated(false);
        marketChart.getData().add(serie);

        serie.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setOpacity(0);
                FadeTransition ft = new FadeTransition(Duration.millis(500), newNode);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();

                Platform.runLater(() -> {
                    Node chartLine = marketChart.lookup(".chart-series-line");
                    if (chartLine != null)
                        chartLine.setStyle("-fx-stroke: #B892FF; -fx-stroke-width: 2px;");
                });
            }
        });
    }

    @FXML
    private void abrirModalCompra() {
        abrirModalOrdem("COMPRA");
    }

    @FXML
    private void abrirModalVenda() {
        abrirModalOrdem("VENDA");
    }

    private void abrirModalOrdem(String tipo) {
        if (moedaAtualSelecionada == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/BuySell.fxml"));
            Parent root = loader.load();

            // 1) Passa os dados ao controller
            OrdemController controller = loader.getController();
            controller.configurar(
                    tipo,
                    moedaAtualSelecionada,
                    SessaoAtual.utilizadorId,
                    this.connection
            );

            // 2) Cria um Stage TRANSPARENTE
            Stage modal = new Stage();
            modal.initOwner(btn1D.getScene().getWindow());          // (ou qualquer Node do MarketController)
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.TRANSPARENT);

            // 3) Prepara a Scene também transparente
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            modal.setScene(scene);
            modal.setTitle(tipo + " de " + moedaAtualSelecionada.getNome());
            modal.setResizable(false);
            modal.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Setter que será chamado pelo NavigationHelper.goToWithController
    public void setConnection(Connection connection) {
        this.connection = connection;
    }
}
