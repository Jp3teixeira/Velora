package Repository;

import model.Transacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * Insere nova transação (compra/venda) de acordo com o esquema atual:
     * colunas: id_utilizador, id_moeda, tipo, quantidade, preco_unitario_eur, total_eur, data_hora
     */
    public void inserirTransacao(int idUtilizador,
                                 int idMoeda,
                                 String tipo,                // "COMPRA" ou "VENDA"
                                 BigDecimal quantidade,
                                 BigDecimal precoUnitarioEur) throws SQLException {
        // total_eur = quantidade * precoUnitarioEur
        BigDecimal totalEur = quantidade.multiply(precoUnitarioEur).setScale(8, RoundingMode.HALF_UP);

        String sql = """
            INSERT INTO Transacao
              (id_utilizador, id_moeda, tipo, quantidade, preco_unitario_eur, total_eur, data_hora)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idUtilizador);
            stmt.setInt(2, idMoeda);
            stmt.setString(3, tipo);
            stmt.setBigDecimal(4, quantidade.setScale(8, RoundingMode.HALF_UP));
            stmt.setBigDecimal(5, precoUnitarioEur.setScale(8, RoundingMode.HALF_UP));
            stmt.setBigDecimal(6, totalEur);
            stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        }
    }
}
