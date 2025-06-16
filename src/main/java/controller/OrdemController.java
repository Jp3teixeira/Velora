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
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

public class OrdemController {

    @FXML private Label labelTitulo;
    @FXML private Label labelPrecoAtual;
    @FXML private Label labelSaldo;
    @FXML private Label lblErro;

    @FXML private RadioButton rbMarket, rbLimit;
    @FXML private TextField txtPrecoLimite, txtQuantidade;
    @FXML private VBox boxPrecoLimite;
    @FXML private Button btnConfirmar;

    private String tipoOrdem;
    private Moeda moeda;
    private int userId;
    private Connection conn;

    private final WalletRepository    walletRepo    = WalletRepository.getInstance();
    private final PortfolioRepository portfolioRepo = new PortfolioRepository();

    public void configurar(String tipoOrdem, Moeda moeda, int userId, Connection conn) {
        this.tipoOrdem = tipoOrdem.toUpperCase();
        this.moeda     = moeda;
        this.userId    = userId;
        this.conn      = conn;

        labelTitulo.setText(this.tipoOrdem + " – " + moeda.getNome());
        labelPrecoAtual.setText("Preço atual: € " + moeda.getValorAtual());
        labelSaldo.setText("Saldo disponível: € " + walletRepo.getSaldo(userId));

        rbMarket.setSelected(true);
        toggleLimit(false);
        clearErro();

        rbMarket.setOnAction(e -> { toggleLimit(false); clearErro(); });
        rbLimit .setOnAction(e -> { toggleLimit(true);  clearErro(); });
    }

    private void toggleLimit(boolean show) {
        boxPrecoLimite.setVisible(show);
        boxPrecoLimite.setManaged(show);
        txtPrecoLimite.clear();
        txtQuantidade.clear();
        btnConfirmar.setDisable(true);
    }

    @FXML
    private void onQuantidadeTyped() {
        clearErro();
        boolean ok = true;
        try {
            new BigDecimal(txtQuantidade.getText().trim())
                    .compareTo(BigDecimal.ZERO);
        } catch (Exception e) {
            ok = false;
        }
        if (rbLimit.isSelected()) {
            try {
                new BigDecimal(txtPrecoLimite.getText().trim())
                        .compareTo(BigDecimal.ZERO);
            } catch (Exception e) {
                ok = false;
            }
        }
        btnConfirmar.setDisable(!ok);
    }

    @FXML
    private void confirmarOrdem() {
        clearErro();
        try {
            if (conn == null) conn = DBConnection.getConnection();

            BigDecimal qtd = new BigDecimal(txtQuantidade.getText().trim());
            BigDecimal price = rbMarket.isSelected()
                    ? moeda.getValorAtual()
                    : new BigDecimal(txtPrecoLimite.getText().trim());

            // Validações
            if (qtd.compareTo(BigDecimal.ZERO) <= 0) {
                showErro("Quantidade deve ser > 0");
                return;
            }
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                showErro("Preço deve ser > 0");
                return;
            }

            if ("COMPRA".equals(tipoOrdem)) {
                BigDecimal custo = price.multiply(qtd);
                if (walletRepo.getSaldo(userId).compareTo(custo) < 0 ||
                        !walletRepo.withdraw(userId, custo)) {
                    showErro("Saldo insuficiente");
                    return;
                }
            } else {
                if (portfolioRepo.getQuantidade(userId, moeda.getIdMoeda())
                        .compareTo(qtd) < 0 ||
                        !portfolioRepo.diminuirQuantidade(userId, moeda.getIdMoeda(), qtd)) {
                    showErro("Crypto insuficiente");
                    return;
                }
            }

            // Cria e persiste ordem
            Ordem ord = new Ordem();
            ord.setUtilizador(new Utilizador() {{ setIdUtilizador(userId); }});
            ord.setMoeda(moeda);
            ord.setQuantidade(qtd);
            ord.setPrecoUnitarioEur(price);
            ord.setDataCriacao(LocalDateTime.now());
            ord.setDataExpiracao(LocalDateTime.now().plusHours(24));

            OrdemRepository repo = new OrdemRepository(conn);
            ord.setIdTipoOrdem(repo.obterIdTipoOrdem(tipoOrdem.toLowerCase()));
            ord.setIdModo     (repo.obterIdModo    (rbMarket.isSelected() ? "market" : "limit"));
            ord.setIdStatus   (repo.obterIdStatus  ("ativa"));

            Optional<Integer> newId = repo.inserirOrdem(ord);
            if (newId.isEmpty()) {
                showErro("Falha ao criar ordem");
                return;
            }
            ord.setIdOrdem(newId.get());

            // Matching
            TradeService svc = new TradeService(conn);
            if ("COMPRA".equals(tipoOrdem)) {
                svc.processarOrdemCompra(ord);
            } else {
                svc.processarOrdemVenda(ord);
            }

            fecharJanela();
        } catch (SQLException ex) {
            showErro("Erro BD: " + ex.getMessage());
        } catch (Exception ex) {
            showErro("Erro interno");
        }
    }

    @FXML
    private void fecharJanela() {
        Stage st = (Stage) btnConfirmar.getScene().getWindow();
        st.close();
    }

    private void showErro(String msg) {
        lblErro.setText(msg);
        lblErro.setVisible(true);
        lblErro.setManaged(true);
    }

    private void clearErro() {
        lblErro.setText("");
        lblErro.setVisible(false);
        lblErro.setManaged(false);
    }
}
