package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import model.Moeda;
import Repository.MarketRepository;
import Repository.WalletRepository;
import utils.Routes;
import utils.SessaoAtual;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static utils.NavigationHelper.goTo;

public class HomeController implements Initializable {

    @FXML private ListView<String> cryptoList;
    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    @FXML private Label balanceLabel;
    @FXML private ListView<String> walletTable;
    @FXML private Button depositButton;
    @FXML private Button withdrawButton;

    private List<Moeda> todasMoedas;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1) Carrega lista de moedas
        todasMoedas = MarketRepository.getTodasAsMoedas();
        carregarListaMoedas();
        atualizarSaldo();
    }

    private void carregarListaMoedas() {
        for (Moeda moeda : todasMoedas) {
            cryptoList.getItems().add(moeda.getNome());
        }

        cryptoList.getSelectionModel().selectedItemProperty().addListener((obs, old, novaMoedaNome) -> {
            if (novaMoedaNome != null) {
                carregarGrafico(novaMoedaNome);
            }
        });
    }

    private void carregarGrafico(String nomeMoeda) {
        priceChart.getData().clear();
        XYChart.Series<Number, Number> serie = new XYChart.Series<>();
        serie.setName(nomeMoeda);

        // Encontra o objeto Moeda pelo nome
        Moeda selecionada = todasMoedas.stream()
                .filter(m -> m.getNome().equals(nomeMoeda))
                .findFirst()
                .orElse(null);
        if (selecionada == null) return;

        int idMoeda = selecionada.getIdMoeda();

        // Obtém histórico (String→Number) e converte para índice X
        List<XYChart.Data<String, Number>> historicoString =
                MarketRepository.getHistoricoPorMoedaFiltrado(idMoeda, "MAX");

        for (int i = 0; i < historicoString.size(); i++) {
            Number y = historicoString.get(i).getYValue();
            serie.getData().add(new XYChart.Data<>(i, y));
        }

        priceChart.getData().add(serie);
    }

    private void atualizarSaldo() {
        int userId = SessaoAtual.utilizadorId;
        BigDecimal saldo = WalletRepository.getInstance().getSaldo(userId);
        balanceLabel.setText("Saldo: " + saldo.setScale(2, RoundingMode.HALF_UP) + " €");
    }

    @FXML
    private void handleLogOut(ActionEvent event) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Tem certeza que deseja sair?",
                ButtonType.OK,
                ButtonType.CANCEL
        );
        alert.setTitle("Confirmação de Logout");

        try (InputStream iconStream = getClass().getResourceAsStream("/icons/moedas.png")) {
            if (iconStream != null) {
                ((Stage) alert.getDialogPane().getScene().getWindow())
                        .getIcons().add(new Image(iconStream));
            }
        } catch (Exception ignored) {}

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                goTo(Routes.LOGIN, false);
            }
        });
    }
}
