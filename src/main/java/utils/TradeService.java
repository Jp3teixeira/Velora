// utils/TradeService.java
package utils;

import Repository.OrdemRepository;
import Repository.PortfolioRepository;
import Repository.TransacaoRepository;
import Repository.WalletRepository;
import model.Ordem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


public class TradeService {

    private final OrdemRepository ordemRepo;
    private final TransacaoRepository transacaoRepo;
    private final PortfolioRepository portfolioRepo;
    private final WalletRepository walletRepo;
    private final Connection conn;

    public TradeService(Connection connection) {
        this.conn            = connection;
        this.ordemRepo       = new OrdemRepository(connection);
        this.transacaoRepo   = new TransacaoRepository();
        this.portfolioRepo   = new PortfolioRepository();
        this.walletRepo      = WalletRepository.getInstance();
    }

    private int getStatusId(String status) throws SQLException {
        String sql = "SELECT id_status FROM OrdemStatus WHERE status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("Status não encontrado: " + status);
            }
        }
    }

    /**
     * Processa uma ordem de compra (market ou limit) com cálculo de preço médio.
     */
    public void processarOrdemCompra(Ordem novaCompra) throws SQLException {
        BigDecimal restante    = novaCompra.getQuantidade();
        boolean   compraMarket = "market".equalsIgnoreCase(novaCompra.getModo());
        int       idStatusExec = getStatusId("executada");
        int       idStatusAtv  = getStatusId("ativa");

        // 1) busca ordens de venda compatíveis
        List<Ordem> ordensVenda = ordemRepo.obterOrdensPendentes(
                novaCompra.getMoeda().getIdMoeda(),
                "venda",
                novaCompra.getModo(),
                novaCompra.getPrecoUnitarioEur()


        );


        // 2) matching
        for (Ordem venda : ordensVenda) {
            if (restante.signum() <= 0) break;

            BigDecimal disponivel = venda.getQuantidade();
            BigDecimal qtdeMatch  = restante.min(disponivel);

            boolean vendaMarket = "market".equalsIgnoreCase(venda.getModo());
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

            // regista transações
            transacaoRepo.inserirTransacao(
                    novaCompra.getUtilizador().getIdUtilizador(),
                    novaCompra.getMoeda().getIdMoeda(),
                    qtdeMatch,
                    precoExec,
                    novaCompra.getIdTipoOrdem()
            );
            transacaoRepo.inserirTransacao(
                    venda.getUtilizador().getIdUtilizador(),
                    venda.getMoeda().getIdMoeda(),
                    qtdeMatch,
                    precoExec,
                    venda.getIdTipoOrdem()
            );

            // atualiza venda
            BigDecimal remVenda = disponivel.subtract(qtdeMatch);
            venda.setQuantidade(remVenda);
            if (remVenda.signum() == 0) {
                venda.setIdStatus(idStatusExec);
                venda.setStatus("executada");
            } else {
                venda.setIdStatus(idStatusAtv);
                venda.setStatus("ativa");
            }
            ordemRepo.atualizarOrdem(venda);

            // mover ativos
            portfolioRepo.aumentarQuantidade(
                    novaCompra.getUtilizador().getIdUtilizador(),
                    novaCompra.getMoeda().getIdMoeda(),
                    qtdeMatch
            );
            walletRepo.deposit(
                    venda.getUtilizador().getIdUtilizador(),
                    qtdeMatch.multiply(precoExec)
            );

            restante = restante.subtract(qtdeMatch);
        }

        // 3) atualiza ordem de compra
        novaCompra.setQuantidade(restante);
        if (restante.signum() == 0) {
            novaCompra.setIdStatus(idStatusExec);
            novaCompra.setStatus("executada");
        } else {
            novaCompra.setIdStatus(idStatusAtv);
            novaCompra.setStatus("ativa");
        }
        ordemRepo.atualizarOrdem(novaCompra);
    }

    /**
     * Processa uma ordem de venda (market ou limit) com cálculo de preço médio.
     */
    public void processarOrdemVenda(Ordem novaVenda) throws SQLException {
        BigDecimal restante    = novaVenda.getQuantidade();
        boolean   vendaMarket  = "market".equalsIgnoreCase(novaVenda.getModo());
        int       idStatusExec = getStatusId("executada");
        int       idStatusAtv  = getStatusId("ativa");

        List<Ordem> ordensCompra = ordemRepo.obterOrdensPendentes(
                novaVenda.getMoeda().getIdMoeda(),
                "compra",
                novaVenda.getModo(),
                novaVenda.getPrecoUnitarioEur()
        );

        for (Ordem compra : ordensCompra) {
            if (restante.signum() <= 0) break;

            BigDecimal disponivel = compra.getQuantidade();
            BigDecimal qtdeMatch  = restante.min(disponivel);

            boolean compraMarket = "market".equalsIgnoreCase(compra.getModo());
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

            transacaoRepo.inserirTransacao(
                    novaVenda.getUtilizador().getIdUtilizador(),
                    novaVenda.getMoeda().getIdMoeda(),
                    qtdeMatch,
                    precoExec,
                    novaVenda.getIdTipoOrdem()
            );
            transacaoRepo.inserirTransacao(
                    compra.getUtilizador().getIdUtilizador(),
                    compra.getMoeda().getIdMoeda(),
                    qtdeMatch,
                    precoExec,
                    compra.getIdTipoOrdem()
            );

            BigDecimal remCompra = disponivel.subtract(qtdeMatch);
            compra.setQuantidade(remCompra);
            if (remCompra.signum() == 0) {
                compra.setIdStatus(idStatusExec);
                compra.setStatus("executada");
            } else {
                compra.setIdStatus(idStatusAtv);
                compra.setStatus("ativa");
            }
            ordemRepo.atualizarOrdem(compra);

            walletRepo.deposit(
                    compra.getUtilizador().getIdUtilizador(),
                    qtdeMatch.multiply(precoExec)
            );
            portfolioRepo.aumentarQuantidade(
                    compra.getUtilizador().getIdUtilizador(),
                    compra.getMoeda().getIdMoeda(),
                    qtdeMatch
            );

            restante = restante.subtract(qtdeMatch);
        }

        novaVenda.setQuantidade(restante);
        if (restante.signum() == 0) {
            novaVenda.setIdStatus(idStatusExec);
            novaVenda.setStatus("executada");
        } else {
            novaVenda.setIdStatus(idStatusAtv);
            novaVenda.setStatus("ativa");
        }
        ordemRepo.atualizarOrdem(novaVenda);
    }

    /**
     * Re-processa todas as ordens market pendentes para um ativo.
     */
    public void processarOrdensCompraMarketPendentes(int idMoeda) throws SQLException {
        List<Ordem> compras = ordemRepo.obterOrdensPendentes(idMoeda, "compra", "market", null);
        for (Ordem c : compras) processarOrdemCompra(c);
    }

    public void processarOrdensVendaMarketPendentes(int idMoeda) throws SQLException {
        List<Ordem> vendas = ordemRepo.obterOrdensPendentes(idMoeda, "venda", "market", null);
        for (Ordem v : vendas) processarOrdemVenda(v);
    }
}
