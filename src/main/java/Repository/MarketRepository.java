package Repository;

import static Database.DBConnection.getConnection;
import model.Moeda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MarketRepository {

    /**
     * Retorna todas as moedas com valor atual, variação 24h e volume negociado nas últimas 24h.
     */
    public static List<Moeda> getTodasAsMoedas() {
        List<Moeda> moedas = new ArrayList<>();
        String sql = """
            SELECT 
              m.id_moeda,
              m.nome,
              m.simbolo,
              pm_pre.preco_em_eur  AS valor_atual,
              pm_old.preco_em_eur  AS valor_24h,
              t24.volume24h
            FROM Moeda m
            /* preço atual */
            LEFT JOIN (
                SELECT p1.id_moeda, p1.preco_em_eur
                  FROM PrecoMoeda p1
                 WHERE p1.timestamp_hora = (
                       SELECT MAX(x.timestamp_hora)
                         FROM PrecoMoeda x
                        WHERE x.id_moeda = p1.id_moeda
                   )
            ) pm_pre ON pm_pre.id_moeda = m.id_moeda
            /* preço 24h atrás */
            LEFT JOIN (
                SELECT p2.id_moeda, p2.preco_em_eur
                  FROM PrecoMoeda p2
                 WHERE p2.timestamp_hora = (
                       SELECT MAX(x.timestamp_hora)
                         FROM PrecoMoeda x
                        WHERE x.id_moeda = p2.id_moeda
                          AND x.timestamp_hora <= DATEADD(hour, -24, GETDATE())
                   )
            ) pm_old ON pm_old.id_moeda = m.id_moeda
            /* volume negociado nas últimas 24h (sum of quantidade * preco) */
            LEFT JOIN (
                SELECT id_moeda,
                       SUM(quantidade * preco_unitario_eur) AS volume24h
                  FROM Transacao
                 WHERE data_hora >= DATEADD(hour, -24, GETDATE())
                 GROUP BY id_moeda
            ) t24 ON t24.id_moeda = m.id_moeda
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id       = rs.getInt("id_moeda");
                String nome  = rs.getString("nome");
                String sim   = rs.getString("simbolo");

                BigDecimal valAtual = rs.getBigDecimal("valor_atual");
                if (valAtual == null) valAtual = BigDecimal.ZERO;

                BigDecimal val24h = rs.getBigDecimal("valor_24h");
                if (val24h == null) val24h = BigDecimal.ZERO;

                BigDecimal vol24h = rs.getBigDecimal("volume24h");
                if (vol24h == null) vol24h = BigDecimal.ZERO;

                BigDecimal variacao = calcularVariacao(val24h, valAtual);

                moedas.add(new Moeda(id, nome, sim, valAtual, variacao, vol24h));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return moedas;
    }

    /**
     * Insere novo snapshot de preços em PrecoMoeda (uso interno, hourly).
     * Não grava mais volume, apenas preço.
     */
    public static void gravarSnapshot(Map<Integer, Moeda> moedas) {
        String sqlPreco = """
            INSERT INTO PrecoMoeda (id_moeda, preco_em_eur, timestamp_hora)
            VALUES (?, ?, ?)
            """;

        try (Connection conn = getConnection();
             PreparedStatement stmtPreco = conn.prepareStatement(sqlPreco)) {

            Timestamp agora = Timestamp.valueOf(LocalDateTime.now());

            for (Moeda moeda : moedas.values()) {
                stmtPreco.setInt(1, moeda.getIdMoeda());
                stmtPreco.setBigDecimal(2, moeda.getValorAtual().setScale(8, RoundingMode.HALF_UP));
                stmtPreco.setTimestamp(3, agora);
                stmtPreco.addBatch();
            }
            stmtPreco.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adiciona nova criptomoeda ao sistema.
     * Insere em Moeda (tipo='crypto') e cria snapshot inicial em PrecoMoeda.
     */
    public static boolean addNewCoin(String nome, String simbolo, String imageName, BigDecimal initialValue) {
        String insertMoeda = """
            INSERT INTO Moeda (nome, simbolo, foto, id_tipo)
                 VALUES (?, ?, ?, 
                         (SELECT id_tipo FROM MoedaTipo WHERE tipo = 'crypto'))
            """;
        String insertPreco = """
            INSERT INTO PrecoMoeda (id_moeda, preco_em_eur, timestamp_hora)
                 VALUES (?, ?, ?)
            """;

        try (Connection conn = getConnection();
             PreparedStatement stmtMoeda = conn.prepareStatement(insertMoeda, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtPreco = conn.prepareStatement(insertPreco)) {

            conn.setAutoCommit(false);

            stmtMoeda.setString(1, nome);
            stmtMoeda.setString(2, simbolo);
            stmtMoeda.setString(3, imageName);
            if (stmtMoeda.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }

            try (ResultSet rs = stmtMoeda.getGeneratedKeys()) {
                if (!rs.next()) {
                    conn.rollback();
                    return false;
                }
                int newId = rs.getInt(1);

                Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
                stmtPreco.setInt(1, newId);
                stmtPreco.setBigDecimal(2, initialValue.setScale(8, RoundingMode.HALF_UP));
                stmtPreco.setTimestamp(3, agora);
                stmtPreco.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Atualiza o nome, símbolo e foto da moeda e adiciona um novo preço.
     * Não grava volume.
     */
    public static void updateMoeda(Moeda moeda) throws SQLException {
        String updateSql = "UPDATE Moeda SET nome = ?, simbolo = ?, foto = ? WHERE id_moeda = ?";
        String insertPrecoSql = """
            INSERT INTO PrecoMoeda (id_moeda, preco_em_eur, timestamp_hora)
                 VALUES (?, ?, ?)
            """;

        try (Connection conn = getConnection();
             PreparedStatement upd = conn.prepareStatement(updateSql);
             PreparedStatement insPreco = conn.prepareStatement(insertPrecoSql)) {

            conn.setAutoCommit(false);

            upd.setString(1, moeda.getNome());
            upd.setString(2, moeda.getSimbolo());
            upd.setString(3, moeda.getFoto());
            upd.setInt(4, moeda.getIdMoeda());
            upd.executeUpdate();

            Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
            insPreco.setInt(1, moeda.getIdMoeda());
            insPreco.setBigDecimal(2, moeda.getValorAtual());
            insPreco.setTimestamp(3, agora);
            insPreco.executeUpdate();

            conn.commit();
        }
    }

    /**
     * Exclui uma moeda de todas as tabelas referenciadas antes de deletar de Moeda.
     */
    public static void deleteMoeda(int idMoeda) throws SQLException {
        String[] deletes = {
                "DELETE FROM Portfolio    WHERE id_moeda = ?",
                "DELETE FROM Ordem        WHERE id_moeda = ?",
                "DELETE FROM Transacao    WHERE id_moeda = ?",
                "DELETE FROM PrecoMoeda   WHERE id_moeda = ?",
                "DELETE FROM Moeda        WHERE id_moeda = ?"
        };

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            for (String d : deletes) {
                try (PreparedStatement stmt = conn.prepareStatement(d)) {
                    stmt.setInt(1, idMoeda);
                    stmt.executeUpdate();
                }
            }
            conn.commit();
        }
    }

    /**
     * Retorna dados para gráfico de linha de preços (filtrado).
     */
    public static List<javafx.scene.chart.XYChart.Data<String, Number>>
    getHistoricoPorMoedaFiltrado(int idMoeda, String intervalo) {

        List<javafx.scene.chart.XYChart.Data<String, Number>> dados = new ArrayList<>();

        String intervaloSQL = switch (intervalo) {
            case "1D" -> "DATEADD(day, -1, GETDATE())";
            case "1W" -> "DATEADD(week, -1, GETDATE())";
            case "1M" -> "DATEADD(month, -1, GETDATE())";
            case "3M" -> "DATEADD(month, -3, GETDATE())";
            case "1Y" -> "DATEADD(year, -1, GETDATE())";
            default -> null;
        };

        String sql = "SELECT timestamp_hora, preco_em_eur FROM PrecoMoeda WHERE id_moeda = ? ";
        if (intervaloSQL != null) {
            sql += "AND timestamp_hora >= " + intervaloSQL + " ";
        }
        sql += "ORDER BY timestamp_hora ASC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMoeda);
            try (ResultSet rs = stmt.executeQuery()) {
                DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");
                DateTimeFormatter dataFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                while (rs.next()) {
                    LocalDateTime dt = rs.getTimestamp("timestamp_hora").toLocalDateTime();
                    BigDecimal preco = rs.getBigDecimal("preco_em_eur");

                    String xValue = switch (intervalo) {
                        case "1D" -> dt.toLocalTime().format(horaFmt);
                        default  -> dt.toLocalDate().format(dataFmt);
                    };
                    dados.add(new javafx.scene.chart.XYChart.Data<>(xValue, preco));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dados;
    }

    /**
     * Calcula variação percentual entre 'antigo' e 'atual'.
     */
    public static BigDecimal calcularVariacao(BigDecimal antigo, BigDecimal atual) {
        if (antigo == null || antigo.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return atual.subtract(antigo)
                .divide(antigo, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
