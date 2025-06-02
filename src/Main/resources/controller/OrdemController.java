package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Moeda;
import model.Ordem;
import utils.TradeService;

import java.sql.Connection;
import java.time.LocalDateTime;

public class OrdemController {

    @FXML private Label labelTitulo;
    @FXML private Label labelPrecoAtual;
    @FXML private TextField txtQuantidade;
    @FXML private Button btnConfirmar;

    private String tipoOrdem; // "COMPRA" ou "VENDA"
    private Moeda moedaSelecionada;
    private Connection connection;
    private int idCarteira;

    public void configurar(String tipoOrdem, Moeda moeda, Connection connection, int idCarteira) {
        this.tipoOrdem = tipoOrdem;
        this.moedaSelecionada = moeda;
        this.connection = connection;
        this.idCarteira = idCarteira;

        labelTitulo.setText(tipoOrdem + " - " + moeda.getNome());
        labelPrecoAtual.setText("Preço atual: $" + moeda.getValorAtual());
    }

    @FXML
    private void confirmarOrdem() {
        try {
            double quantidade = Double.parseDouble(txtQuantidade.getText());
            if (quantidade <= 0) throw new NumberFormatException();

            Ordem ordem = new Ordem();
            ordem.setId_carteira(idCarteira);
            ordem.setId_moeda(moedaSelecionada.getIdMoeda());
            ordem.setTipo(tipoOrdem);
            ordem.setQuantidade_total(quantidade);
            ordem.setQuantidade_restante(quantidade);
            ordem.setPreco_no_momento(moedaSelecionada.getValorAtual().doubleValue());
            ordem.setStatus("PENDENTE");
            ordem.setTimestamp_criacao(LocalDateTime.now());

            TradeService tradeService = new TradeService(connection);

            if ("COMPRA".equalsIgnoreCase(tipoOrdem)) {
                tradeService.processarOrdemCompra(ordem);
            } else if ("VENDA".equalsIgnoreCase(tipoOrdem)) {
                tradeService.processarOrdemVenda(ordem);
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION, tipoOrdem + " executada com sucesso!");
            alert.show();
            fecharJanela();

        } catch (NumberFormatException e) {
            mostrarErro("Insere uma quantidade válida.");
        } catch (Exception e) {
            mostrarErro("Erro ao processar a ordem.");
            e.printStackTrace();
        }
    }

    private void mostrarErro(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR, mensagem);
        alert.show();
    }

    @FXML
    private void fecharJanela() {
        Stage stage = (Stage) btnConfirmar.getScene().getWindow();
        stage.close();
    }
}
