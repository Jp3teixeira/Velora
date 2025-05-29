package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import model.Moeda;
import Repository.MarketRepository;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MoedaController implements Initializable {

    @FXML private ImageView iconMoeda;
    @FXML private Label nomeMoeda;
    @FXML private Label simboloMoeda;
    @FXML private Label valorAtual;
    @FXML private Label variacao24h;
    @FXML private Label volumeMercado;
    @FXML private LineChart<String, Number> historicoChart;

    private Moeda moedaSelecionada;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Este método será chamado quando a página moeda.fxml for carregada
    }

    public void carregarDetalhesMoeda(Moeda moeda) {
        this.moedaSelecionada = moeda;

        nomeMoeda.setText(moeda.getNome());
        simboloMoeda.setText("(" + moeda.getSimbolo() + ")");
        valorAtual.setText(String.format("$%.2f", moeda.getValorAtual()));
        variacao24h.setText(String.format("%.2f%%", moeda.getVariacao24h()));
        volumeMercado.setText(String.format("$%,.2f", moeda.getVolumeMercado()));

        try {
            String path = "/icons/" + moeda.getSimbolo().toLowerCase() + ".png";
            Image image = new Image(getClass().getResourceAsStream(path));
            iconMoeda.setImage(image);
        } catch (Exception e) {
            iconMoeda.setImage(null);
        }

        carregarHistorico(moeda.getIdMoeda());
    }

    private void carregarHistorico(int idMoeda) {
        List<XYChart.Data<String, Number>> historico = MarketRepository.getHistoricoPorMoedaFiltrado(idMoeda, "MAX");
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Histórico de Preço");

        for (XYChart.Data<String, Number> ponto : historico) {
            serie.getData().add(ponto);
        }

        historicoChart.getData().clear();
        historicoChart.getData().add(serie);
    }
    public void goToHome(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/homepage.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene novaCena = new Scene(root);

            stage.setScene(novaCena);

            //  fullscreen
            stage.setFullScreen(true);


            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
