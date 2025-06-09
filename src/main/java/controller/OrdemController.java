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

    @FXML private ToggleGroup toggleTipoOrdem;

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
        this.tipoOrdem        = tipoOrdem;
        this.moedaSelecionada = moeda;
        this.userId           = userId;
        this.connection       = connection;

        labelTitulo.setText(tipoOrdem + " – " + moeda.getNome());
        labelPrecoAtual.setText("Preço atual: € " + moeda.getValorAtual());

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
            BigDecimal quantidade = new BigDecimal(txtQuantidade.getText().trim());
            if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }

            // determina modo e preço unitário
            String modo;
            BigDecimal precoUnitario;
            if (rbMarket.isSelected()) {
                modo = "market";
                precoUnitario = moedaSelecionada.getValorAtual();
            } else {
                modo = "limit";
                BigDecimal p = new BigDecimal(txtPrecoLimite.getText().trim());
                if (p.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
                precoUnitario = p;
            }

            // reserva fundos ou cripto
            if ("COMPRA".equalsIgnoreCase(tipoOrdem)) {
                BigDecimal custoTotal = precoUnitario.multiply(quantidade);
                BigDecimal meuSaldo = walletRepo.getSaldo(userId);
                if (meuSaldo.compareTo(custoTotal) < 0) {
                    mostrarErro("Saldo insuficiente em Euros.");
                    return;
                }
                if (!walletRepo.withdraw(userId, custoTotal)) {
                    mostrarErro("Erro ao bloquear saldo em Euros.");
                    return;
                }
            } else {
                BigDecimal minhaQtd = portfolioRepo.getQuantidade(userId, moedaSelecionada.getIdMoeda());
                if (minhaQtd.compareTo(quantidade) < 0) {
                    mostrarErro("Quantidade insuficiente na carteira de cripto.");
                    return;
                }
                if (!portfolioRepo.diminuirQuantidade(userId, moedaSelecionada.getIdMoeda(), quantidade)) {
                    mostrarErro("Erro ao bloquear quantidade de cripto.");
                    return;
                }
            }

            // cria objeto Ordem com os FKs corretos
            Ordem ordem = new Ordem();
            ordem.setUtilizador(new Utilizador(){{
                setIdUtilizador(userId);
            }});
            ordem.setMoeda(moedaSelecionada);

            // Mapeia texto → ID nas tabelas de domínio
            ordem.setIdTipoOrdem(
                    "COMPRA".equalsIgnoreCase(tipoOrdem) ? 1 : 2
            );
            ordem.setIdModo(
                    "market".equalsIgnoreCase(modo) ? 1 : 2
            );
            ordem.setIdStatus(
                    1 // 'ativa' na tabela OrdemStatus
            );

            ordem.setQuantidade(quantidade);
            ordem.setPrecoUnitarioEur(precoUnitario);
            ordem.setDataCriacao(LocalDateTime.now());
            ordem.setDataExpiracao(LocalDateTime.now().plusHours(24));

            // persiste e processa
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
