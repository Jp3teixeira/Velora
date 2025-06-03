package Repository;

import model.Transacao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class TransacaoRepository {

    private final Connection connection;

    public TransacaoRepository(Connection connection) {
        this.connection = connection;
    }

    // Insere nova transação de criptomoeda (compras ou vendas concluídas)
    public void inserirTransacao(int idOrdemCompra,
                                 int idOrdemVenda,
                                 int idMoeda,
                                 BigDecimal quantidadeExecutada,
                                 BigDecimal precoExecutado) throws SQLException {
        String sql = "INSERT INTO Transacao (id_ordem_compra, id_ordem_venda, id_moeda, quantidade_executada, preco_executado, data_hora) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idOrdemCompra);
            stmt.setInt(2, idOrdemVenda);
            stmt.setInt(3, idMoeda);
            stmt.setBigDecimal(4, quantidadeExecutada);
            stmt.setBigDecimal(5, precoExecutado);
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        }
    }
}
