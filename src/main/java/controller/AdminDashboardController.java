package controller;

import Repository.MarketRepository;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.Moeda;
import utils.SessaoAtual;
import utils.NavigationHelper;
import utils.Routes;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class AdminDashboardController {

    @FXML private Button adminButton;
    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        // Mostra o botão de admin apenas se o tipo for "admin"
        if (adminButton != null) {
            boolean isAdmin = SessaoAtual.tipo != null &&
                    SessaoAtual.tipo.equalsIgnoreCase("admin");
            adminButton.setVisible(isAdmin);
            adminButton.setManaged(isAdmin);
        }
    }

    private void createAddCoinForm() {
        try {
            Label title = new Label("Adicionar Nova Moeda");
            title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4B3F72;");

            TextField nameField = new TextField();
            nameField.setPromptText("Nome da Moeda (ex: Bitcoin)");

            TextField symbolField = new TextField();
            symbolField.setPromptText("Símbolo (ex: BTC)");

            // Mantido apenas como placeholder; esquema de BD não armazena imagem
            TextField imageField = new TextField();
            imageField.setPromptText("Nome da Imagem (ex: btc.png)");

            TextField initialValueField = new TextField();
            initialValueField.setPromptText("Valor Inicial (ex: 50000.00)");

            Button submitButton = new Button("Adicionar Moeda");
            submitButton.setStyle("-fx-background-color: #4B3F72; -fx-text-fill: white;");

            Label statusLabel = new Label();

            submitButton.setOnAction(e -> {
                try {
                    String name   = nameField.getText().trim();
                    String symbol = symbolField.getText().trim().toUpperCase();
                    String image  = imageField.getText().trim().toLowerCase();
                    BigDecimal initialValue = new BigDecimal(initialValueField.getText().trim());

                    if (name.isEmpty() || symbol.isEmpty() || image.isEmpty()) {
                        statusLabel.setText("Preencha todos os campos!");
                        statusLabel.setStyle("-fx-text-fill: red;");
                        return;
                    }

                    // Insere em Moeda, PrecoMoeda e VolumeMercado
                    boolean success = MarketRepository.addNewCoin(name, symbol, image, initialValue);

                    if (success) {
                        statusLabel.setText("Moeda adicionada com sucesso!");
                        statusLabel.setStyle("-fx-text-fill: green;");
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

            VBox form = new VBox(10,
                    title,
                    nameField,
                    symbolField,
                    imageField,
                    initialValueField,
                    submitButton,
                    statusLabel);
            form.setStyle("-fx-padding: 20; -fx-background-color: #F8F8F8; -fx-background-radius: 10;");
            form.setMaxWidth(400);

            contentArea.getChildren().setAll(form);
        } catch (Exception e) {
            e.printStackTrace();
            contentArea.getChildren().setAll(new Label("Erro ao criar formulário: " + e.getMessage()));
        }
    }

    @FXML
    private void handleViewStatistics() {
        contentArea.getChildren().clear();

        TableView<Moeda> tableView = new TableView<>();

        // Coluna Nome
        TableColumn<Moeda, String> nomeCol = new TableColumn<>("Nome");
        nomeCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNome()));

        // Coluna Símbolo
        TableColumn<Moeda, String> simboloCol = new TableColumn<>("Símbolo");
        simboloCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSimbolo()));

        // Coluna Valor Atual (€)
        TableColumn<Moeda, String> valorCol = new TableColumn<>("Valor Atual (€)");
        valorCol.setCellValueFactory(cellData -> {
            BigDecimal v = cellData.getValue().getValorAtual();
            return new javafx.beans.property.SimpleStringProperty(
                    v != null ? v.toString() : "N/A");
        });

        // Coluna Variação 24h
        TableColumn<Moeda, String> variacaoCol = new TableColumn<>("Variação 24h");
        variacaoCol.setCellValueFactory(cellData -> {
            BigDecimal v = cellData.getValue().getVariacao24h();
            return new javafx.beans.property.SimpleStringProperty(
                    v != null ? v.toString() + "%" : "N/A");
        });

        // Coluna Volume Mercado
        TableColumn<Moeda, String> volumeCol = new TableColumn<>("Volume Mercado (€)");
        volumeCol.setCellValueFactory(cellData -> {
            BigDecimal v = cellData.getValue().getVolumeMercado();
            return new javafx.beans.property.SimpleStringProperty(
                    v != null ? v.toString() : "N/A");
        });

        tableView.getColumns().addAll(nomeCol, simboloCol, valorCol, variacaoCol, volumeCol);

        // Popula com lista de Moedas
        List<Moeda> listaMoedas = MarketRepository.getTodasAsMoedas();
        tableView.getItems().addAll((Collection<? extends Moeda>) listaMoedas);

        // Coluna de Ações
        TableColumn<Moeda, Void> actionCol = new TableColumn<>("Ações");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar   = new Button("Editar");
            private final Button btnEliminar = new Button("Eliminar");
            private final HBox pane = new HBox(10, btnEditar, btnEliminar);

            {
                btnEditar.setStyle("-fx-background-color: #4B3F72; -fx-text-fill: white;");
                btnEliminar.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white;");

                btnEditar.setOnAction(event -> {
                    Moeda moeda = getTableView().getItems().get(getIndex());
                    abrirFormularioEdicao(moeda, tableView);
                });

                btnEliminar.setOnAction(event -> {
                    Moeda moeda = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(AlertType.CONFIRMATION);
                    alert.setTitle("Confirmação");
                    alert.setHeaderText("Eliminar Moeda");
                    alert.setContentText("Deseja realmente eliminar " + moeda.getNome() + "?");

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
        });

        tableView.getColumns().add(actionCol);
        contentArea.getChildren().add(tableView);
    }

    private void abrirFormularioEdicao(Moeda moeda, TableView<Moeda> tableView) {
        contentArea.getChildren().clear();

        Label title = new Label("Editar Moeda: " + moeda.getNome());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4B3F72;");

        TextField nameField      = new TextField(moeda.getNome());
        TextField symbolField    = new TextField(moeda.getSimbolo());
        TextField valorAtualField = new TextField(moeda.getValorAtual().toString());
        TextField variacaoField  = new TextField(moeda.getVariacao24h().toString());
        TextField volumeField    = new TextField(moeda.getVolumeMercado().toString());

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

                // Atualiza tabela Moeda (colunas nome, símbolo, tipo não mudam)
                MarketRepository.updateMoeda(moeda);
                statusLabel.setText("Moeda atualizada com sucesso!");
                statusLabel.setStyle("-fx-text-fill: green;");

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

        VBox form = new VBox(10,
                title,
                nameField,
                symbolField,
                valorAtualField,
                variacaoField,
                volumeField,
                salvarBtn,
                statusLabel);
        form.setStyle("-fx-padding: 20; -fx-background-color: #F8F8F8; -fx-background-radius: 10;");
        form.setMaxWidth(400);

        contentArea.getChildren().add(form);
    }

    @FXML
    private void handleLogOut() {
        SessaoAtual.limparSessao();
        NavigationHelper.goTo(Routes.LOGIN, false);
    }

    @FXML
    private void handleCreateCrypto(ActionEvent event) {
        createAddCoinForm();
    }
}
