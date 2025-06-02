package Repository;

import model.Transacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransacaoRepository {

    private final Connection connection;

    public TransacaoRepository(Connection connection) {
        this.connection = connection;
    }

    // Inserir nova transação
    public void inserirTransacao(Transacao transacao) throws SQLException {
        String sql = "INSERT INTO transacoes (id_ordem_compra, id_ordem_venda, id_moeda, quantidade_executada, preco_executado) " +
                "VALUES (?, ?, ?, ?, ?)";

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, transacao.getId_ordem_compra());
        stmt.setInt(2, transacao.getId_ordem_venda());
        stmt.setInt(3, transacao.getId_moeda());
        stmt.setDouble(4, transacao.getQuantidade_executada());
        stmt.setDouble(5, transacao.getPreco_executado());

        stmt.executeUpdate();
        stmt.close();
    }
}
