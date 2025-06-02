package utils;

import Repository.OrdemRepository;
import Repository.TransacaoRepository;
import model.Ordem;
import model.Transacao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class TradeService {

    private final OrdemRepository ordemRepo;
    private final TransacaoRepository transacaoRepo;

    public TradeService(Connection connection) {
        this.ordemRepo = new OrdemRepository(connection);
        this.transacaoRepo = new TransacaoRepository(connection);
    }

    public void processarOrdemCompra(Ordem novaCompra) throws SQLException {
        final double margemPercentual = 0.02; // 2% de margem permitida

        List<Ordem> ordensVenda = ordemRepo.obterOrdensPendentes(novaCompra.getId_moeda(), "VENDA");
        double restante = novaCompra.getQuantidade_restante();

        for (Ordem venda : ordensVenda) {
            if (restante <= 0) break;

            double precoCompra = novaCompra.getPreco_no_momento();
            double precoVenda = venda.getPreco_no_momento();
            double diferenca = Math.abs(precoVenda - precoCompra);
            double margemAceitavel = precoCompra * margemPercentual;

            if (diferenca > margemAceitavel) continue;

            double matchQuantidade = Math.min(restante, venda.getQuantidade_restante());
            double precoExecucao = precoVenda;

            Transacao t = new Transacao();
            t.setId_ordem_compra(novaCompra.getId_ordem());
            t.setId_ordem_venda(venda.getId_ordem());
            t.setId_moeda(novaCompra.getId_moeda());
            t.setQuantidade_executada(matchQuantidade);
            t.setPreco_executado(precoExecucao);
            t.setTimestamp(LocalDateTime.now());
            transacaoRepo.inserirTransacao(t);

            restante -= matchQuantidade;
            double novaVendaRestante = venda.getQuantidade_restante() - matchQuantidade;
            venda.setQuantidade_restante(novaVendaRestante);
            venda.setStatus(novaVendaRestante == 0 ? "EXECUTADA" : "PARCIAL");
            ordemRepo.atualizarOrdem(venda);
        }

        novaCompra.setQuantidade_restante(restante);
        novaCompra.setStatus(restante == 0 ? "EXECUTADA" : "PARCIAL");
        ordemRepo.atualizarOrdem(novaCompra);
    }

    public void processarOrdemVenda(Ordem novaVenda) throws SQLException {
        final double margemPercentual = 0.02; // 2% de margem permitida

        List<Ordem> ordensCompra = ordemRepo.obterOrdensPendentes(novaVenda.getId_moeda(), "COMPRA");
        double restante = novaVenda.getQuantidade_restante();

        for (Ordem compra : ordensCompra) {
            if (restante <= 0) break;

            double precoVenda = novaVenda.getPreco_no_momento();
            double precoCompra = compra.getPreco_no_momento();
            double diferenca = Math.abs(precoCompra - precoVenda);
            double margemAceitavel = precoVenda * margemPercentual;

            if (diferenca > margemAceitavel) continue;

            double matchQuantidade = Math.min(restante, compra.getQuantidade_restante());
            double precoExecucao = precoCompra;

            Transacao t = new Transacao();
            t.setId_ordem_compra(compra.getId_ordem());
            t.setId_ordem_venda(novaVenda.getId_ordem());
            t.setId_moeda(novaVenda.getId_moeda());
            t.setQuantidade_executada(matchQuantidade);
            t.setPreco_executado(precoExecucao);
            t.setTimestamp(LocalDateTime.now());
            transacaoRepo.inserirTransacao(t);

            restante -= matchQuantidade;
            double novaCompraRestante = compra.getQuantidade_restante() - matchQuantidade;
            compra.setQuantidade_restante(novaCompraRestante);
            compra.setStatus(novaCompraRestante == 0 ? "EXECUTADA" : "PARCIAL");
            ordemRepo.atualizarOrdem(compra);
        }

        novaVenda.setQuantidade_restante(restante);
        novaVenda.setStatus(restante == 0 ? "EXECUTADA" : "PARCIAL");
        ordemRepo.atualizarOrdem(novaVenda);
    }
}
