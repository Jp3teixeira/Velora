package controller;

import Repository.MarketRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
    @FXML private TextField inputIdUser;
    @FXML private TextField inputNomeUser;
    @FXML private TextField inputEmailUser;
    @FXML private TextField inputPerfilUser;

    @FXML private TableView<Utilizador> tabelaUtilizadores;
    @FXML private TableColumn<Utilizador, String> colId;
    @FXML private TableColumn<Utilizador, String> colNome;
    @FXML private TableColumn<Utilizador, String> colEmail;
    @FXML private TableColumn<Utilizador, String> colPerfil;
    @FXML private TableColumn<Utilizador, String> colAtivo;
    private ComboBox<String> perfilBox;




    private final UserManagementController userManagement = new UserManagementController();






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
        VBox painel = new VBox(15);
        painel.setStyle("-fx-padding: 20");
        VBox.setVgrow(painel, Priority.ALWAYS);

        Label titulo = new Label("Gestão de Utilizadores");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        // ----- TABELA -----
        TableView<Utilizador> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Utilizador, String> colId = new TableColumn<>("ID");
        TableColumn<Utilizador, String> colNome = new TableColumn<>("Nome");
        TableColumn<Utilizador, String> colEmail = new TableColumn<>("Email");
        TableColumn<Utilizador, String> colPerfil = new TableColumn<>("Perfil");

        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getIdUtilizador())));
        colNome.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNome()));
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        colPerfil.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPerfil()));

        tabela.getColumns().addAll(colId, colNome, colEmail, colPerfil);
        tabela.getItems().setAll(new UserRepository().obterTodosUtilizadores());

        tabela.setMinHeight(400);
        tabela.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(tabela);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // ----- BOTÕES -----
        Button editarBtn    = new Button("Editar");
        Button desativarBtn = new Button("Desativar");   // só estes dois
        Button ativarBtn = new Button("Ativar");
        ativarBtn.setDisable(true);
        editarBtn.setDisable(true);
        desativarBtn.setDisable(true);

        HBox botoes = new HBox(10, editarBtn, desativarBtn, ativarBtn);




        // ----- FORMULÁRIO DE EDIÇÃO -----
        VBox formEdicao = new VBox(8);
        formEdicao.setVisible(false);

        TextField nomeField = new TextField();
        TextField emailField = new TextField();

        perfilBox = new ComboBox<>();
        perfilBox.getItems().addAll("user", "admin");
        perfilBox.setPromptText("Selecionar perfil");

        Button guardarBtn = new Button("Guardar Alterações");

        formEdicao.getChildren().addAll(
                new Label("Nome:"), nomeField,
                new Label("Email:"), emailField,
                new Label("Perfil:"), perfilBox,
                guardarBtn
        );

        final Utilizador[] sel = new Utilizador[1];

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, oldU, newU) -> {
            if (newU != null) {
                sel[0] = newU;
                editarBtn.setDisable(false);
                formEdicao.setVisible(false);

                if (newU.isAtivo()) {
                    desativarBtn.setDisable(false);
                    ativarBtn.setDisable(true);
                } else {
                    desativarBtn.setDisable(true);
                    ativarBtn.setDisable(false);
                }
            }
        });

        editarBtn.setOnAction(e -> {
            if (sel[0] != null) {
                nomeField.setText(sel[0].getNome());
                emailField.setText(sel[0].getEmail());
                perfilBox.setValue(sel[0].getPerfil().toLowerCase());
                formEdicao.setVisible(true);
            }
        });

        guardarBtn.setOnAction(e -> {
            try {
                String perfilSelecionado = perfilBox.getValue();
                if (perfilSelecionado == null || perfilSelecionado.isBlank()) {
                    new Alert(Alert.AlertType.ERROR, "Seleciona um perfil válido.").showAndWait();
                    return;
                }

                Optional<Integer> optId = new UserRepository().getPerfilId(perfilSelecionado);
                if (optId.isEmpty()) {
                    new Alert(Alert.AlertType.ERROR, "Perfil inválido.").showAndWait();
                    return;
                }

                new UserManagementController().editarUtilizadorSemPassword(
                        sel[0].getIdUtilizador(),
                        nomeField.getText(),
                        emailField.getText(),
                        optId.get()
                );

                tabela.getItems().setAll(new UserRepository().obterTodosUtilizadores());
                formEdicao.setVisible(false);
            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Erro ao atualizar utilizador:\n" + ex.getMessage()).showAndWait();
            }
        });

        desativarBtn.setOnAction(e -> {
            if (sel[0] != null) {
                // soft-delete (ativo = 0)
                new UserManagementController().desativarUtilizadorPorId(sel[0].getIdUtilizador());
                tabela.getItems().setAll(new UserRepository().obterTodosUtilizadores());
            }
        });

        ativarBtn.setOnAction(e -> {
            if (sel[0] != null) {
                new UserManagementController().ativarUtilizador(sel[0].getIdUtilizador());
                tabela.getItems().setAll(new UserRepository().obterTodosUtilizadores());
            }
        });



        tabela.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Utilizador item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (!item.isAtivo()) {
                    setStyle("-fx-background-color: #dcdcdc; -fx-text-fill: gray;"); // cinzento claro
                } else {
                    setStyle("");
                }
            }
        });


        painel.getChildren().addAll(titulo, scrollPane, botoes, formEdicao);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentArea.getChildren().setAll(painel);
    }





    private void editUser(int index, TableView<Utilizador> table) {
        Utilizador u = table.getItems().get(index);

        Dialog<Utilizador> dialog = new Dialog<>();
        dialog.setTitle("Editar Utilizador");

        TextField nomeField = new TextField(u.getNome());
        TextField emailField = new TextField(u.getEmail());

        perfilBox = new ComboBox<>();
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

            handleManageUsers(null);
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
