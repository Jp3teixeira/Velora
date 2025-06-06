package controller;

import Database.DBConnection;
import Repository.OrdemRepository;
import Repository.PortfolioRepository;
import Repository.WalletRepository;
import model.Moeda;
import model.Ordem;
import model.Utilizador;
import utils.TradeService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class OrdemController {

    @FXML private Label labelTitulo;
    @FXML private Label labelPrecoAtual;
    @FXML private TextField txtQuantidade;
    @FXML private Button btnConfirmar;

    @FXML private RadioButton rbMarket;
    @FXML private RadioButton rbLimit;
    @FXML private TextField txtPrecoLimite;
    @FXML private VBox boxPrecoLimite;

    @FXML private ToggleGroup toggleTipoOrdem; // injetado pelo FXMLLoader, definido em FXML

    private String tipoOrdem;       // "COMPRA" ou "VENDA"
    private Moeda moedaSelecionada;
    private int userId;             // Utilizador.idUtilizador
    private Connection connection;  // para TradeService

    private final WalletRepository    walletRepo    = WalletRepository.getInstance();
    private final PortfolioRepository portfolioRepo = new PortfolioRepository();

    public void configurar(String tipoOrdem,
                           Moeda moeda,
                           int userId,
                           Connection connection) {
        this.tipoOrdem        = tipoOrdem;
        this.moedaSelecionada = moeda;
        this.userId           = userId;
        this.connection       = connection;

        labelTitulo.setText(tipoOrdem + " – " + moeda.getNome());
        labelPrecoAtual.setText("Preço atual: € " + moeda.getValorAtual());

        // ToggleGroup e RadioButtons já definidos em FXML, apenas seleciona default
        rbMarket.setSelected(true);

        // Esconde o box de preço limite inicialmente
        boxPrecoLimite.setVisible(false);
        boxPrecoLimite.setManaged(false);

        // Listener para alternar entre Market e Limit
        rbMarket.setOnAction(e -> {
            boxPrecoLimite.setVisible(false);
            boxPrecoLimite.setManaged(false);
        });
        rbLimit.setOnAction(e -> {
            boxPrecoLimite.setVisible(true);
            boxPrecoLimite.setManaged(true);
        });

        btnConfirmar.setDisable(true);
    }

    @FXML
    private void onQuantidadeTyped() {
        String texto = txtQuantidade.getText().trim();
        try {
            BigDecimal qtd = new BigDecimal(texto);
            btnConfirmar.setDisable(qtd.compareTo(BigDecimal.ZERO) <= 0);
        } catch (Exception e) {
            btnConfirmar.setDisable(true);
        }
    }

    @FXML
    private void confirmarOrdem() {
        if (connection == null) {
            try {
                connection = DBConnection.getConnection();
            } catch (SQLException sqlEx) {
                mostrarErro("Não foi possível conectar ao banco de dados.");
                return;
            }
        }

        try {
            BigDecimal quantidade = new BigDecimal(txtQuantidade.getText().trim());
            if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }

            String modo;
            BigDecimal precoUnitario;
            if (rbMarket.isSelected()) {
                modo = "market";
                precoUnitario = moedaSelecionada.getValorAtual();
            } else {
                modo = "limit";
                String textoPreco = txtPrecoLimite.getText().trim();
                BigDecimal preco = new BigDecimal(textoPreco);
                if (preco.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new NumberFormatException();
                }
                precoUnitario = preco;
            }

            if ("COMPRA".equalsIgnoreCase(tipoOrdem)) {
                BigDecimal custoTotal = precoUnitario.multiply(quantidade);
                BigDecimal meuSaldo = walletRepo.getSaldo(userId);
                if (meuSaldo.compareTo(custoTotal) < 0) {
                    mostrarErro("Saldo insuficiente em Euros.");
                    return;
                }
                boolean debitou = walletRepo.withdraw(userId, custoTotal);
                if (!debitou) {
                    mostrarErro("Erro ao bloquear saldo em Euros.");
                    return;
                }
            } else {
                BigDecimal minhaQtd = portfolioRepo.getQuantidade(userId, moedaSelecionada.getIdMoeda());
                if (minhaQtd.compareTo(quantidade) < 0) {
                    mostrarErro("Quantidade insuficiente na carteira de cripto.");
                    return;
                }
                boolean decremented = portfolioRepo.decrementarQuantidade(
                        userId,
                        moedaSelecionada.getIdMoeda(),
                        quantidade
                );
                if (!decremented) {
                    mostrarErro("Erro ao bloquear quantidade de cripto.");
                    return;
                }
            }

            Ordem ordem = new Ordem();
            Utilizador u = new Utilizador();
            u.setIdUtilizador(userId);
            ordem.setUtilizador(u);
            ordem.setMoeda(moedaSelecionada);
            ordem.setTipo(tipoOrdem.toLowerCase());
            ordem.setModo(modo);
            ordem.setQuantidade(quantidade);
            ordem.setPrecoUnitarioEur(precoUnitario);
            ordem.setDataCriacao(LocalDateTime.now());
            ordem.setDataExpiracao(LocalDateTime.now().plusHours(24));
            ordem.setStatus("ativa");

            OrdemRepository ordemRepo = new OrdemRepository(connection);
            ordemRepo.inserirOrdem(ordem);

            TradeService tradeService = new TradeService(connection);
            if ("COMPRA".equalsIgnoreCase(tipoOrdem)) {
                tradeService.processarOrdemCompra(ordem);
            } else {
                tradeService.processarOrdemVenda(ordem);
            }

            new Alert(Alert.AlertType.INFORMATION,
                    tipoOrdem + " executada com sucesso!")
                    .show();
            fecharJanela();
        }
        catch (NumberFormatException e) {
            mostrarErro("Insira valores válidos (quantidade e, se limit, preço).");
        }
        catch (Exception e) {
            mostrarErro("Erro ao processar a ordem.");
            e.printStackTrace();
        }
    }

    @FXML
    private void fecharJanela() {
        Stage stage = (Stage) btnConfirmar.getScene().getWindow();
        stage.close();
    }

    private void mostrarErro(String mensagem) {
        new Alert(Alert.AlertType.ERROR, mensagem).show();
    }
}
