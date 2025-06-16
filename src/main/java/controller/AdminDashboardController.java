package controller;

import Repository.MarketRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.Moeda;
import utils.MarketSimulator;
import utils.NavigationHelper;
import utils.Routes;
import utils.SessaoAtual;
import model.Utilizador;
import Repository.UserRepository;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static utils.SessaoAtual.limparSessao;

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

    @FXML
    private void handleManageUsers(ActionEvent event) {
        showUsersTable();
    }

    private void showUsersTable() {
        TableView<Utilizador> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Utilizador, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getIdUtilizador())));

        TableColumn<Utilizador, String> nomeCol = new TableColumn<>("Nome");
        nomeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getNome()));

        TableColumn<Utilizador, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEmail()));

        TableColumn<Utilizador, String> perfilCol = new TableColumn<>("Perfil");
        perfilCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPerfil()));

        TableColumn<Utilizador, Void> actionCol = new TableColumn<>("Ações");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Editar");
            private final Button btnDelete = new Button("Eliminar");
            private final HBox actionBox = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.setStyle("-fx-background-color: #4B3F72; -fx-text-fill: white;");
                btnDelete.setStyle("-fx-background-color: #D9534F; -fx-text-fill: white;");

                btnEdit.setOnAction(e -> editUser(getIndex(), table));
                btnDelete.setOnAction(e -> deleteUser(getIndex(), table));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionBox);
            }
        });


        table.getColumns().addAll(idCol, nomeCol, emailCol, perfilCol, actionCol);

        List<Utilizador> lista = Repository.UserRepository.getTodos();
        table.setItems(FXCollections.observableArrayList(lista));

        contentArea.getChildren().setAll(table);
    }

    private void editUser(int index, TableView<Utilizador> table) {
        Utilizador u = table.getItems().get(index);

        Dialog<Utilizador> dialog = new Dialog<>();
        dialog.setTitle("Editar Utilizador");

        TextField nomeField = new TextField(u.getNome());
        TextField emailField = new TextField(u.getEmail());

        ComboBox<String> perfilBox = new ComboBox<>();
        perfilBox.getItems().addAll("User", "Admin");
        perfilBox.setValue(u.getPerfil());

        VBox vb = new VBox(10,
                new Label("Nome:"), nomeField,
                new Label("Email:"), emailField,
                new Label("Perfil:"), perfilBox);
        vb.setStyle("-fx-padding: 10;");

        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                u.setNome(nomeField.getText().trim());
                u.setEmail(emailField.getText().trim());
                u.setPerfil(perfilBox.getValue());
                return u;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            Optional<Integer> novoIdPerfil = new Repository.UserRepository().getPerfilId(updated.getPerfil());
            if (novoIdPerfil.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Perfil inválido").showAndWait();
                return;
            }

            boolean sucesso = Repository.UserRepository.atualizarUtilizador(
                    updated.getIdUtilizador(),
                    updated.getNome(),
                    updated.getEmail(),
                    novoIdPerfil.get()
            );

            if (sucesso) {
                showUsersTable(); // refrescar
            } else {
                new Alert(Alert.AlertType.ERROR, "Erro ao atualizar utilizador").showAndWait();
            }
        });
    }


    private void deleteUser(int index, TableView<Utilizador> table) {
        Utilizador u = table.getItems().get(index);

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Tens a certeza que queres eliminar o utilizador \"" + u.getNome() + "\"?",
                ButtonType.OK, ButtonType.CANCEL
        );
        confirm.setTitle("Confirmar Eliminação");

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                boolean sucesso = UserRepository.eliminarUtilizador(u.getIdUtilizador());
                if (sucesso) {
                    table.getItems().remove(index);
                } else {
                    new Alert(Alert.AlertType.ERROR, "Erro ao eliminar utilizador.").showAndWait();
                }
            }
        });
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
        initialValueField.setPromptText("Valor Base (ex: 50000.00)");

        Button submitButton = new Button("Adicionar Moeda");
        submitButton.setStyle("-fx-background-color: #4B3F72; -fx-text-fill: white;");
        Label statusLabel   = new Label();

        submitButton.setOnAction(e -> {
            String nome    = nameField.getText().trim();
            String simbolo = symbolField.getText().trim().toUpperCase();
            String foto    = imageField.getText().trim();

            try {
                if (nome.isEmpty() || simbolo.isEmpty() || foto.isEmpty()) {
                    throw new IllegalArgumentException("Preencha todos os campos!");
                }
                BigDecimal valorInicial = new BigDecimal(initialValueField.getText().trim());

                // Insere e recebe o novo ID
                OptionalInt optId = MarketRepository.addNewCoinReturnId(
                        nome, simbolo, foto, valorInicial

                );

                if (optId.isPresent()) {
                    int novoId = optId.getAsInt();

                    // Injeta no simulador
                    Moeda m = new Moeda(
                            novoId,
                            nome,
                            simbolo,
                            valorInicial.setScale(2, RoundingMode.HALF_UP),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO
                    );
                    MarketSimulator.getMoedasSimuladas().put(novoId, m);

                    statusLabel.setText("Moeda adicionada com sucesso!");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    nameField.clear(); symbolField.clear();
                    imageField.clear(); initialValueField.clear();
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
                new SimpleStringProperty(
                        Optional.ofNullable(cd.getValue().getNome()).orElse("—")
                )
        );

        TableColumn<Moeda, String> simboloCol = new TableColumn<>("Símbolo");
        simboloCol.setCellValueFactory(cd ->
                new SimpleStringProperty(
                        Optional.ofNullable(cd.getValue().getSimbolo()).orElse("—")
                )
        );

        TableColumn<Moeda, String> valorCol = new TableColumn<>("Valor Atual (€)");
        valorCol.setCellValueFactory(cd -> {
            BigDecimal v = cd.getValue().getValorAtual();
            return new SimpleStringProperty(
                    v != null ? v.toPlainString() : "—"
            );
        });

        TableColumn<Moeda, String> variacaoCol = new TableColumn<>("Variação 24h (%)");
        variacaoCol.setCellValueFactory(cd -> {
            BigDecimal v = cd.getValue().getVariacao24h();
            return new SimpleStringProperty(
                    v != null ? v.toPlainString() : "—"
            );
        });

        TableColumn<Moeda, String> volumeCol = new TableColumn<>("Volume 24h");
        volumeCol.setCellValueFactory(cd -> {
            BigDecimal v = cd.getValue().getVolume24h();
            return new SimpleStringProperty(
                    v != null ? v.toPlainString() : "—"
            );
        });

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

        List<Moeda> lista = MarketRepository.getMoedasOrdenadas(
                "", "Valor Atual", false
        );
        tableView.setItems(FXCollections.observableArrayList(lista));

        contentArea.getChildren().setAll(tableView);
    }

    private void editarMoeda(int index, TableView<Moeda> tableView) {
        Moeda m = tableView.getItems().get(index);
        Dialog<Moeda> dialog = new Dialog<>();
        dialog.setTitle("Editar Moeda");

        TextField nomeField    = new TextField(m.getNome());
        TextField simboloField = new TextField(m.getSimbolo());
        TextField valorField   = new TextField(
                m.getValorAtual() != null ? m.getValorAtual().toPlainString() : ""
        );

        VBox vb = new VBox(8,
                new Label("Nome:"), nomeField,
                new Label("Símbolo:"), simboloField,
                new Label("Valor Atual:"), valorField
        );
        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL
        );
        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                m.setNome(nomeField.getText().trim());
                m.setSimbolo(simboloField.getText().trim());
                try {
                    m.setValorAtual(new BigDecimal(valorField.getText().trim()));
                } catch (Exception ignore) {}
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
