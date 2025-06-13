package Repository;

import static Database.DBConnection.getConnection;
import model.Moeda;
import utils.TradeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import javafx.scene.chart.XYChart;

public class MarketRepository {

    /**
     * Busca por nome/símbolo com filtro numérico opcional,
     * e ordena pelo critério escolhido (desc) + nome.
     *
     * @param termo    texto para LIKE em nome ou símbolo
     * @param campo    “Variação 24h”, “Valor Atual” ou “Volume Mercado”
     * @param operador “<” ou “>”
     * @param valor    valor de comparação
     * @param sortBy   “Volume 24h”, “Valor Atual” ou “Variação 24h”
     */
    /**
     * Busca por nome/símbolo e ordena por Valor Atual ou Variação 24h,
     * crescente ou decrescente.
     *
     * @param termo texto para LIKE em nome ou símbolo (já em lowercase)
     * @param campo "Valor Atual" ou "Variação 24h"
     * @param asc   true = ASC, false = DESC
     */
    public static List<Moeda> getMoedasOrdenadas(String termo,
                                                 String campo,
                                                 boolean asc) {
        List<Moeda> moedas = new ArrayList<>();
        String coluna = colunaParaCampo(campo);
        String order = asc ? "ASC" : "DESC";

        String sql = "SELECT r.id_moeda, r.nome, r.simbolo, r.valor_atual, r.variacao_24h, r.volume24h " +
                "FROM dbo.fn_MoedaResumo(24) r " +
                "WHERE LOWER(r.nome) LIKE ? OR LOWER(r.simbolo) LIKE ? " +
                "ORDER BY r." + coluna + " " + order + ", r.nome";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String like = "%" + termo + "%";
            ps.setString(1, like);
            ps.setString(2, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    moedas.add(mapRowToMoeda(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return moedas;
    }

    private static String colunaParaCampo(String campo) {
        return switch (campo) {
            case "Variação 24h" -> "variacao_24h";
            case "Valor Atual"  -> "valor_atual";
            default              -> "valor_atual";
        };
    }



    /**
     * Insere novo snapshot de preços em PrecoMoeda (uso interno, hourly).
     */
    public static void gravarSnapshot(java.util.Map<Integer, Moeda> moedas) {
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

            TradeService ts = new TradeService(conn);
            for (Integer id : moedas.keySet()) {
                ts.processarOrdensVendaMarketPendentes(id);
                ts.processarOrdensCompraMarketPendentes(id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adiciona nova criptomoeda e registra preço inicial.
     */
    public static OptionalInt addNewCoinReturnId(String nome,
                                                 String simbolo,
                                                 String imageName,
                                                 BigDecimal initialValue) {
        String insertMoeda = """
        INSERT INTO Moeda (nome, simbolo, foto, id_tipo)
        VALUES (?, ?, ?, (SELECT id_tipo FROM MoedaTipo WHERE tipo='crypto'))
    """;
        String insertPreco = """
        INSERT INTO PrecoMoeda (id_moeda, preco_em_eur, timestamp_hora)
        VALUES (?, ?, GETDATE())
    """;
        try (Connection conn = getConnection();
             PreparedStatement psM = conn.prepareStatement(insertMoeda, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psP = conn.prepareStatement(insertPreco)) {

            conn.setAutoCommit(false);
            psM.setString(1, nome);
            psM.setString(2, simbolo);
            psM.setString(3, imageName);
            if (psM.executeUpdate() == 0) {
                conn.rollback();
                return OptionalInt.empty();
            }
            try (ResultSet rs = psM.getGeneratedKeys()) {
                if (!rs.next()) {
                    conn.rollback();
                    return OptionalInt.empty();
                }
                int newId = rs.getInt(1);
                // Grava o preço inicial na tabela PrecoMoeda
                psP.setInt(1, newId);
                psP.setBigDecimal(2, initialValue.setScale(8, RoundingMode.HALF_UP));
                psP.executeUpdate();
                conn.commit();
                return OptionalInt.of(newId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return OptionalInt.empty();
        }
    }

    /**
     * Atualiza metadados e registra novo preço (timestamp = agora).
     */
    public static void updateMoeda(Moeda m) throws SQLException {
        String upd = "UPDATE Moeda SET nome=?, simbolo=?, foto=? WHERE id_moeda=?";
        String ins = "INSERT INTO PrecoMoeda (id_moeda, preco_em_eur, timestamp_hora) VALUES (?,?,GETDATE())";
        try (Connection conn = getConnection();
             PreparedStatement pUp = conn.prepareStatement(upd);
             PreparedStatement pIn = conn.prepareStatement(ins)) {

            conn.setAutoCommit(false);
            pUp.setString(1, m.getNome());
            pUp.setString(2, m.getSimbolo());
            pUp.setString(3, m.getFoto());
            pUp.setInt(4, m.getIdMoeda());
            pUp.executeUpdate();

            pIn.setInt(1, m.getIdMoeda());
            pIn.setBigDecimal(2, m.getValorAtual().setScale(8, RoundingMode.HALF_UP));
            pIn.executeUpdate();
            conn.commit();
        }
    }

    /**
     * Exclui todo o histórico e metadados de uma moeda.
     */
    public static void deleteMoeda(int idMoeda) throws SQLException {
        String[] deletes = {
                "DELETE FROM Portfolio WHERE id_moeda=?",
                "DELETE FROM Ordem WHERE id_moeda=?",
                "DELETE FROM Transacao WHERE id_moeda=?",
                "DELETE FROM PrecoMoeda WHERE id_moeda=?",
                "DELETE FROM Moeda WHERE id_moeda=?"
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
    public static List<XYChart.Data<String, Number>> getHistoricoPorMoedaFiltrado(int idMoeda,
                                                                                  String intervalo) {
        List<XYChart.Data<String, Number>> dados = new ArrayList<>();
        String clause = switch (intervalo) {
            case "1D" -> "AND timestamp_hora >= DATEADD(day,-1,GETDATE())";
            case "1W" -> "AND timestamp_hora >= DATEADD(week,-1,GETDATE())";
            case "1M" -> "AND timestamp_hora >= DATEADD(month,-1,GETDATE())";
            case "3M" -> "AND timestamp_hora >= DATEADD(month,-3,GETDATE())";
            case "1Y" -> "AND timestamp_hora >= DATEADD(year,-1,GETDATE())";
            default  -> "";
        };
        String sql = String.format(
                "SELECT timestamp_hora, preco_em_eur FROM PrecoMoeda WHERE id_moeda=? %s ORDER BY timestamp_hora ASC",
                clause);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idMoeda);
            try (ResultSet rs = ps.executeQuery()) {
                DateTimeFormatter fh = DateTimeFormatter.ofPattern("HH:mm");
                DateTimeFormatter fdt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    LocalDateTime dt = rs.getTimestamp("timestamp_hora").toLocalDateTime();
                    BigDecimal preco = rs.getBigDecimal("preco_em_eur");
                    String x = intervalo.equals("1D") ? dt.toLocalTime().format(fh) : dt.format(fdt);
                    dados.add(new XYChart.Data<>(x, preco));
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
                .divide(antigo, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    // mapeia uma linha de ResultSet para Moeda
    private static Moeda mapRowToMoeda(ResultSet rs) throws SQLException {
        return new Moeda(
                rs.getInt("id_moeda"),
                rs.getString("nome"),
                rs.getString("simbolo"),
                rs.getBigDecimal("valor_atual"),
                rs.getBigDecimal("variacao_24h"),
                rs.getBigDecimal("volume24h")
        );
    }
}
