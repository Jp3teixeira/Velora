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
    private Moeda moedaAtualSelecionada;
    private Connection connection;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inicia o simulador (se ainda não iniciado) e carrega a lista
        MarketSimulator.startSimulador();
        listaMoedas.setAll(MarketSimulator.getMoedasSimuladas().values());
        watchlistView.setItems(listaMoedas);

        // Configura células da ListView
        watchlistView.setCellFactory(param -> new ListCell<>() {
            private final HBox hBox = new HBox(10);
            private final ImageView img = new ImageView();
            private final VBox vBox = new VBox(2);
            private final Label lblNome = new Label();
            private final Label lblValor= new Label();

            {
                img.setFitWidth(24); img.setFitHeight(24);
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
                    } catch (Exception e) {
                        img.setImage(null);
                    }
                    setGraphic(hBox);
                }
            }
        });

        setupToggleButtons();

        watchlistView.setOnMouseClicked(evt -> {
            Moeda m = watchlistView.getSelectionModel().getSelectedItem();
            if (m != null && !m.equals(moedaAtualSelecionada)) {
                moedaAtualSelecionada = m;
                atualizarInformacoesMoeda();
                aplicarFiltro("MAX");
            }
        });

        // Atualiza UI a cada 1 minuto
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> Platform.runLater(() -> {
            List<Moeda> atuais = List.copyOf(MarketSimulator.getMoedasSimuladas().values());
            listaMoedas.setAll(atuais);
            watchlistView.refresh();

            if (moedaAtualSelecionada != null) {
                int id = moedaAtualSelecionada.getIdMoeda();
                Moeda novo = MarketSimulator.getMoedasSimuladas().get(id);
                if (novo != null) {
                    moedaAtualSelecionada.setValorAtual(novo.getValorAtual());
                    moedaAtualSelecionada.setVariacao24h(novo.getVariacao24h());
                    moedaAtualSelecionada.setVolumeMercado(novo.getVolumeMercado());
                    atualizarInformacoesMoeda();
                }
            }
        }), 0, 1, TimeUnit.MINUTES);
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
        if (moedaAtualSelecionada == null) return;
        marketChart.getData().clear();
        List<XYChart.Data<String, Number>> hist =
                MarketRepository.getHistoricoPorMoedaFiltrado(
                        moedaAtualSelecionada.getIdMoeda(), intervalo);
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName(intervalo);
        hist.forEach(p -> {
            serie.getData().add(p);
            Tooltip tp = new Tooltip(String.format("%s: € %.2f",
                    p.getXValue(), p.getYValue().doubleValue()));
            tp.setShowDelay(Duration.millis(50));
            Tooltip.install(p.getNode(), tp);
            p.getNode().setStyle("-fx-background-color: white, #B892FF; -fx-background-radius:6px;");
        });
        marketChart.getData().add(serie);
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
                    this.connection);

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

    // Chamado pelo NavigationHelper ao abrir esta tela
    public void setConnection(Connection connection) {
        this.connection = connection;
    }
}
