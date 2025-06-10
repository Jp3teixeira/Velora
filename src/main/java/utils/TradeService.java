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
     * Processa uma ordem de compra (market ou limit).
     */
    public void processarOrdemCompra(Ordem novaCompra) throws SQLException {
        BigDecimal restante    = novaCompra.getQuantidade();
        String    modo         = novaCompra.getModo();
        BigDecimal precoLimite = novaCompra.getPrecoUnitarioEur();

        // 1) busca ordens de venda compatíveis
        List<Ordem> ordensVenda = ordemRepo.obterOrdensPendentes(
                novaCompra.getMoeda().getIdMoeda(),
                "venda",
                modo,
                precoLimite
        );

        // 2) faz matching
        for (Ordem venda : ordensVenda) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal disponivelVenda = venda.getQuantidade();
            BigDecimal matchQtde       = restante.min(disponivelVenda);
            BigDecimal precoExecucao   = venda.getPrecoUnitarioEur()
                    .setScale(8, RoundingMode.HALF_UP);

            // insere as duas transações (compra e venda)
            transacaoRepo.inserirTransacao(
                    novaCompra.getUtilizador().getIdUtilizador(),
                    novaCompra.getMoeda().getIdMoeda(),
                    matchQtde,
                    precoExecucao,
                    novaCompra.getIdTipoOrdem()
            );
            transacaoRepo.inserirTransacao(
                    venda.getUtilizador().getIdUtilizador(),
                    venda.getMoeda().getIdMoeda(),
                    matchQtde,
                    precoExecucao,
                    venda.getIdTipoOrdem()
            );

            // actualiza ordem de venda
            BigDecimal novaVendaRest = disponivelVenda.subtract(matchQtde);
            venda.setQuantidade(novaVendaRest);
            venda.setStatus(
                    novaVendaRest.compareTo(BigDecimal.ZERO) == 0
                            ? "executada"
                            : "ativa"
            );
            ordemRepo.atualizarOrdem(venda);

            // credita cripto ao comprador e EUR ao vendedor
            portfolioRepo.aumentarQuantidade(
                    novaCompra.getUtilizador().getIdUtilizador(),
                    novaCompra.getMoeda().getIdMoeda(),
                    matchQtde
            );
            BigDecimal valorVendedor = matchQtde.multiply(precoExecucao);
            walletRepo.deposit(
                    venda.getUtilizador().getIdUtilizador(),
                    valorVendedor
            );

            restante = restante.subtract(matchQtde);
        }

        // 3) actualiza a própria ordem de compra
        novaCompra.setQuantidade(restante);
        if (restante.compareTo(BigDecimal.ZERO) == 0) {
            novaCompra.setStatus("executada");
        } else if ("market".equalsIgnoreCase(modo)) {
            // devolve o EUR não gasto e marca como executada
            novaCompra.setStatus("executada");
            BigDecimal devolve = restante.multiply(precoLimite)
                    .setScale(8, RoundingMode.HALF_UP);
            walletRepo.deposit(
                    novaCompra.getUtilizador().getIdUtilizador(),
                    devolve
            );
            restante = BigDecimal.ZERO;
        } else {
            // limit que não casou totalmente continua ativa
            novaCompra.setStatus("ativa");
        }
        ordemRepo.atualizarOrdem(novaCompra);
    }

    /**
     * Processa uma ordem de venda (market ou limit).
     */
    public void processarOrdemVenda(Ordem novaVenda) throws SQLException {
        BigDecimal restante    = novaVenda.getQuantidade();
        String    modo         = novaVenda.getModo();
        BigDecimal precoLimite = novaVenda.getPrecoUnitarioEur();

        // 1) busca ordens de compra compatíveis
        List<Ordem> ordensCompra = ordemRepo.obterOrdensPendentes(
                novaVenda.getMoeda().getIdMoeda(),
                "compra",
                modo,
                precoLimite
        );

        // 2) faz matching
        for (Ordem compra : ordensCompra) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal disponivelCompra = compra.getQuantidade();
            BigDecimal matchQtde        = restante.min(disponivelCompra);
            BigDecimal precoExecucao    = compra.getPrecoUnitarioEur()
                    .setScale(8, RoundingMode.HALF_UP);

            // insere as transações (venda e compra)
            transacaoRepo.inserirTransacao(
                    novaVenda.getUtilizador().getIdUtilizador(),
                    novaVenda.getMoeda().getIdMoeda(),
                    matchQtde,
                    precoExecucao,
                    novaVenda.getIdTipoOrdem()
            );
            transacaoRepo.inserirTransacao(
                    compra.getUtilizador().getIdUtilizador(),
                    compra.getMoeda().getIdMoeda(),
                    matchQtde,
                    precoExecucao,
                    compra.getIdTipoOrdem()
            );

            // actualiza ordem de compra
            BigDecimal novaCompraRest = disponivelCompra.subtract(matchQtde);
            compra.setQuantidade(novaCompraRest);
            compra.setStatus(
                    novaCompraRest.compareTo(BigDecimal.ZERO) == 0
                            ? "executada"
                            : "ativa"
            );
            ordemRepo.atualizarOrdem(compra);

            // credita EUR ao vendedor e cripto ao comprador
            BigDecimal valorVendedor = matchQtde.multiply(precoExecucao);
            walletRepo.deposit(
                    compra.getUtilizador().getIdUtilizador(),
                    valorVendedor
            );
            portfolioRepo.aumentarQuantidade(
                    compra.getUtilizador().getIdUtilizador(),
                    compra.getMoeda().getIdMoeda(),
                    matchQtde
            );

            restante = restante.subtract(matchQtde);
        }

        // 3) actualiza a própria ordem de venda
        novaVenda.setQuantidade(restante);
        if (restante.compareTo(BigDecimal.ZERO) == 0) {
            novaVenda.setStatus("executada");
        } else if ("market".equalsIgnoreCase(modo)) {
            // devolve cripto não vendido e marca como executada
            novaVenda.setStatus("executada");
            portfolioRepo.aumentarQuantidade(
                    novaVenda.getUtilizador().getIdUtilizador(),
                    novaVenda.getMoeda().getIdMoeda(),
                    restante
            );
            restante = BigDecimal.ZERO;
        } else {
            // limit que não casou totalmente continua ativa
            novaVenda.setStatus("ativa");
        }
        ordemRepo.atualizarOrdem(novaVenda);
    }
}
