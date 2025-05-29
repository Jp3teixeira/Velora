package controller;

import Repository.MarketRepository;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import utils.SessaoAtual;
import utils.NavigationHelper;

import java.io.IOException;
import java.math.BigDecimal;

public class AdminDashboardController {

    @FXML
    private Button adminButton;
    @FXML
    private StackPane contentArea;



    @FXML
    public void initialize() {
        // Controle de visibilidade do botão admin
        if (adminButton != null) {
            boolean isAdmin = "Admin".equals(SessaoAtual.tipo);
            adminButton.setVisible(isAdmin);
            adminButton.setManaged(isAdmin);
        }
    }




    private void createAddCoinForm() {
        try {
            // Cria os campos do formulário
            Label title = new Label("Adicionar Nova Moeda");
            title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4B3F72;");

            TextField nameField = new TextField();
            nameField.setPromptText("Nome da Moeda (ex: Bitcoin)");

            TextField symbolField = new TextField();
            symbolField.setPromptText("Símbolo (ex: BTC)");

            TextField imageField = new TextField();
            imageField.setPromptText("Nome da Imagem (ex: btc.png)");

            TextField initialValueField = new TextField();
            initialValueField.setPromptText("Valor Inicial (ex: 50000.00)");

            Button submitButton = new Button("Adicionar Moeda");
            submitButton.setStyle("-fx-background-color: #4B3F72; -fx-text-fill: white;");

            Label statusLabel = new Label();

            // Ação do botão de submit
            submitButton.setOnAction(e -> {
                try {
                    String name = nameField.getText().trim();
                    String symbol = symbolField.getText().trim().toUpperCase();
                    String image = imageField.getText().trim().toLowerCase();
                    BigDecimal initialValue = new BigDecimal(initialValueField.getText().trim());

                    if (name.isEmpty() || symbol.isEmpty() || image.isEmpty()) {
                        statusLabel.setText("Preencha todos os campos!");
                        statusLabel.setStyle("-fx-text-fill: red;");
                        return;
                    }

                    // Adiciona à base de dados
                    boolean success = MarketRepository.addNewCoin(name, symbol, image, initialValue);

                    if (success) {
                        statusLabel.setText("Moeda adicionada com sucesso!");
                        statusLabel.setStyle("-fx-text-fill: green;");
                        // Limpa os campos
                        nameField.clear();
                        symbolField.clear();
                        imageField.clear();
                        initialValueField.clear();
                    } else {
                        statusLabel.setText("Erro ao adicionar moeda!");
                        statusLabel.setStyle("-fx-text-fill: red;");
                    }
                } catch (NumberFormatException ex) {
                    statusLabel.setText("Valor inválido! Use formato 50000.00");
                    statusLabel.setStyle("-fx-text-fill: red;");
                } catch (Exception ex) {
                    statusLabel.setText("Erro: " + ex.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                    ex.printStackTrace();
                }
            });

            // Organiza os elementos em um VBox
            VBox form = new VBox(10, title, nameField, symbolField, imageField,
                    initialValueField, submitButton, statusLabel);
            form.setStyle("-fx-padding: 20; -fx-background-color: #F8F8F8; -fx-background-radius: 10;");
            form.setMaxWidth(400);

            // Adiciona ao contentArea
            contentArea.getChildren().add(form);
        } catch (Exception e) {
            e.printStackTrace();
            contentArea.getChildren().add(new Label("Erro ao criar formulário: " + e.getMessage()));
        }
    }

    @FXML
    private void handleViewStatistics() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(new Label("Estatísticas serão exibidas aqui"));
    }

    @FXML
    private void handleLogOut() {
        NavigationHelper.goTo("/view/login.fxml", false);
    }

    @FXML
    private void goToMarket() {
        NavigationHelper.goTo("/view/market.fxml", true);
    }


    @FXML
    private void goToHome() {
        NavigationHelper.goTo("/view/homepage.fxml", true);
    }


    private void showAlert(String mensagem, AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Mensagem");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    @FXML
    private void handleCreateCrypto(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CriarCriptomoeda.fxml"));
            Parent criarCryptoForm = loader.load();
            contentArea.getChildren().setAll(criarCryptoForm);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}