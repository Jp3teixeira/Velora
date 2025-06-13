package controller;

import Repository.MarketRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.OptionalInt;
import model.Moeda;
import utils.MarketSimulator;

public class CriptomoedaController {

    @FXML private TextField nomeField;
    @FXML private TextField simboloField;
    @FXML private TextField valorField;
    @FXML private Label mensagemLabel;

    @FXML
    private void handleCriar() {
        String nome    = nomeField.getText().trim();
        String simbolo = simboloField.getText().trim().toUpperCase();
        String valorStr= valorField.getText().trim();

        if (nome.isEmpty() || simbolo.isEmpty() || valorStr.isEmpty()) {
            mensagemLabel.setText("Preencha todos os campos!");
            mensagemLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        try {
            BigDecimal valor = new BigDecimal(valorStr);

            // Chama o método que devolve o ID (OptionalInt)
            OptionalInt optId = MarketRepository.addNewCoinReturnId(
                    nome, simbolo, "imagem.png", valor);

            if (optId.isPresent()) {
                int novoId = optId.getAsInt();

                // Injeta no simulador em memória
                Moeda m = new Moeda(
                        novoId,
                        nome,
                        simbolo,
                        valor.setScale(2, RoundingMode.HALF_UP),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );
                MarketSimulator.getMoedasSimuladas().put(novoId, m);

                mensagemLabel.setText("Criptomoeda criada com sucesso!");
                mensagemLabel.setStyle("-fx-text-fill: green;");
                nomeField.clear();
                simboloField.clear();
                valorField.clear();
            } else {
                mensagemLabel.setText("Erro ao criar criptomoeda.");
                mensagemLabel.setStyle("-fx-text-fill: red;");
            }
        } catch (NumberFormatException ex) {
            mensagemLabel.setText("Valor inválido! Use um número correto.");
            mensagemLabel.setStyle("-fx-text-fill: red;");
        } catch (Exception ex) {
            mensagemLabel.setText("Erro: " + ex.getMessage());
            mensagemLabel.setStyle("-fx-text-fill: red;");
            ex.printStackTrace();
        }
    }
}
