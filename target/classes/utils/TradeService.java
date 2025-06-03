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
import java.time.LocalDateTime;
import java.util.List;

public class TradeService {

    private final OrdemRepository ordemRepo;
    private final TransacaoRepository transacaoRepo;
    private final PortfolioRepository portfolioRepo;
    private final WalletRepository walletRepo;

    public TradeService(Connection connection) {
        this.ordemRepo       = new OrdemRepository(connection);
        this.transacaoRepo   = new TransacaoRepository(connection);
        this.portfolioRepo   = new PortfolioRepository();
        this.walletRepo      = WalletRepository.getInstance();
    }

    /**
     * Processa uma ordem de compra:
     * - Casa contra ordens de venda pendentes.
     * - Para cada lote casado:
     *    • Insere Transacao
     *    • Credita cripto no portfolio do comprador
     *    • Credita euros no saldo do vendedor
     * - Atualiza status = 'executada' somente se quantidade_restante == 0;
     *   caso contrário, mantém 'ativa'.
     * - Se sobrar quantidade nao casada, devolve montante em euros.
     */
    public void processarOrdemCompra(Ordem novaCompra) throws SQLException {
        final BigDecimal margemPercentual = BigDecimal.valueOf(0.06); // 6%

        List<Ordem> ordensVenda = ordemRepo.obterOrdensPendentes(
                novaCompra.getMoeda().getIdMoeda(), "venda"
        );

        BigDecimal restante = novaCompra.getQuantidade();
        BigDecimal precoUnitarioOriginal = novaCompra.getPrecoUnitarioEur();

        for (Ordem venda : ordensVenda) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0)
                break;

            BigDecimal precoVenda     = venda.getPrecoUnitarioEur();
            BigDecimal precoCompra    = precoUnitarioOriginal;
            BigDecimal diferenca      = precoVenda.subtract(precoCompra).abs();
            BigDecimal margemAceitavel = precoCompra.multiply(margemPercentual);

            if (diferenca.compareTo(margemAceitavel) > 0) {
                continue;
            }

            BigDecimal disponivelVenda = venda.getQuantidade();
            BigDecimal matchQtde       = restante.min(disponivelVenda);
            BigDecimal precoExecucao   = precoVenda.setScale(8, RoundingMode.HALF_UP);

            // 1) Insere transacao
            transacaoRepo.inserirTransacao(
                    novaCompra.getIdOrdem(),
                    venda.getIdOrdem(),
                    novaCompra.getMoeda().getIdMoeda(),
                    matchQtde.setScale(8, RoundingMode.HALF_UP),
                    precoExecucao
            );

            // 2) Atualiza restante na ordem de venda
            BigDecimal novaVendaRestante = disponivelVenda.subtract(matchQtde);
            venda.setQuantidade(novaVendaRestante);
            venda.setStatus(
                    novaVendaRestante.compareTo(BigDecimal.ZERO) == 0
                            ? "executada"
                            : "ativa"
            );
            ordemRepo.atualizarOrdem(venda);

            // 3) Credita cripto no portfolio do comprador
            portfolioRepo.incrementarQuantidade(
                    novaCompra.getUtilizador().getIdUtilizador(),
                    novaCompra.getMoeda().getIdMoeda(),
                    matchQtde
            );

            // 4) Credita euros ao vendedor
            BigDecimal valorParaVendedor = matchQtde.multiply(precoExecucao);
            walletRepo.deposit(
                    venda.getUtilizador().getIdUtilizador(),
                    valorParaVendedor
            );

            // 5) Diminui restante de compra
            restante = restante.subtract(matchQtde);
        }

        // 6) Atualiza restante na ordem de compra
        novaCompra.setQuantidade(restante);
        novaCompra.setStatus(
                restante.compareTo(BigDecimal.ZERO) == 0
                        ? "executada"
                        : "ativa"
        );
        ordemRepo.atualizarOrdem(novaCompra);

        // 7) Se sobrar quantidade nao casada, devolve euros nao usados
        if (restante.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal naoComprada = restante.multiply(precoUnitarioOriginal)
                    .setScale(8, RoundingMode.HALF_UP);
            walletRepo.deposit(
                    novaCompra.getUtilizador().getIdUtilizador(),
                    naoComprada
            );
        }
    }

    /**
     * Processa uma ordem de venda:
     * - Casa contra ordens de compra pendentes.
     * - Para cada lote casado:
     *    • Insere Transacao
     *    • Credita euros no saldo do vendedor
     *    • Credita cripto no portfolio do comprador
     * - Atualiza status = 'executada' somente se quantidade_restante == 0;
     *   caso contrário, mantém 'ativa'.
     * - Se sobrar quantidade nao casada, devolve a cripto ao portfolio.
     */
    public void processarOrdemVenda(Ordem novaVenda) throws SQLException {
        final BigDecimal margemPercentual = BigDecimal.valueOf(0.02); // 2%

        List<Ordem> ordensCompra = ordemRepo.obterOrdensPendentes(
                novaVenda.getMoeda().getIdMoeda(), "compra"
        );

        BigDecimal restante = novaVenda.getQuantidade();
        BigDecimal precoUnitarioOriginal = novaVenda.getPrecoUnitarioEur();

        for (Ordem compra : ordensCompra) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0)
                break;

            BigDecimal precoCompra    = compra.getPrecoUnitarioEur();
            BigDecimal precoVenda     = precoUnitarioOriginal;
            BigDecimal diferenca      = precoCompra.subtract(precoVenda).abs();
            BigDecimal margemAceitavel = precoVenda.multiply(margemPercentual);

            if (diferenca.compareTo(margemAceitavel) > 0) {
                continue;
            }

            BigDecimal disponivelCompra = compra.getQuantidade();
            BigDecimal matchQtde        = restante.min(disponivelCompra);
            BigDecimal precoExecucao    = precoCompra.setScale(8, RoundingMode.HALF_UP);

            // 1) Insere transacao
            transacaoRepo.inserirTransacao(
                    compra.getIdOrdem(),
                    novaVenda.getIdOrdem(),
                    novaVenda.getMoeda().getIdMoeda(),
                    matchQtde.setScale(8, RoundingMode.HALF_UP),
                    precoExecucao
            );

            // 2) Atualiza restante na ordem de compra
            BigDecimal novaCompraRestante = disponivelCompra.subtract(matchQtde);
            compra.setQuantidade(novaCompraRestante);
            compra.setStatus(
                    novaCompraRestante.compareTo(BigDecimal.ZERO) == 0
                            ? "executada"
                            : "ativa"
            );
            ordemRepo.atualizarOrdem(compra);

            // 3) Credita euros no saldo do vendedor
            BigDecimal valorParaVendedor = matchQtde.multiply(precoExecucao);
            walletRepo.deposit(
                    novaVenda.getUtilizador().getIdUtilizador(),
                    valorParaVendedor
            );

            // 4) Credita cripto no portfolio do comprador
            portfolioRepo.incrementarQuantidade(
                    compra.getUtilizador().getIdUtilizador(),
                    novaVenda.getMoeda().getIdMoeda(),
                    matchQtde
            );

            // 5) Diminui restante da ordem de venda
            restante = restante.subtract(matchQtde);
        }

        // 6) Atualiza restante na ordem de venda
        novaVenda.setQuantidade(restante);
        novaVenda.setStatus(
                restante.compareTo(BigDecimal.ZERO) == 0
                        ? "executada"
                        : "ativa"
        );
        ordemRepo.atualizarOrdem(novaVenda);

        // 7) Se sobrar quantidade nao casada, devolve cripto ao portfolio
        if (restante.compareTo(BigDecimal.ZERO) > 0) {
            portfolioRepo.incrementarQuantidade(
                    novaVenda.getUtilizador().getIdUtilizador(),
                    novaVenda.getMoeda().getIdMoeda(),
                    restante
            );
        }
    }
}
