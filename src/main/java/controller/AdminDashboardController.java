package controller;


import Repository.MarketRepository;
import Repository.TransacaoRepository;
import Repository.UserRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import model.Moeda;
import model.Transacao;
import model.Utilizador;
import model.Perfil;
import utils.MarketSimulator;
import utils.NavigationHelper;
import utils.Routes;
import utils.SessaoAtual;
import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class AdminDashboardController {

    @FXML private Button adminButton;
    @FXML private StackPane contentArea;

    // Usuários
    @FXML private TableView<Utilizador> tabelaUtilizadores;
    @FXML private TableColumn<Utilizador, String> colId;
    @FXML private TableColumn<Utilizador, String> colNome;
    @FXML private TableColumn<Utilizador, String> colEmail;
    @FXML private TableColumn<Utilizador, String> colPerfil;
    @FXML private TableColumn<Utilizador, Void> colCsv;

    private ComboBox<Perfil> perfilBox;

    private final UserManagementController userManagement = new UserManagementController();

    @FXML
    public void initialize() {
        boolean hasPermission = SessaoAtual.tipo != null &&
                (SessaoAtual.tipo.equalsIgnoreCase(Perfil.ADMIN.name()) || SessaoAtual.isSuperAdmin);
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

        // Configura colunas
        colId.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().getId()))
        );
        colNome.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getNome())
        );
        colEmail.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getEmail())
        );
        colPerfil.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getPerfil().name())
        );
        colCsv.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button("CSV");
            {
                btn.setStyle("-fx-background-color: #4B3F72; -fx-text-fill: white; -fx-font-size: 11;");
                btn.setOnAction(e -> {
                    Utilizador u = getTableView().getItems().get(getIndex());
                    exportarTransacoesUsuarioCSV(u);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        tabelaUtilizadores.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabelaUtilizadores.getColumns().setAll(colId, colNome, colEmail, colPerfil, colCsv);
        tabelaUtilizadores.setItems(FXCollections.observableArrayList(
                new UserRepository().getAll()
        ));
        tabelaUtilizadores.setMinHeight(400);
        VBox.setVgrow(tabelaUtilizadores, Priority.ALWAYS);

        // Botões
        Button editarBtn    = new Button("Editar");
        Button desativarBtn = new Button("Desativar");
        Button ativarBtn   = new Button("Ativar");
        editarBtn.setDisable(true);
        desativarBtn.setDisable(true);
        ativarBtn.setDisable(true);
        HBox botoes = new HBox(10, editarBtn, desativarBtn, ativarBtn);

        // Formulário de edição
        VBox formEdicao = new VBox(8);
        formEdicao.setVisible(false);
        TextField nomeField  = new TextField();
        TextField emailField = new TextField();
        perfilBox = new ComboBox<>();
        perfilBox.getItems().setAll(Perfil.values());
        perfilBox.setPromptText("Selecionar perfil");
        Button guardarBtn = new Button("Guardar Alterações");
        formEdicao.getChildren().addAll(
                new Label("Nome:"), nomeField,
                new Label("Email:"), emailField,
                new Label("Perfil:"), perfilBox,
                guardarBtn
        );

        final Utilizador[] sel = new Utilizador[1];
        tabelaUtilizadores.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldU, newU) -> {
                    editarBtn.setDisable(newU == null);
                    if (newU != null) {
                        sel[0] = newU;
                        desativarBtn.setDisable(!newU.isAtivo());
                        ativarBtn.setDisable(newU.isAtivo());
                        formEdicao.setVisible(false);
                    }
                });

        editarBtn.setOnAction(e -> {
            nomeField.setText(sel[0].getNome());
            emailField.setText(sel[0].getEmail());
            perfilBox.setValue(sel[0].getPerfil());
            formEdicao.setVisible(true);
        });

        guardarBtn.setOnAction(e -> {
            Perfil escolhido = perfilBox.getValue();
            if (escolhido == null) {
                new Alert(Alert.AlertType.ERROR, "Seleciona um perfil válido.").showAndWait();
                return;
            }
            Optional<Integer> optId = new UserRepository().getPerfilId(escolhido.name());
            if (optId.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Perfil inválido.").showAndWait();
                return;
            }
            userManagement.editarUtilizadorSemPassword(
                    sel[0].getId(),
                    nomeField.getText().trim(),
                    emailField.getText().trim(),
                    optId.get()
            );
            tabelaUtilizadores.setItems(FXCollections.observableArrayList(
                    new UserRepository().getAll()
            ));
            formEdicao.setVisible(false);
        });

        desativarBtn.setOnAction(e -> {
            userManagement.desativarUtilizadorPorId(sel[0].getId());
            tabelaUtilizadores.setItems(FXCollections.observableArrayList(
                    new UserRepository().getAll()
            ));
        });

        ativarBtn.setOnAction(e -> {
            userManagement.ativarUtilizador(sel[0].getId());
            tabelaUtilizadores.setItems(FXCollections.observableArrayList(
                    new UserRepository().getAll()
            ));
        });

        tabelaUtilizadores.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Utilizador item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !item.isAtivo()) {
                    setStyle("-fx-background-color: #dcdcdc; -fx-text-fill: gray;");
                } else {
                    setStyle("");
                }
            }
        });

        painel.getChildren().setAll(titulo, tabelaUtilizadores, botoes, formEdicao);
        contentArea.getChildren().setAll(painel);
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
            try {
                String nome    = nameField.getText().trim();
                String simbolo = symbolField.getText().trim().toUpperCase();
                String foto    = imageField.getText().trim();
                if (nome.isEmpty() || simbolo.isEmpty() || foto.isEmpty()) {
                    throw new IllegalArgumentException("Preencha todos os campos!");
                }
                BigDecimal valorInicial = new BigDecimal(initialValueField.getText().trim());

                OptionalInt optId = MarketRepository.addNewCoinReturnId(
                        nome, simbolo, foto, valorInicial
                );
                if (optId.isPresent()) {
                    int novoId = optId.getAsInt();
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
                new SimpleStringProperty(Optional.ofNullable(cd.getValue().getNome()).orElse("—"))
        );
        TableColumn<Moeda, String> simboloCol = new TableColumn<>("Símbolo");
        simboloCol.setCellValueFactory(cd ->
                new SimpleStringProperty(Optional.ofNullable(cd.getValue().getSimbolo()).orElse("—"))
        );
        TableColumn<Moeda, String> valorCol = new TableColumn<>("Valor Atual (€)");
        valorCol.setCellValueFactory(cd -> {
            BigDecimal v = cd.getValue().getValorAtual();
            return new SimpleStringProperty(v != null ? v.toPlainString() : "—");
        });
        TableColumn<Moeda, String> variacaoCol = new TableColumn<>("Variação 24h (%)");
        variacaoCol.setCellValueFactory(cd -> {
            BigDecimal v = cd.getValue().getVariacao24h();
            return new SimpleStringProperty(v != null ? v.toPlainString() : "—");
        });
        TableColumn<Moeda, String> volumeCol = new TableColumn<>("Volume 24h");
        volumeCol.setCellValueFactory(cd -> {
            BigDecimal v = cd.getValue().getVolume24h();
            return new SimpleStringProperty(v != null ? v.toPlainString() : "—");
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

        tableView.getColumns().setAll(
                nomeCol, simboloCol, valorCol, variacaoCol, volumeCol, actionCol
        );
        tableView.setItems(FXCollections.observableArrayList(
                MarketRepository.getMoedasOrdenadas("", "Valor Atual", false)
        ));
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
                try {
                    m.setValorAtual(new BigDecimal(valorField.getText().trim()));
                } catch (Exception ignore) {}
                return m;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            try {
                MarketRepository.updateMoeda(updated);
                handleViewStatistics();
            } catch (SQLException ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Erro ao atualizar moeda: " + ex.getMessage()
                ).showAndWait();
            }
        });
    }

    private void eliminarMoeda(int index, TableView<Moeda> tableView) {
        Moeda m = tableView.getItems().get(index);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Deseja realmente eliminar " + m.getNome() + "?",
                ButtonType.OK, ButtonType.CANCEL
        );
        confirm.setTitle("Confirmar exclusão");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    MarketRepository.deleteMoeda(m.getId());
                    tableView.getItems().remove(index);
                } catch (SQLException ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Erro ao eliminar moeda: " + ex.getMessage()
                    ).showAndWait();
                }
            }
        });
    }

    private void exportarTransacoesUsuarioCSV(Utilizador user) {
        try {
            TransacaoRepository repo = new TransacaoRepository();
            List<Transacao> transacoes = repo.listarPorUsuario(user.getId());
            if (transacoes.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION,
                        "O utilizador \"" + user.getNome() + "\" não tem transações."
                ).showAndWait();
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exportar Transações para CSV");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV", "*.csv")
            );
            chooser.setInitialFileName("transacoes_" +
                    user.getNome().replaceAll("\\s+", "_") + ".csv"
            );
            File file = chooser.showSaveDialog(contentArea.getScene().getWindow());
            if (file == null) return;

            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                writer.println("Data,Moeda,Tipo,Quantidade,Total (€)");
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                for (Transacao tx : transacoes) {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            tx.getDataHora().format(fmt),
                            tx.getMoeda().getNome(),
                            tx.getTipo(),
                            tx.getQuantidade().setScale(8, RoundingMode.HALF_UP),
                            tx.getTotalEur().setScale(2, RoundingMode.HALF_UP)
                    );
                }
            }
            new Alert(Alert.AlertType.INFORMATION,
                    "Transações exportadas para:\n" + file.getAbsolutePath()
            ).showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Erro ao exportar transações:\n" + ex.getMessage()
            ).showAndWait();
        }
    }

    @FXML
    private void handleLogOut() {
        SessaoAtual.limparSessao();
        NavigationHelper.goTo(Routes.LOGIN, false);
    }
}
