package controller;

import Repository.MarketRepository;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Moeda;
import utils.SessaoAtual;
import utils.NavigationHelper;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;

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

        TableView<Moeda> tableView = new TableView<>();

        // Coluna Nome
        TableColumn<Moeda, String> nomeCol = new TableColumn<>("Nome");
        nomeCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNome()));

        // Coluna Símbolo
        TableColumn<Moeda, String> simboloCol = new TableColumn<>("Símbolo");
        simboloCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSimbolo()));

        // Coluna Valor Atual
        TableColumn<Moeda, String> valorCol = new TableColumn<>("Valor Atual (€)");
        valorCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getValorAtual() != null ? cellData.getValue().getValorAtual().toString() : "N/A"));

        // Coluna Variação 24h
        TableColumn<Moeda, String> variacaoCol = new TableColumn<>("Variação 24h");
        variacaoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getVariacao24h() != null ? cellData.getValue().getVariacao24h().toString() : "N/A"));

        // Coluna Volume Mercado
        TableColumn<Moeda, String> volumeCol = new TableColumn<>("Volume Mercado");
        volumeCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getVolumeMercado() != null ? cellData.getValue().getVolumeMercado().toString() : "N/A"));

        tableView.getColumns().addAll(nomeCol, simboloCol, valorCol, variacaoCol, volumeCol);

        // Busca lista de moedas na base de dados (assumindo método existente em MarketRepository)
        var listaMoedas = MarketRepository.getAllCoins();

        tableView.getItems().addAll((Collection<? extends Moeda>) listaMoedas);

        contentArea.getChildren().add(tableView);

        TableColumn<Moeda, Void> actionCol = new TableColumn<>("Ações");

        actionCol.setCellFactory(col -> {
            return new TableCell<Moeda, Void>() {
                private final Button btnEditar = new Button("Editar");
                private final Button btnEliminar = new Button("Eliminar");
                private final HBox pane = new HBox(10, btnEditar, btnEliminar);

                {
                    btnEditar.setStyle("-fx-background-color: #4B3F72; -fx-text-fill: white;");
                    btnEliminar.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white;");

                    // Ação do botão Editar
                    btnEditar.setOnAction(event -> {
                        Moeda moeda = getTableView().getItems().get(getIndex());
                        abrirFormularioEdicao(moeda);
                    });

                    // Ação do botão Eliminar
                    btnEliminar.setOnAction(event -> {
                        Moeda moeda = getTableView().getItems().get(getIndex());
                        Alert alert = new Alert(AlertType.CONFIRMATION);
                        alert.setTitle("Confirmação");
                        alert.setHeaderText("Eliminar Moeda");
                        alert.setContentText("Tem a certeza que deseja eliminar " + moeda.getNome() + "?");

                        Optional<ButtonType> resultado = alert.showAndWait();
                        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
                            try {
                                MarketRepository.deleteMoeda(moeda.getIdMoeda());
                                getTableView().getItems().remove(moeda);
                            } catch (SQLException e) {
                                e.printStackTrace();
                                Alert erro = new Alert(AlertType.ERROR, "Erro ao eliminar a moeda.");
                                erro.show();
                            }
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(pane);
                    }
                }
            };
        });


// Adicione a coluna de ações à tabela
        tableView.getColumns().add(actionCol);



    }

    private void abrirFormularioEdicao(Moeda moeda) {
        contentArea.getChildren().clear();

        Label title = new Label("Editar Moeda: " + moeda.getNome());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4B3F72;");

        TextField nameField = new TextField(moeda.getNome());
        TextField symbolField = new TextField(moeda.getSimbolo());
        TextField valorAtualField = new TextField(moeda.getValorAtual().toString());
        TextField variacaoField = new TextField(moeda.getVariacao24h().toString());
        TextField volumeField = new TextField(moeda.getVolumeMercado().toString());

        Button salvarBtn = new Button("Salvar");
        salvarBtn.setStyle("-fx-background-color: #4B3F72; -fx-text-fill: white;");
        Label statusLabel = new Label();

        salvarBtn.setOnAction(e -> {
            try {
                moeda.setNome(nameField.getText().trim());
                moeda.setSimbolo(symbolField.getText().trim());
                moeda.setValorAtual(new BigDecimal(valorAtualField.getText().trim()));
                moeda.setVariacao24h(new BigDecimal(variacaoField.getText().trim()));
                moeda.setVolumeMercado(new BigDecimal(volumeField.getText().trim()));

                MarketRepository.updateMoeda(moeda);
                statusLabel.setText("Moeda atualizada com sucesso!");
                statusLabel.setStyle("-fx-text-fill: green;");

                // Atualizar a tabela - para isso, poderias recarregar a lista ou manipular diretamente o TableView
                handleViewStatistics(); // Recarrega a tabela

            } catch (NumberFormatException ex) {
                statusLabel.setText("Erro: valores inválidos!");
                statusLabel.setStyle("-fx-text-fill: red;");
            } catch (SQLException ex) {
                statusLabel.setText("Erro ao atualizar moeda.");
                statusLabel.setStyle("-fx-text-fill: red;");
                ex.printStackTrace();
            }
        });

        VBox form = new VBox(10, title, nameField, symbolField, valorAtualField, variacaoField, volumeField, salvarBtn, statusLabel);
        form.setStyle("-fx-padding: 20; -fx-background-color: #F8F8F8; -fx-background-radius: 10;");
        form.setMaxWidth(400);

        contentArea.getChildren().add(form);
    }


    private void showEditMoedaDialog(Moeda moeda, TableView<Moeda> tableView) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar Moeda");

        ButtonType salvarButtonType = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(salvarButtonType, ButtonType.CANCEL);

        TextField nomeField = new TextField(moeda.getNome());
        TextField simboloField = new TextField(moeda.getSimbolo());
        TextField valorField = new TextField(moeda.getValorAtual().toString());
        TextField variacaoField = new TextField(moeda.getVariacao24h().toString());
        TextField volumeField = new TextField(moeda.getVolumeMercado().toString());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nomeField, 1, 0);
        grid.add(new Label("Símbolo:"), 0, 1);
        grid.add(simboloField, 1, 1);
        grid.add(new Label("Valor Atual:"), 0, 2);
        grid.add(valorField, 1, 2);
        grid.add(new Label("Variação 24h:"), 0, 3);
        grid.add(variacaoField, 1, 3);
        grid.add(new Label("Volume Mercado:"), 0, 4);
        grid.add(volumeField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == salvarButtonType) {
            try {
                moeda.setNome(nomeField.getText());
                moeda.setSimbolo(simboloField.getText());
                moeda.setValorAtual(new BigDecimal(valorField.getText()));
                moeda.setVariacao24h(new BigDecimal(variacaoField.getText()));
                moeda.setVolumeMercado(new BigDecimal(volumeField.getText()));

                MarketRepository.updateMoeda(moeda);
                tableView.refresh();
            } catch (Exception ex) {
                alert("Erro: " + ex.getMessage());
            }
        }
    }

    private void eliminarMoeda(Moeda moeda, TableView<Moeda> tableView) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminação");
        confirm.setHeaderText("Quer mesmo eliminar a moeda " + moeda.getNome() + "?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                MarketRepository.deleteMoeda(moeda.getIdMoeda());
                tableView.getItems().remove(moeda);
            } catch (Exception ex) {
                alert("Erro ao eliminar: " + ex.getMessage());
            }
        }
    }

    private void alert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
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