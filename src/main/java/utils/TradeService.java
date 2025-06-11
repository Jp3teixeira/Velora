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
     * Processa uma ordem de compra (market ou limit) com cálculo de preço médio.
     */
    public void processarOrdemCompra(Ordem novaCompra) throws SQLException {
        BigDecimal restante    = novaCompra.getQuantidade();
        String    modo         = novaCompra.getModo();
        BigDecimal precoLimite = novaCompra.getPrecoUnitarioEur();
        boolean   compraMarket = "market".equalsIgnoreCase(modo);
        // status IDs
        int idStatusExecutada = ordemRepo.obterIdStatus("executada");
        int idStatusAtiva     = ordemRepo.obterIdStatus("ativa");

        // 1) busca ordens de venda compatíveis (filtrando expiradas no repository)
        List<Ordem> ordensVenda = ordemRepo.obterOrdensPendentes(
                novaCompra.getMoeda().getIdMoeda(),
                "venda",
                modo,
                precoLimite
        );

        // 2) faz matching com cálculo de preço de execução
        for (Ordem venda : ordensVenda) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal disponivelVenda = venda.getQuantidade();
            BigDecimal matchQtde       = restante.min(disponivelVenda);

            boolean vendaMarket = "market".equalsIgnoreCase(venda.getModo());
            BigDecimal precoExecucao;
            if (!compraMarket && !vendaMarket) {
                precoExecucao = novaCompra.getPrecoUnitarioEur()
                        .add(venda.getPrecoUnitarioEur())
                        .divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
            } else if (compraMarket && !vendaMarket) {
                precoExecucao = venda.getPrecoUnitarioEur();
            } else if (!compraMarket && vendaMarket) {
                precoExecucao = novaCompra.getPrecoUnitarioEur();
            } else {
                precoExecucao = novaCompra.getPrecoUnitarioEur();
            }
            precoExecucao = precoExecucao.setScale(8, RoundingMode.HALF_UP);

            // insere transações
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

            // atualiza ordem de venda
            BigDecimal novaVendaRest = disponivelVenda.subtract(matchQtde);
            venda.setQuantidade(novaVendaRest);
            if (novaVendaRest.compareTo(BigDecimal.ZERO) == 0) {
                venda.setStatus("executada");
                venda.setIdStatus(idStatusExecutada);
            } else {
                venda.setStatus("ativa");
                venda.setIdStatus(idStatusAtiva);
            }
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

        // 3) atualiza a própria ordem de compra
        novaCompra.setQuantidade(restante);
        if (restante.compareTo(BigDecimal.ZERO) == 0) {
            novaCompra.setStatus("executada");
            novaCompra.setIdStatus(idStatusExecutada);
        } else {
            novaCompra.setStatus("ativa");
            novaCompra.setIdStatus(idStatusAtiva);
        }
        ordemRepo.atualizarOrdem(novaCompra);
    }

    /**
     * Processa uma ordem de venda (market ou limit) com cálculo de preço médio.
     */
    public void processarOrdemVenda(Ordem novaVenda) throws SQLException {
        BigDecimal restante    = novaVenda.getQuantidade();
        String    modo         = novaVenda.getModo();
        BigDecimal precoLimite = novaVenda.getPrecoUnitarioEur();
        boolean   vendaMarket  = "market".equalsIgnoreCase(modo);
        // status IDs
        int idStatusExecutada = ordemRepo.obterIdStatus("executada");
        int idStatusAtiva     = ordemRepo.obterIdStatus("ativa");

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

            boolean compraMarket = "market".equalsIgnoreCase(compra.getModo());
            BigDecimal precoExecucao;
            if (!vendaMarket && !compraMarket) {
                precoExecucao = novaVenda.getPrecoUnitarioEur()
                        .add(compra.getPrecoUnitarioEur())
                        .divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
            } else if (vendaMarket && !compraMarket) {
                precoExecucao = compra.getPrecoUnitarioEur();
            } else if (!vendaMarket && compraMarket) {
                precoExecucao = novaVenda.getPrecoUnitarioEur();
            } else {
                precoExecucao = novaVenda.getPrecoUnitarioEur();
            }
            precoExecucao = precoExecucao.setScale(8, RoundingMode.HALF_UP);

            // insere transações
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

            // atualiza ordem de compra
            BigDecimal novaCompraRest = disponivelCompra.subtract(matchQtde);
            compra.setQuantidade(novaCompraRest);
            if (novaCompraRest.compareTo(BigDecimal.ZERO) == 0) {
                compra.setStatus("executada");
                compra.setIdStatus(idStatusExecutada);
            } else {
                compra.setStatus("ativa");
                compra.setIdStatus(idStatusAtiva);
            }
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

        // 3) atualiza a própria ordem de venda
        novaVenda.setQuantidade(restante);
        if (restante.compareTo(BigDecimal.ZERO) == 0) {
            novaVenda.setStatus("executada");
            novaVenda.setIdStatus(idStatusExecutada);
        } else {
            novaVenda.setStatus("ativa");
            novaVenda.setIdStatus(idStatusAtiva);
        }
        ordemRepo.atualizarOrdem(novaVenda);
    }

    /**
     * Re-processa todas as ordens market de compra pendentes para um dado ativo.
     */
    public void processarOrdensCompraMarketPendentes(int idMoeda) throws SQLException {
        List<Ordem> compras = ordemRepo.obterOrdensPendentes(
                idMoeda, "compra", "market", null);
        for (Ordem compra : compras) {
            processarOrdemCompra(compra);
        }
    }

    /**
     * Re-processa todas as ordens market de venda pendentes para um dado ativo.
     */
    public void processarOrdensVendaMarketPendentes(int idMoeda) throws SQLException {
        List<Ordem> vendas = ordemRepo.obterOrdensPendentes(
                idMoeda, "venda", "market", null);
        for (Ordem venda : vendas) {
            processarOrdemVenda(venda);
        }
    }
}
