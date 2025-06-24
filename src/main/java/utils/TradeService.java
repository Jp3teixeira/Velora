// src/utils/TradeService.java
package utils;

import Repository.OrdemRepository;
import Repository.PortfolioRepository;
import Repository.TransacaoRepository;
import Repository.WalletRepository;
import model.Ordem;
import model.OrdemModo;
import model.OrdemStatus;
import model.OrdemTipo;
import model.Transacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class TradeService {

    private final OrdemRepository       ordemRepo;
    private final TransacaoRepository   transacaoRepo;
    private final PortfolioRepository   portfolioRepo;
    private final WalletRepository      walletRepo;

    public TradeService(java.sql.Connection connection) {
        this.ordemRepo     = new OrdemRepository(connection);
        this.transacaoRepo = new TransacaoRepository();
        this.portfolioRepo = new PortfolioRepository();
        this.walletRepo    = WalletRepository.getInstance();
    }

    private int getStatusId(OrdemStatus status) throws SQLException {
        return ordemRepo.obterIdStatus(status.name());
    }

    private int getModoId(OrdemModo modo) throws SQLException {
        return ordemRepo.obterIdModo(modo.name());
    }

    private int getTipoId(OrdemTipo tipo) throws SQLException {
        return ordemRepo.obterIdTipoOrdem(tipo.name());
    }

    /**
     * Processa uma ordem de compra (market ou limit) com cálculo de preço médio.
     */
    public void processarOrdemCompra(Ordem novaCompra) throws SQLException {
        BigDecimal restante    = novaCompra.getQuantidade();
        boolean   compraMarket = novaCompra.getModo() == OrdemModo.MARKET;
        int       idExec       = getStatusId(OrdemStatus.EXECUTADA);
        int       idAtiva      = getStatusId(OrdemStatus.ATIVA);

        // 1) obter todas e filtrar vendas pendentes compatíveis
        List<Ordem> ordensVenda = ordemRepo.getAll().stream()
                .filter(o -> o.getMoeda().getId().equals(novaCompra.getMoeda().getId()))
                .filter(o -> o.getTipoOrdem() == OrdemTipo.VENDA)
                .filter(o -> o.getStatus() == OrdemStatus.ATIVA)
                .filter(o -> compraMarket
                        || o.getModo() == OrdemModo.MARKET
                        || o.getPrecoUnitarioEur().compareTo(novaCompra.getPrecoUnitarioEur()) <= 0
                )
                .collect(Collectors.toList());

        for (Ordem venda : ordensVenda) {
            if (restante.signum() <= 0) break;
            BigDecimal disponivel = venda.getQuantidade();
            BigDecimal qtdeMatch  = restante.min(disponivel);

            boolean vendaMarket = venda.getModo() == OrdemModo.MARKET;
            BigDecimal precoExec;
            if (!compraMarket && !vendaMarket) {
                precoExec = novaCompra.getPrecoUnitarioEur()
                        .add(venda.getPrecoUnitarioEur())
                        .divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
            } else if (compraMarket && !vendaMarket) {
                precoExec = venda.getPrecoUnitarioEur();
            } else {
                precoExec = novaCompra.getPrecoUnitarioEur();
            }
            precoExec = precoExec.setScale(8, RoundingMode.HALF_UP);

            // registra transações
            Transacao txC = new Transacao();
            txC.setUtilizador(novaCompra.getUtilizador());
            txC.setMoeda(novaCompra.getMoeda());
            txC.setQuantidade(qtdeMatch);
            txC.setPrecoUnitarioEur(precoExec);
            txC.setTipo(novaCompra.getTipoOrdem().name());
            transacaoRepo.save(txC);

            Transacao txV = new Transacao();
            txV.setUtilizador(venda.getUtilizador());
            txV.setMoeda(venda.getMoeda());
            txV.setQuantidade(qtdeMatch);
            txV.setPrecoUnitarioEur(precoExec);
            txV.setTipo(venda.getTipoOrdem().name());
            transacaoRepo.save(txV);

            // atualiza venda
            venda.setQuantidade(disponivel.subtract(qtdeMatch));
            if (venda.getQuantidade().signum() == 0) {
                venda.setStatus(OrdemStatus.EXECUTADA);
                venda.setIdStatus(idExec);
            } else {
                venda.setStatus(OrdemStatus.ATIVA);
                venda.setIdStatus(idAtiva);
            }
            ordemRepo.update(venda);

            // mover ativos e fundos
            portfolioRepo.aumentarQuantidade(
                    novaCompra.getUtilizador().getId(),
                    novaCompra.getMoeda().getId(),
                    qtdeMatch
            );
            walletRepo.deposit(
                    venda.getUtilizador().getId(),
                    qtdeMatch.multiply(precoExec)
            );

            restante = restante.subtract(qtdeMatch);
        }

        // 3) atualiza ordem de compra
        novaCompra.setQuantidade(restante);
        if (restante.signum() == 0) {
            novaCompra.setStatus(OrdemStatus.EXECUTADA);
            novaCompra.setIdStatus(idExec);
        } else {
            novaCompra.setStatus(OrdemStatus.ATIVA);
            novaCompra.setIdStatus(idAtiva);
        }
        ordemRepo.update(novaCompra);
    }

    /**
     * Processa uma ordem de venda (market ou limit) com cálculo de preço médio.
     */
    public void processarOrdemVenda(Ordem novaVenda) throws SQLException {
        BigDecimal restante    = novaVenda.getQuantidade();
        boolean   vendaMarket  = novaVenda.getModo() == OrdemModo.MARKET;
        int       idExec       = getStatusId(OrdemStatus.EXECUTADA);
        int       idAtiva      = getStatusId(OrdemStatus.ATIVA);

        List<Ordem> ordensCompra = ordemRepo.getAll().stream()
                .filter(o -> o.getMoeda().getId().equals(novaVenda.getMoeda().getId()))
                .filter(o -> o.getTipoOrdem() == OrdemTipo.COMPRA)
                .filter(o -> o.getStatus() == OrdemStatus.ATIVA)
                .filter(o -> vendaMarket
                        || o.getModo() == OrdemModo.MARKET
                        || o.getPrecoUnitarioEur().compareTo(novaVenda.getPrecoUnitarioEur()) >= 0
                )
                .collect(Collectors.toList());

        for (Ordem compra : ordensCompra) {
            if (restante.signum() <= 0) break;
            BigDecimal disponivel = compra.getQuantidade();
            BigDecimal qtdeMatch  = restante.min(disponivel);

            boolean compraMarket = compra.getModo() == OrdemModo.MARKET;
            BigDecimal precoExec;
            if (!vendaMarket && !compraMarket) {
                precoExec = novaVenda.getPrecoUnitarioEur()
                        .add(compra.getPrecoUnitarioEur())
                        .divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
            } else if (vendaMarket && !compraMarket) {
                precoExec = compra.getPrecoUnitarioEur();
            } else {
                precoExec = novaVenda.getPrecoUnitarioEur();
            }
            precoExec = precoExec.setScale(8, RoundingMode.HALF_UP);

            Transacao txV = new Transacao();
            txV.setUtilizador(novaVenda.getUtilizador());
            txV.setMoeda(novaVenda.getMoeda());
            txV.setQuantidade(qtdeMatch);
            txV.setPrecoUnitarioEur(precoExec);
            txV.setTipo(novaVenda.getTipoOrdem().name());
            transacaoRepo.save(txV);

            Transacao txC = new Transacao();
            txC.setUtilizador(compra.getUtilizador());
            txC.setMoeda(compra.getMoeda());
            txC.setQuantidade(qtdeMatch);
            txC.setPrecoUnitarioEur(precoExec);
            txC.setTipo(compra.getTipoOrdem().name());
            transacaoRepo.save(txC);

            compra.setQuantidade(disponivel.subtract(qtdeMatch));
            if (compra.getQuantidade().signum() == 0) {
                compra.setStatus(OrdemStatus.EXECUTADA);
                compra.setIdStatus(idExec);
            } else {
                compra.setStatus(OrdemStatus.ATIVA);
                compra.setIdStatus(idAtiva);
            }
            ordemRepo.update(compra);

            walletRepo.deposit(
                    compra.getUtilizador().getId(),
                    qtdeMatch.multiply(precoExec)
            );
            portfolioRepo.aumentarQuantidade(
                    compra.getUtilizador().getId(),
                    compra.getMoeda().getId(),
                    qtdeMatch
            );

            restante = restante.subtract(qtdeMatch);
        }

        novaVenda.setQuantidade(restante);
        if (restante.signum() == 0) {
            novaVenda.setStatus(OrdemStatus.EXECUTADA);
            novaVenda.setIdStatus(idExec);
        } else {
            novaVenda.setStatus(OrdemStatus.ATIVA);
            novaVenda.setIdStatus(idAtiva);
        }
        ordemRepo.update(novaVenda);
    }
    /**
     * Processa todas as market-sell pendentes para uma moeda.
     */
    public void processarOrdensVendaMarketPendentes(int idMoeda) throws SQLException {
        List<Ordem> pendentes = ordemRepo.getAll().stream()
                .filter(o -> o.getMoeda().getId().equals(idMoeda))
                .filter(o -> o.getTipoOrdem() == OrdemTipo.VENDA)
                .filter(o -> o.getModo() == OrdemModo.MARKET)
                .filter(o -> o.getStatus() == OrdemStatus.ATIVA)
                .collect(Collectors.toList());
        for (Ordem o : pendentes) {
            processarOrdemVenda(o);
        }
    }

    /**
     * Processa todas as market-buy pendentes para uma moeda.
     */
    public void processarOrdensCompraMarketPendentes(int idMoeda) throws SQLException {
        List<Ordem> pendentes = ordemRepo.getAll().stream()
                .filter(o -> o.getMoeda().getId().equals(idMoeda))
                .filter(o -> o.getTipoOrdem() == OrdemTipo.COMPRA)
                .filter(o -> o.getModo() == OrdemModo.MARKET)
                .filter(o -> o.getStatus() == OrdemStatus.ATIVA)
                .collect(Collectors.toList());
        for (Ordem o : pendentes) {
            processarOrdemCompra(o);
        }
    }
}








