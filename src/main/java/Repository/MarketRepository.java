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
     * Retorna todas as moedas com valor atual, valor há 24h, variação 24h e volume negociado nas últimas 24h.
     */
    public static List<Moeda> getTodasAsMoedas() {
        List<Moeda> moedas = new ArrayList<>();
        String sql = "SELECT * FROM dbo.fn_MoedaResumo(24)";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                moedas.add(new Moeda(
                        rs.getInt("id_moeda"),
                        rs.getString("nome"),
                        rs.getString("simbolo"),
                        rs.getBigDecimal("valor_atual"),
                        rs.getBigDecimal("variacao_24h"),
                        rs.getBigDecimal("volume24h")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return moedas;
    }

    /**
     * Insere novo snapshot de preços em PrecoMoeda (uso interno, hourly).
     */
    public static void gravarSnapshot(Map<Integer, Moeda> moedas) {
        String sql = """
            INSERT INTO PrecoMoeda (id_moeda, preco_em_eur, timestamp_hora)
            VALUES (?, ?, ?)
            """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
            for (Moeda m : moedas.values()) {
                ps.setInt(1, m.getIdMoeda());
                ps.setBigDecimal(2, m.getValorAtual().setScale(8, RoundingMode.HALF_UP));
                ps.setTimestamp(3, agora);
                ps.addBatch();
            }
            ps.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adiciona nova criptomoeda e registra preço inicial.
     */
    public static boolean addNewCoin(String nome,
                                     String simbolo,
                                     String imageName,
                                     BigDecimal initialValue) {
        String insertMoeda = """
            INSERT INTO Moeda (nome, simbolo, foto, id_tipo)
            VALUES (?, ?, ?, 
                    (SELECT id_tipo FROM MoedaTipo WHERE tipo = 'crypto'))
            """;
        String insertPreco = """
            INSERT INTO PrecoMoeda (id_moeda, preco_em_eur, timestamp_hora)
            VALUES (?, ?, GETDATE())
            """;

        try (Connection conn = getConnection();
             PreparedStatement psMoeda = conn.prepareStatement(insertMoeda, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psPreco = conn.prepareStatement(insertPreco)) {

            conn.setAutoCommit(false);
            psMoeda.setString(1, nome);
            psMoeda.setString(2, simbolo);
            psMoeda.setString(3, imageName);
            if (psMoeda.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }

            try (ResultSet rs = psMoeda.getGeneratedKeys()) {
                if (!rs.next()) {
                    conn.rollback();
                    return false;
                }
                int newId = rs.getInt(1);
                psPreco.setInt(1, newId);
                psPreco.setBigDecimal(2, initialValue.setScale(8, RoundingMode.HALF_UP));
                psPreco.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Atualiza metadados e registra novo preço timestamp = agora.
     */
    public static void updateMoeda(Moeda moeda) throws SQLException {
        String updSql = "UPDATE Moeda SET nome = ?, simbolo = ?, foto = ? WHERE id_moeda = ?";
        String insSql = "INSERT INTO PrecoMoeda (id_moeda, preco_em_eur, timestamp_hora) VALUES (?, ?, GETDATE())";

        try (Connection conn = getConnection();
             PreparedStatement pUp = conn.prepareStatement(updSql);
             PreparedStatement pIn = conn.prepareStatement(insSql)) {

            conn.setAutoCommit(false);
            pUp.setString(1, moeda.getNome());
            pUp.setString(2, moeda.getSimbolo());
            pUp.setString(3, moeda.getFoto());
            pUp.setInt(4, moeda.getIdMoeda());
            pUp.executeUpdate();

            pIn.setInt(1, moeda.getIdMoeda());
            pIn.setBigDecimal(2, moeda.getValorAtual().setScale(8, RoundingMode.HALF_UP));
            pIn.executeUpdate();

            conn.commit();
        }
    }

    /**
     * Exclui todo o histórico e metadados de uma moeda.
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
                try (PreparedStatement ps = conn.prepareStatement(d)) {
                    ps.setInt(1, idMoeda);
                    ps.executeUpdate();
                }
            }
            conn.commit();
        }
    }

    /**
     * Retorna histórico de preços para o gráfico, filtrado pelo intervalo.
     */
    public static List<javafx.scene.chart.XYChart.Data<String, Number>>
    getHistoricoPorMoedaFiltrado(int idMoeda, String intervalo) {

        List<javafx.scene.chart.XYChart.Data<String, Number>> dados = new ArrayList<>();
        String clause = switch (intervalo) {
            case "1D" -> "AND timestamp_hora >= DATEADD(day, -1, GETDATE())";
            case "1W" -> "AND timestamp_hora >= DATEADD(week, -1, GETDATE())";
            case "1M" -> "AND timestamp_hora >= DATEADD(month, -1, GETDATE())";
            case "3M" -> "AND timestamp_hora >= DATEADD(month, -3, GETDATE())";
            case "1Y" -> "AND timestamp_hora >= DATEADD(year, -1, GETDATE())";
            default  -> "";
        };
        String sql = String.format(
                "SELECT timestamp_hora, preco_em_eur " +
                        "FROM PrecoMoeda " +
                        "WHERE id_moeda = ? %s " +
                        "ORDER BY timestamp_hora ASC",
                clause
        );

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idMoeda);
            try (ResultSet rs = ps.executeQuery()) {
                DateTimeFormatter tfHora = DateTimeFormatter.ofPattern("HH:mm");
                DateTimeFormatter tfDT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

                while (rs.next()) {
                    LocalDateTime dt = rs.getTimestamp("timestamp_hora").toLocalDateTime();
                    BigDecimal preco = rs.getBigDecimal("preco_em_eur");

                    String xVal = "1D".equals(intervalo)
                            ? dt.toLocalTime().format(tfHora)
                            : dt.format(tfDT);

                    dados.add(new javafx.scene.chart.XYChart.Data<>(xVal, preco));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dados;
    }

    /**
     * Calcula variação percentual entre antigo e atual.
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
