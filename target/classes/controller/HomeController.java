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
    @FXML private TableView<?> walletTable;
    @FXML private Button depositButton;
    @FXML private Button withdrawButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        carregarListaMoedas();
        atualizarSaldo();
    }

    private void carregarListaMoedas() {
        List<Moeda> moedas = MarketRepository.getTodasAsMoedas();  // sem getInstance()
        for (Moeda moeda : moedas) {
            cryptoList.getItems().add(moeda.getNome());
        }

        cryptoList.getSelectionModel().selectedItemProperty().addListener((obs, old, novaMoeda) -> {
            if (novaMoeda != null) {
                carregarGrafico(novaMoeda);
            }
        });
    }

    private void carregarGrafico(String nomeMoeda) {
        priceChart.getData().clear();
        XYChart.Series<Number, Number> serie = new XYChart.Series<>();

        // ❗ Placeholder: Este método tens de criar ou adaptar
        List<Double> historico = getHistoricoSimples(nomeMoeda);

        for (int i = 0; i < historico.size(); i++) {
            serie.getData().add(new XYChart.Data<>(i, historico.get(i)));
        }

        serie.setName(nomeMoeda);
        priceChart.getData().add(serie);
    }

    // Exemplo simples só para não dar erro de compilação
    private List<Double> getHistoricoSimples(String nomeMoeda) {
        // ⚠️ Tens de adaptar isto ao teu sistema (por id, símbolo, etc)
        return List.of(1.0, 1.2, 1.3, 1.25, 1.4); // Simulação temporária
    }

    private void atualizarSaldo() {
        int userId = SessaoAtual.utilizadorId;
        BigDecimal saldo = WalletRepository.getInstance().getSaldo(userId);
        balanceLabel.setText("Saldo: " + saldo.setScale(2, RoundingMode.HALF_UP) + " €");
    }

    @FXML
    private void handleLogOut(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Tem certeza que deseja sair?", ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Confirmação de Logout");

        try (InputStream iconStream = getClass().getResourceAsStream("/icons/moedas.png")) {
            if (iconStream != null) {
                ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image(iconStream));
            }
        } catch (Exception ignored) {}

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) goTo("/view/login.fxml", false);
        });
    }
}
