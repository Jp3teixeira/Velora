package utils;

import Repository.OrdemRepository;
import Repository.PortfolioRepository;
import Repository.TransacaoRepository;
import Repository.WalletRepository;
import model.Ordem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class TradeService {

    private final OrdemRepository ordemRepo;
    private final TransacaoRepository transacaoRepo;
    private final PortfolioRepository portfolioRepo;
    private final WalletRepository walletRepo;

    public TradeService(Connection connection) {
        this.ordemRepo     = new OrdemRepository(connection);
        this.transacaoRepo = new TransacaoRepository();
        this.portfolioRepo = new PortfolioRepository();
        this.walletRepo    = WalletRepository.getInstance();
    }


    /**
     * “Market buy”: tenta consumir toda a fila de vendas FIFO.
     */
    public void processarOrdemCompra(Ordem novaCompra) throws SQLException {
        BigDecimal restante = novaCompra.getQuantidade();

        // 1) Busca todas as ordens de VENDA ativas, por ordem de criação (FIFO)
        List<Ordem> ordensVenda = ordemRepo.obterOrdensPendentes(
                novaCompra.getMoeda().getIdMoeda(),
                "venda"
        );

        for (Ordem venda : ordensVenda) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal disponivelVenda = venda.getQuantidade();
            BigDecimal matchQtde = restante.min(disponivelVenda);

            // Preço de execução = o preço que a própria ordem de venda declarou
            BigDecimal precoExecucao = venda.getPrecoUnitarioEur().setScale(8, RoundingMode.HALF_UP);

            // 2.a) Insere transação “compra” na conta do comprador
            transacaoRepo.inserirTransacao(
                    novaCompra.getUtilizador().getIdUtilizador(),
                    novaCompra.getMoeda().getIdMoeda(),
                    "compra",
                    matchQtde.setScale(8, RoundingMode.HALF_UP),
                    precoExecucao
            );
            // 2.b) Insere transação “venda” na conta do vendedor
            transacaoRepo.inserirTransacao(
                    venda.getUtilizador().getIdUtilizador(),
                    venda.getMoeda().getIdMoeda(),
                    "venda",
                    matchQtde.setScale(8, RoundingMode.HALF_UP),
                    precoExecucao
            );

            // 3) Atualiza a ordem de venda: diminui quantidade e, se zero, marca “executada”
            BigDecimal novaVendaRestante = disponivelVenda.subtract(matchQtde);
            venda.setQuantidade(novaVendaRestante);
            venda.setStatus(
                    novaVendaRestante.compareTo(BigDecimal.ZERO) == 0 ?
                            "executada" : "ativa"
            );
            ordemRepo.atualizarOrdem(venda);

            // 4) Credita crypto ao comprador (Portfolio) e euros ao vendedor (Wallet)
            portfolioRepo.incrementarQuantidade(
                    novaCompra.getUtilizador().getIdUtilizador(),
                    novaCompra.getMoeda().getIdMoeda(),
                    matchQtde
            );
            BigDecimal valorParaVendedor = matchQtde.multiply(precoExecucao);
            walletRepo.deposit(
                    venda.getUtilizador().getIdUtilizador(),
                    valorParaVendedor
            );

            // 5) Atualiza restante do BUY
            restante = restante.subtract(matchQtde);
        }

        // 6) Atualiza a ordem de compra: permanece ativa ou é finalizada
        novaCompra.setQuantidade(restante);
        novaCompra.setStatus(
                restante.compareTo(BigDecimal.ZERO) == 0 ?
                        "executada" : "ativa"
        );
        ordemRepo.atualizarOrdem(novaCompra);

        // 7) Se ainda sobrar BUY, devolve os euros “não gastos” ao comprador
        if (restante.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal devolve = restante.multiply(novaCompra.getPrecoUnitarioEur())
                    .setScale(8, RoundingMode.HALF_UP);
            walletRepo.deposit(
                    novaCompra.getUtilizador().getIdUtilizador(),
                    devolve
            );
        }
    }

    /**
     * “Market sell”: tenta consumir toda a fila de compras FIFO.
     */
    public void processarOrdemVenda(Ordem novaVenda) throws SQLException {
        BigDecimal restante = novaVenda.getQuantidade();

        // 1) Procura todas as ordens de COMPRA ativas, por data de criação (FIFO)
        List<Ordem> ordensCompra = ordemRepo.obterOrdensPendentes(
                novaVenda.getMoeda().getIdMoeda(),
                "compra"
        );

        for (Ordem compra : ordensCompra) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal disponivelCompra = compra.getQuantidade();
            BigDecimal matchQtde = restante.min(disponivelCompra);

            // Preço de execução = preço que a própria ordem de compra definiu
            BigDecimal precoExecucao = compra.getPrecoUnitarioEur().setScale(8, RoundingMode.HALF_UP);

            // 2.a) Insere transação “venda” para o vendedor
            transacaoRepo.inserirTransacao(
                    novaVenda.getUtilizador().getIdUtilizador(),
                    novaVenda.getMoeda().getIdMoeda(),
                    "venda",
                    matchQtde.setScale(8, RoundingMode.HALF_UP),
                    precoExecucao
            );
            // 2.b) Insere transação “compra” para o comprador
            transacaoRepo.inserirTransacao(
                    compra.getUtilizador().getIdUtilizador(),
                    compra.getMoeda().getIdMoeda(),
                    "compra",
                    matchQtde.setScale(8, RoundingMode.HALF_UP),
                    precoExecucao
            );

            // 3) Atualiza a ordem de compra: diminui quantidade e altera status
            BigDecimal novaCompraRestante = disponivelCompra.subtract(matchQtde);
            compra.setQuantidade(novaCompraRestante);
            compra.setStatus(
                    novaCompraRestante.compareTo(BigDecimal.ZERO) == 0 ?
                            "executada" : "ativa"
            );
            ordemRepo.atualizarOrdem(compra);

            // 4) Credita euros ao vendedor e crypto ao comprador
            BigDecimal valorParaVendedor = matchQtde.multiply(precoExecucao);
            walletRepo.deposit(
                    novaVenda.getUtilizador().getIdUtilizador(),
                    valorParaVendedor
            );
            portfolioRepo.incrementarQuantidade(
                    compra.getUtilizador().getIdUtilizador(),
                    compra.getMoeda().getIdMoeda(),
                    matchQtde
            );

            // 5) Atualiza restante do SELL
            restante = restante.subtract(matchQtde);
        }

        // 6) Atualiza a ordem de venda: se sobrar, fica “ativa”, senão “executada”
        novaVenda.setQuantidade(restante);
        novaVenda.setStatus(
                restante.compareTo(BigDecimal.ZERO) == 0 ?
                        "executada" : "ativa"
        );
        ordemRepo.atualizarOrdem(novaVenda);

        // 7) Se ainda sobrar SELL, devolve a crypto não vendida ao portfólio do vendedor
        if (restante.compareTo(BigDecimal.ZERO) > 0) {
            portfolioRepo.incrementarQuantidade(
                    novaVenda.getUtilizador().getIdUtilizador(),
                    novaVenda.getMoeda().getIdMoeda(),
                    restante
            );
        }
    }
}
