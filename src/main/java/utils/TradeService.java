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
     * Processa uma ordem de compra, seja market ou limit.
     */
    public void processarOrdemCompra(Ordem novaCompra) throws SQLException {
        BigDecimal restante = novaCompra.getQuantidade();
        String modo = novaCompra.getModo();                  // "market" ou "limit"
        BigDecimal precoLimite = novaCompra.getPrecoUnitarioEur();

        // 1) Busca ordens de venda que atendam ao modo e preço-limite
        List<Ordem> ordensVenda = ordemRepo.obterOrdensPendentes(
                novaCompra.getMoeda().getIdMoeda(),
                "venda",
                modo,
                precoLimite
        );

        // 2) Executa matching enquanto houver quantidade e ordens compatíveis
        for (Ordem venda : ordensVenda) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal disponivelVenda = venda.getQuantidade();
            BigDecimal matchQtde = restante.min(disponivelVenda);

            // Preço de execução = preço que a ordem de venda declarou
            BigDecimal precoExecucao = venda.getPrecoUnitarioEur().setScale(8, RoundingMode.HALF_UP);

            // Insere transação “compra” e “venda”
            transacaoRepo.inserirTransacao(
                    novaCompra.getUtilizador().getIdUtilizador(),
                    novaCompra.getMoeda().getIdMoeda(),
                    "compra",
                    matchQtde.setScale(8, RoundingMode.HALF_UP),
                    precoExecucao
            );
            transacaoRepo.inserirTransacao(
                    venda.getUtilizador().getIdUtilizador(),
                    venda.getMoeda().getIdMoeda(),
                    "venda",
                    matchQtde.setScale(8, RoundingMode.HALF_UP),
                    precoExecucao
            );

            // Atualiza ordem de venda (quantidade e status)
            BigDecimal novaVendaRestante = disponivelVenda.subtract(matchQtde);
            venda.setQuantidade(novaVendaRestante);
            venda.setStatus(
                    novaVendaRestante.compareTo(BigDecimal.ZERO) == 0 ?
                            "executada" : "ativa"
            );
            ordemRepo.atualizarOrdem(venda);

            // Credita cripto ao comprador e euros ao vendedor
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

            restante = restante.subtract(matchQtde);
        }

        // 3) Atualiza a própria ordem de compra
        novaCompra.setQuantidade(restante);
        // Se for limit e não casa nada, fica "ativa". Se casas tudo, "executada".
        if (restante.compareTo(BigDecimal.ZERO) == 0) {
            novaCompra.setStatus("executada");
        } else if ("market".equalsIgnoreCase(modo)) {
            // Para market, devolve o não usado e marca como executada
            novaCompra.setStatus("executada");
            BigDecimal devolve = restante.multiply(precoLimite)
                    .setScale(8, RoundingMode.HALF_UP);
            walletRepo.deposit(
                    novaCompra.getUtilizador().getIdUtilizador(),
                    devolve
            );
            restante = BigDecimal.ZERO;
        } else {
            // Limit que não casou totalmente continua "ativa"
            novaCompra.setStatus("ativa");
        }
        ordemRepo.atualizarOrdem(novaCompra);
    }

    /**
     * Processa uma ordem de venda, seja market ou limit.
     */
    public void processarOrdemVenda(Ordem novaVenda) throws SQLException {
        BigDecimal restante = novaVenda.getQuantidade();
        String modo = novaVenda.getModo();                  // "market" ou "limit"
        BigDecimal precoLimite = novaVenda.getPrecoUnitarioEur();

        // 1) Busca ordens de compra que atendam ao modo e preço-limite
        List<Ordem> ordensCompra = ordemRepo.obterOrdensPendentes(
                novaVenda.getMoeda().getIdMoeda(),
                "compra",
                modo,
                precoLimite
        );

        // 2) Executa matching enquanto houver quantidade e ordens compatíveis
        for (Ordem compra : ordensCompra) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal disponivelCompra = compra.getQuantidade();
            BigDecimal matchQtde = restante.min(disponivelCompra);

            // Preço de execução = preço que a ordem de compra declarou
            BigDecimal precoExecucao = compra.getPrecoUnitarioEur().setScale(8, RoundingMode.HALF_UP);

            // Insere transação “venda” e “compra”
            transacaoRepo.inserirTransacao(
                    novaVenda.getUtilizador().getIdUtilizador(),
                    novaVenda.getMoeda().getIdMoeda(),
                    "venda",
                    matchQtde.setScale(8, RoundingMode.HALF_UP),
                    precoExecucao
            );
            transacaoRepo.inserirTransacao(
                    compra.getUtilizador().getIdUtilizador(),
                    compra.getMoeda().getIdMoeda(),
                    "compra",
                    matchQtde.setScale(8, RoundingMode.HALF_UP),
                    precoExecucao
            );

            // Atualiza ordem de compra (quantidade e status)
            BigDecimal novaCompraRestante = disponivelCompra.subtract(matchQtde);
            compra.setQuantidade(novaCompraRestante);
            compra.setStatus(
                    novaCompraRestante.compareTo(BigDecimal.ZERO) == 0 ?
                            "executada" : "ativa"
            );
            ordemRepo.atualizarOrdem(compra);

            // Credita euros ao vendedor e cripto ao comprador
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

            restante = restante.subtract(matchQtde);
        }

        // 3) Atualiza a própria ordem de venda
        novaVenda.setQuantidade(restante);
        if (restante.compareTo(BigDecimal.ZERO) == 0) {
            novaVenda.setStatus("executada");
        } else if ("market".equalsIgnoreCase(modo)) {
            // Para market, devolve cripto não vendido e marca como executada
            novaVenda.setStatus("executada");
            portfolioRepo.incrementarQuantidade(
                    novaVenda.getUtilizador().getIdUtilizador(),
                    novaVenda.getMoeda().getIdMoeda(),
                    restante
            );
            restante = BigDecimal.ZERO;
        } else {
            // Limit que não casou totalmente continua "ativa"
            novaVenda.setStatus("ativa");
        }
        ordemRepo.atualizarOrdem(novaVenda);
    }
}
