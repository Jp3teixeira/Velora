package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import model.Moeda;
import Repository.MarketRepository;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;

public class MarketController implements Initializable {

    @FXML private ToggleButton btn1D;
    @FXML private ToggleButton btn1W;
    @FXML private ToggleButton btn1M;
    @FXML private ToggleButton btn3M;
    @FXML private ToggleButton btn1Y;
    @FXML private ToggleButton btnMAX;

    @FXML private ImageView iconMoeda;
    @FXML private Label labelValorAtual;
    @FXML private Label labelVariacao;
    @FXML private Label labelVolume;
    @FXML private ListView<Moeda> watchlistView;
    @FXML private Label marketTitle;
    @FXML private Button buyButton;
    @FXML private Button sellButton;
    @FXML private LineChart<String, Number> marketChart;

    private ObservableList<Moeda> listaMoedas = FXCollections.observableArrayList();
    private Moeda moedaAtualSelecionada = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        marketChart.getStylesheets().add(getClass().getResource("/view/css/market.css").toExternalForm());

        listaMoedas.addAll(MarketRepository.getTodasAsMoedas());
        watchlistView.setItems(listaMoedas);
        watchlistView.getSelectionModel().selectFirst();

        setupToggleButtons();

        watchlistView.setOnMouseClicked(event -> {
            moedaAtualSelecionada = watchlistView.getSelectionModel().getSelectedItem();
            if (moedaAtualSelecionada != null) {
                atualizarInformacoesMoeda();
                aplicarFiltro("MAX");
            }
        });


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
        labelValorAtual.setText(String.format("$%.2f", moedaAtualSelecionada.getValorAtual()));
        labelVariacao.setText(String.format("%.2f%%", moedaAtualSelecionada.getVariacao24h()));
        labelVolume.setText(String.format("$%,.2f", moedaAtualSelecionada.getVolumeMercado()));

        if (moedaAtualSelecionada.getVariacao24h().doubleValue() >= 0) {
            labelVariacao.getStyleClass().removeAll("negative");
            labelVariacao.getStyleClass().add("positive");
        } else {
            labelVariacao.getStyleClass().removeAll("positive");
            labelVariacao.getStyleClass().add("negative");
        }

        try {
            String path = "/icons/" + moedaAtualSelecionada.getSimbolo().toLowerCase() + ".png";
            Image image = new Image(getClass().getResourceAsStream(path));
            iconMoeda.setImage(image);
        } catch (Exception e) {
            System.out.println("Ícone não encontrado para: " + moedaAtualSelecionada.getSimbolo());
            iconMoeda.setImage(null);
        }
    }

    private void aplicarFiltro(String intervalo) {
        if (moedaAtualSelecionada != null) {
            atualizarGraficoComTooltip(moedaAtualSelecionada.getid_moeda(), intervalo);
        }
    }

    private void atualizarGraficoComTooltip(int idMoeda, String intervalo) {
        marketChart.getData().clear();
        List<XYChart.Data<String, Number>> historico = MarketRepository.getHistoricoPorMoedaFiltrado(idMoeda, intervalo);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Variação");

        for (XYChart.Data<String, Number> ponto : historico) {
            serie.getData().add(ponto);

            Tooltip tooltip = new Tooltip(String.format("Hora: %s\nValor: $%.2f",
                    ponto.getXValue(), ponto.getYValue().doubleValue()));
            tooltip.setShowDelay(Duration.millis(50));
            tooltip.setStyle("-fx-background-color: #2c2c2c; -fx-text-fill: white;");

            ponto.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    Tooltip.install(newNode, tooltip);
                    newNode.setStyle("-fx-background-color: white, #B892FF; -fx-background-radius: 6px;");
                    newNode.setEffect(new DropShadow(5, Color.web("#4B3F72")));
                }
            });
        }

        marketChart.getData().add(serie);
        marketChart.setAnimated(false);

        Platform.runLater(() -> {
            Node chartLine = marketChart.lookup(".chart-series-line");
            if (chartLine != null) {
                chartLine.setStyle("-fx-stroke: #B892FF; -fx-stroke-width: 2px;");
            }

            Node chartSymbol = marketChart.lookup(".chart-line-symbol");
            if (chartSymbol != null) {
                chartSymbol.setStyle("-fx-background-color: #B892FF, white;");
            }
        });
    }
    public void goToHome(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/homepage.fxml")); //
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
