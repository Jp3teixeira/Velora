package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.Moeda;
import Repository.MarketRepository;

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
        // nada a inicializar aqui
    }

    public void carregarDetalhesMoeda(Moeda moeda) {
        this.moedaSelecionada = moeda;

        nomeMoeda.setText(moeda.getNome());
        simboloMoeda.setText("(" + moeda.getSimbolo() + ")");
        // Ajustado para mostrar em euros
        valorAtual.setText(String.format("€ %.2f", moeda.getValorAtual()));
        variacao24h.setText(String.format("%.2f%%", moeda.getVariacao24h()));
        volumeMercado.setText(String.format("€ %,.2f", moeda.getVolume24h()));

        try {
            String path = "/icons/" + moeda.getSimbolo().toLowerCase() + ".png";
            Image image = new Image(getClass().getResourceAsStream(path));
            iconMoeda.setImage(image);
        } catch (Exception e) {
            iconMoeda.setImage(null);
        }

        carregarHistorico(moeda.getId());
    }

    private void carregarHistorico(int idMoeda) {
        // "MAX" não gera intervalo específico; MarketRepository busca tudo
        List<XYChart.Data<String, Number>> historico =
                MarketRepository.getHistoricoPorMoedaFiltrado(idMoeda, "MAX");
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Histórico de Preço");

        for (XYChart.Data<String, Number> ponto : historico) {
            serie.getData().add(ponto);
        }

        historicoChart.getData().clear();
        historicoChart.getData().add(serie);
    }
}
