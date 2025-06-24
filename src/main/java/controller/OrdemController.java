package controller;

import Database.DBConnection;
import Repository.OrdemRepository;
import Repository.PortfolioRepository;
import Repository.WalletRepository;
import model.Moeda;
import model.Ordem;
import model.OrdemModo;
import model.OrdemStatus;
import model.OrdemTipo;
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
        // usa getSaldoPorUtilizador em vez de getSaldo
        labelSaldo.setText("Saldo disponível: € " + walletRepo.getSaldoPorUtilizador(userId));

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
            new BigDecimal(txtQuantidade.getText().trim()).compareTo(BigDecimal.ZERO);
            if (rbLimit.isSelected()) {
                new BigDecimal(txtPrecoLimite.getText().trim()).compareTo(BigDecimal.ZERO);
            }
        } catch (Exception e) {
            ok = false;
        }
        btnConfirmar.setDisable(!ok);
    }

    @FXML
    private void confirmarOrdem() {
        clearErro();
        try {
            if (conn == null) conn = DBConnection.getConnection();

            BigDecimal qtd   = new BigDecimal(txtQuantidade.getText().trim());
            BigDecimal price = rbMarket.isSelected()
                    ? moeda.getValorAtual()
                    : new BigDecimal(txtPrecoLimite.getText().trim());

            // validações
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
                if (walletRepo.getSaldoPorUtilizador(userId).compareTo(custo) < 0
                        || !walletRepo.withdraw(userId, custo)) {
                    showErro("Saldo insuficiente");
                    return;
                }
            } else {
                if (portfolioRepo.getQuantidade(userId, moeda.getId())
                        .compareTo(qtd) < 0
                        || !portfolioRepo.diminuirQuantidade(userId, moeda.getId(), qtd)) {
                    showErro("Crypto insuficiente");
                    return;
                }
            }

            // monta ordem
            Ordem ord = new Ordem();
            ord.setUtilizador(new Utilizador() {{ setId(userId); }});
            ord.setMoeda(moeda);
            ord.setQuantidade(qtd);
            ord.setPrecoUnitarioEur(price);
            ord.setDataCriacao(LocalDateTime.now());
            ord.setDataExpiracao(LocalDateTime.now().plusHours(24));

            // define enums e busca IDs
            OrdemTipo tipoEnum = OrdemTipo.valueOf(tipoOrdem);
            ord.setTipoOrdem(tipoEnum);
            ord.setIdTipoOrdem(new OrdemRepository(conn).obterIdTipoOrdem(tipoEnum.name()));

            OrdemModo modoEnum = rbMarket.isSelected() ? OrdemModo.MARKET : OrdemModo.LIMIT;
            ord.setModo(modoEnum);
            ord.setIdModo(new OrdemRepository(conn).obterIdModo(modoEnum.name()));

            ord.setStatus(OrdemStatus.ATIVA);
            ord.setIdStatus(new OrdemRepository(conn).obterIdStatus(ord.getStatus().name()));

            // persiste usando save()
            OrdemRepository repo = new OrdemRepository(conn);
            if (!repo.save(ord)) {
                showErro("Falha ao criar ordem");
                return;
            }

            // processa matching
            TradeService svc = new TradeService(conn);
            if (tipoEnum == OrdemTipo.COMPRA) svc.processarOrdemCompra(ord);
            else                              svc.processarOrdemVenda(ord);

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
