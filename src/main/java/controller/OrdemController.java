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

    private String tipoOrdem;       // "COMPRA" ou "VENDA"
    private Moeda moedaSelecionada;
    private int userId;
    private Connection connection;

    private final WalletRepository    walletRepo    = WalletRepository.getInstance();
    private final PortfolioRepository portfolioRepo = new PortfolioRepository();

    public void configurar(String tipoOrdem,
                           Moeda moeda,
                           int userId,
                           Connection connection) {
        this.tipoOrdem        = tipoOrdem.toUpperCase();
        this.moedaSelecionada = moeda;
        this.userId           = userId;
        this.connection       = connection;

        labelTitulo.setText(this.tipoOrdem + " – " + moeda.getNome());
        labelPrecoAtual.setText("Preço atual: € " + moeda.getValorAtual());

        // inicialmente market
        rbMarket.setSelected(true);
        boxPrecoLimite.setVisible(false);
        boxPrecoLimite.setManaged(false);

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
        try {
            BigDecimal qtd = new BigDecimal(txtQuantidade.getText().trim());
            btnConfirmar.setDisable(qtd.compareTo(BigDecimal.ZERO) <= 0);
        } catch (Exception e) {
            btnConfirmar.setDisable(true);
        }
    }

    @FXML
    private void confirmarOrdem() {
        // garante conexão
        if (connection == null) {
            try {
                connection = DBConnection.getConnection();
            } catch (SQLException ex) {
                mostrarErro("Não foi possível conectar ao banco de dados.");
                return;
            }
        }

        try {
            // lê quantidade
            BigDecimal quantidade = new BigDecimal(txtQuantidade.getText().trim());
            if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }

            // determina modo e preço unitário
            String modo = rbMarket.isSelected() ? "market" : "limit";
            BigDecimal precoUnitario = rbMarket.isSelected()
                    ? moedaSelecionada.getValorAtual()
                    : new BigDecimal(txtPrecoLimite.getText().trim());

            if (precoUnitario.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }

            // reserva fundos ou cripto
            if ("COMPRA".equals(tipoOrdem)) {
                BigDecimal custoTotal = precoUnitario.multiply(quantidade);
                if (walletRepo.getSaldo(userId).compareTo(custoTotal) < 0
                        || !walletRepo.withdraw(userId, custoTotal)) {
                    mostrarErro("Saldo insuficiente em Euros.");
                    return;
                }
            } else {
                if (portfolioRepo.getQuantidade(userId, moedaSelecionada.getIdMoeda())
                        .compareTo(quantidade) < 0
                        || !portfolioRepo.diminuirQuantidade(userId, moedaSelecionada.getIdMoeda(), quantidade)) {
                    mostrarErro("Quantidade insuficiente na carteira de cripto.");
                    return;
                }
            }

            // monta objeto Ordem
            Ordem ordem = new Ordem();
            ordem.setUtilizador(new Utilizador() {{ setIdUtilizador(userId); }});
            ordem.setMoeda(moedaSelecionada);
            ordem.setQuantidade(quantidade);
            ordem.setPrecoUnitarioEur(precoUnitario);
            ordem.setDataCriacao(LocalDateTime.now());
            ordem.setDataExpiracao(LocalDateTime.now().plusHours(24));

            // busca FKs pelo repositório
            OrdemRepository repo = new OrdemRepository(connection);
            int idTipo = repo.obterIdTipoOrdem(tipoOrdem.toLowerCase());
            int idModo = repo.obterIdModo(modo.toLowerCase());
            int idStatus = repo.obterIdStatus("ativa");

            ordem.setIdTipoOrdem(idTipo);
            ordem.setIdModo(idModo);
            ordem.setIdStatus(idStatus);

            // persiste
            repo.inserirOrdem(ordem);

            // processa matching
            TradeService tradeService = new TradeService(connection);
            if ("COMPRA".equals(tipoOrdem)) {
                tradeService.processarOrdemCompra(ordem);
            } else {
                tradeService.processarOrdemVenda(ordem);
            }

            new Alert(Alert.AlertType.INFORMATION,
                    tipoOrdem + " executada com sucesso!")
                    .show();
            fecharJanela();

        } catch (NumberFormatException e) {
            mostrarErro("Insira valores válidos (quantidade e preço).");
        } catch (SQLException e) {
            mostrarErro("Erro no banco de dados: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            mostrarErro("Erro ao processar a ordem.");
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
