package controller;

import Repository.MarketRepository;
import model.Moeda;
import utils.SessaoAtual;
import utils.NavigationHelper;
import utils.Routes;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminDashboardController {

    @FXML private Button adminButton;
    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        boolean hasPermission = SessaoAtual.tipo != null &&
                (SessaoAtual.tipo.equalsIgnoreCase("admin") || SessaoAtual.isSuperAdmin);

        if (!hasPermission) {
            NavigationHelper.goTo(Routes.HOMEPAGE, true);
        }
    }

    @FXML
    private void handleCreateCrypto(ActionEvent event) {
        showAddCoinForm();
    }

    private void showAddCoinForm() {
        Label title = new Label("Adicionar Nova Moeda");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4B3F72;");

        TextField nameField         = new TextField();
        nameField.setPromptText("Nome da Moeda (ex: Bitcoin)");
        TextField symbolField       = new TextField();
        symbolField.setPromptText("Símbolo (ex: BTC)");
        TextField imageField        = new TextField();
        imageField.setPromptText("Nome da Imagem (ex: btc.png)");
        TextField initialValueField = new TextField();
        initialValueField.setPromptText("Valor Inicial (ex: 50000.00)");

        Button submitButton = new Button("Adicionar Moeda");
        submitButton.setStyle("-fx-background-color: #4B3F72; -fx-text-fill: white;");
        Label statusLabel   = new Label();

        submitButton.setOnAction(e -> {
            String nome   = nameField.getText().trim();
            String simbolo= symbolField.getText().trim().toUpperCase();
            String foto   = imageField.getText().trim();
            try {
                if (nome.isEmpty() || simbolo.isEmpty() || foto.isEmpty()) {
                    throw new IllegalArgumentException("Preencha todos os campos!");
                }
                BigDecimal valorInicial = new BigDecimal(initialValueField.getText().trim());

                boolean sucesso = MarketRepository.addNewCoin(
                        nome, simbolo, foto, valorInicial);

                if (sucesso) {
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
            } catch (IllegalArgumentException ex) {
                statusLabel.setText(ex.getMessage());
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
                statusLabel
        );
        form.setStyle("-fx-padding: 20; -fx-background-color: #F8F8F8; -fx-background-radius: 10;");
        form.setMaxWidth(400);

        contentArea.getChildren().setAll(form);
    }

    @FXML
    private void handleViewStatistics() {
        TableView<Moeda> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Moeda, String> nomeCol = new TableColumn<>("Nome");
        nomeCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getNome()));

        TableColumn<Moeda, String> simboloCol = new TableColumn<>("Símbolo");
        simboloCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getSimbolo()));

        TableColumn<Moeda, String> valorCol = new TableColumn<>("Valor Atual (€)");
        valorCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        cd.getValue().getValorAtual().toPlainString()));

        TableColumn<Moeda, String> variacaoCol = new TableColumn<>("Variação 24h (%)");
        variacaoCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        cd.getValue().getVariacao24h().toPlainString()));

        TableColumn<Moeda, String> volumeCol = new TableColumn<>("Volume 24h");
        volumeCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        cd.getValue().getVolumeMercado().toPlainString()));

        TableColumn<Moeda, Void> actionCol = new TableColumn<>("Ações");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar   = new Button("Editar");
            private final Button btnEliminar = new Button("Eliminar");
            private final HBox pane = new HBox(8, btnEditar, btnEliminar);

            {
                btnEditar.setStyle("-fx-background-color: #4B3F72; -fx-text-fill: white;");
                btnEliminar.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white;");

                btnEditar.setOnAction(e -> editarMoeda(getIndex(), tableView));
                btnEliminar.setOnAction(e -> eliminarMoeda(getIndex(), tableView));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        tableView.getColumns().addAll(
                nomeCol, simboloCol, valorCol, variacaoCol, volumeCol, actionCol
        );

        List<Moeda> lista = MarketRepository.getTodasAsMoedas();
        tableView.setItems(FXCollections.observableArrayList(lista));

        contentArea.getChildren().setAll(tableView);
    }

    private void editarMoeda(int index, TableView<Moeda> tableView) {
        Moeda m = tableView.getItems().get(index);
        Dialog<Moeda> dialog = new Dialog<>();
        dialog.setTitle("Editar Moeda");

        TextField nomeField    = new TextField(m.getNome());
        TextField simboloField = new TextField(m.getSimbolo());
        TextField valorField   = new TextField(m.getValorAtual().toPlainString());

        VBox vb = new VBox(8,
                new Label("Nome:"), nomeField,
                new Label("Símbolo:"), simboloField,
                new Label("Valor Atual:"), valorField
        );
        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                m.setNome(nomeField.getText().trim());
                m.setSimbolo(simboloField.getText().trim());
                m.setValorAtual(new BigDecimal(valorField.getText().trim()));
                return m;
            }
            return null;
        });

        Optional<Moeda> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            try {
                MarketRepository.updateMoeda(updated);
                handleViewStatistics();
            } catch (SQLException ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Erro ao atualizar moeda: " + ex.getMessage())
                        .showAndWait();
            }
        });
    }

    private void eliminarMoeda(int index, TableView<Moeda> tableView) {
        Moeda m = tableView.getItems().get(index);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Deseja realmente eliminar " + m.getNome() + "?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Confirmar exclusão");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    MarketRepository.deleteMoeda(m.getIdMoeda());
                    tableView.getItems().remove(index);
                } catch (SQLException ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Erro ao eliminar moeda: " + ex.getMessage())
                            .showAndWait();
                }
            }
        });
    }

    @FXML
    private void handleLogOut() {
        SessaoAtual.limparSessao();
        NavigationHelper.goTo(Routes.LOGIN, false);
    }
}
