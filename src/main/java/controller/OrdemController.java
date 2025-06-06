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
import javafx.scene.control.TextField;
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

    private String tipoOrdem;       // "COMPRA" ou "VENDA"
    private Moeda moedaSelecionada;
    private int userId;             // Utilizador.idUtilizador
    private Connection connection;  // para TradeService

    // Repositórios auxiliares
    private final WalletRepository    walletRepo    = WalletRepository.getInstance();
    private final PortfolioRepository portfolioRepo = new PortfolioRepository();

    /**
     * Deve receber:
     *   - tipoOrdem: "COMPRA" ou "VENDA"
     *   - moeda: objeto Moeda com id, nome, valorAtual, etc.
     *   - userId: id do Utilizador
     *   - connection: Connection JDBC para TradeService e Repositórios
     */
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


        btnConfirmar.setDisable(true);


    }

    /**
     * Chamado sempre que o usuário digita algo no campo "Quantidade".
     * Habilita o botão "Confirmar" se for número > 0, senão mantém desabilitado.
     */
    @FXML
    private void onQuantidadeTyped() {
        String texto = txtQuantidade.getText().trim();
        try {
            BigDecimal qtd = new BigDecimal(texto);
            // Só habilita se for > 0
            btnConfirmar.setDisable(qtd.compareTo(BigDecimal.ZERO) <= 0);
        } catch (Exception e) {
            // Se não for número válido, mantém desabilitado
            btnConfirmar.setDisable(true);
        }
    }

    @FXML
    private void confirmarOrdem() {
        // 1) Garante conexão válida
        if (connection == null) {
            try {
                connection = DBConnection.getConnection();
            } catch (SQLException sqlEx) {
                mostrarErro("Não foi possível conectar ao banco de dados.");
                sqlEx.printStackTrace();
                return;
            }
        }

        try {
            // 2) Ler e validar quantidade escrita
            String texto = txtQuantidade.getText().trim();
            BigDecimal quantidade = new BigDecimal(texto);
            if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }


            BigDecimal precoAtual = moedaSelecionada.getValorAtual();

            // 3) Dependendo do tipo de ordem, validar e “bloquear” saldo ou cripto:
            if ("COMPRA".equalsIgnoreCase(tipoOrdem)) {
                BigDecimal custoTotal = precoAtual.multiply(quantidade);
                BigDecimal meuSaldo   = walletRepo.getSaldo(userId);
                if (meuSaldo.compareTo(custoTotal) < 0) {
                    mostrarErro("Saldo insuficiente em Euros.");
                    return;
                }
                // Bloquear (debitar) o valor total no saldo em euros
                boolean debitou = walletRepo.withdraw(userId, custoTotal);
                if (!debitou) {
                    mostrarErro("Erro ao bloquear saldo em Euros.");
                    return;
                }
            }
            else { // "VENDA"
                BigDecimal minhaQtd = portfolioRepo.getQuantidade(userId, moedaSelecionada.getIdMoeda());
                if (minhaQtd.compareTo(quantidade) < 0) {
                    mostrarErro("Quantidade insuficiente na carteira de cripto.");
                    return;
                }
                // Bloquear (levantar) a quantidade de cripto do portfólio
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

            // 4) Montar o objeto Ordem (será inserido no banco)
            Ordem ordem = new Ordem();
            Utilizador u = new Utilizador();
            u.setIdUtilizador(userId);
            ordem.setUtilizador(u);
            ordem.setMoeda(moedaSelecionada);
            ordem.setTipo(tipoOrdem.toLowerCase());    // "compra" ou "venda"
            ordem.setQuantidade(quantidade);
            ordem.setPrecoUnitarioEur(precoAtual);
            ordem.setDataCriacao(LocalDateTime.now());
            ordem.setDataExpiracao(LocalDateTime.now().plusHours(24));
            ordem.setStatus("ativa");

            // 5) Inserir a ordem na tabela 'Ordem'
            OrdemRepository ordemRepo = new OrdemRepository(connection);
            ordemRepo.inserirOrdem(ordem);

            // 6) Processar o matching de ordens
            TradeService tradeService = new TradeService(connection);
            if ("COMPRA".equalsIgnoreCase(tipoOrdem)) {
                tradeService.processarOrdemCompra(ordem);
            } else {
                tradeService.processarOrdemVenda(ordem);
            }

            // 7) Notificar usuário e fechar janela
            new Alert(Alert.AlertType.INFORMATION,
                    tipoOrdem + " executada com sucesso!")
                    .show();
            fecharJanela();
        }
        catch (NumberFormatException e) {
            mostrarErro("Insira uma quantidade válida (ex: 0.5, 10).");
        }
        catch (Exception e) {
            mostrarErro("Erro ao processar a ordem.");
            e.printStackTrace();
        }
    }

    private void mostrarErro(String mensagem) {
        new Alert(Alert.AlertType.ERROR, mensagem).show();
    }

    @FXML
    private void fecharJanela() {
        Stage stage = (Stage) btnConfirmar.getScene().getWindow();
        stage.close();
    }
}
