package Repository;

import static Database.DBConnection.getConnection;
import javafx.scene.chart.XYChart;
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

    public static List<Moeda> getTodasAsMoedas() {
        List<Moeda> moedas = new ArrayList<>();

        String sql = """
            SELECT 
                m.id_moeda, 
                m.nome, 
                m.simbolo,
                (SELECT valor FROM historico_valores hv1 
                 WHERE hv1.id_moeda = m.id_moeda 
                 ORDER BY timestamp DESC LIMIT 1) AS valor_atual,
                (SELECT valor FROM historico_valores hv2 
                 WHERE hv2.id_moeda = m.id_moeda 
                 AND timestamp <= NOW() - INTERVAL 24 HOUR 
                 ORDER BY timestamp DESC LIMIT 1) AS valor_24h,
                (SELECT SUM(volume) FROM historico_valores hv3 
                 WHERE hv3.id_moeda = m.id_moeda 
                 AND timestamp >= NOW() - INTERVAL 24 HOUR) AS volume_24h
            FROM moeda m
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id_moeda");
                String nome = rs.getString("nome");
                String simbolo = rs.getString("simbolo");

                BigDecimal valorAtual = rs.getBigDecimal("valor_atual") != null ? rs.getBigDecimal("valor_atual") : BigDecimal.ZERO;
                BigDecimal valor24h = rs.getBigDecimal("valor_24h") != null ? rs.getBigDecimal("valor_24h") : BigDecimal.ZERO;
                BigDecimal volume24h = rs.getBigDecimal("volume_24h") != null ? rs.getBigDecimal("volume_24h").setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

                BigDecimal variacao24h = calcularVariacao(valor24h, valorAtual);

                moedas.add(new Moeda(id, nome, simbolo, valorAtual, variacao24h, volume24h));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return moedas;
    }



    public static List<Moeda> getAllCoins() {
        List<Moeda> moedas = new ArrayList<>();
        String sql = "SELECT id_moeda, nome, simbolo, valor_atual, variacao_24h, volume_mercado FROM moeda";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id_moeda");
                String nome = rs.getString("nome");
                String simbolo = rs.getString("simbolo");
                BigDecimal valorAtual = rs.getBigDecimal("valor_atual");
                BigDecimal variacao24h = rs.getBigDecimal("variacao_24h");
                BigDecimal volumeMercado = rs.getBigDecimal("volume_mercado");

                Moeda moeda = new Moeda(id, nome, simbolo, valorAtual, variacao24h, volumeMercado);
                moedas.add(moeda);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return moedas;
    }

    public static void updateMoeda(Moeda moeda) throws SQLException {
        String sql = "UPDATE moeda SET nome=?, simbolo=?, valor_atual=?, variacao_24h=?, volume_mercado=? WHERE id_moeda=?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, moeda.getNome());
            pstmt.setString(2, moeda.getSimbolo());
            pstmt.setBigDecimal(3, moeda.getValorAtual());
            pstmt.setBigDecimal(4, moeda.getVariacao24h());
            pstmt.setBigDecimal(5, moeda.getVolumeMercado());
            pstmt.setInt(6, moeda.getIdMoeda());

            pstmt.executeUpdate();
        }
    }

    public static void deleteMoeda(int idMoeda) throws SQLException {
        Connection conn = null;
        PreparedStatement psHistorico = null;
        PreparedStatement psMoeda = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);  // começar transação

            // 1. Apagar histórico associado
            psHistorico = conn.prepareStatement("DELETE FROM historico_valores WHERE id_moeda = ?");
            psHistorico.setInt(1, idMoeda);
            psHistorico.executeUpdate();

            // 2. Apagar a moeda
            psMoeda = conn.prepareStatement("DELETE FROM moeda WHERE id_moeda = ?");
            psMoeda.setInt(1, idMoeda);
            psMoeda.executeUpdate();

            conn.commit();  // confirmar alterações
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();  // desfazer alterações em caso de erro
            }
            throw e;
        } finally {
            if (psHistorico != null) psHistorico.close();
            if (psMoeda != null) psMoeda.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }



    public static boolean addNewCoin(String name, String symbol, String imageName, BigDecimal initialValue) {
        String sql = "INSERT INTO moeda (nome, simbolo) VALUES (?, ?)";
        String sqlHistory = "INSERT INTO historico_valores (id_moeda, valor, volume, timestamp) VALUES (?, ?, ?, NOW())";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Insere a moeda
            stmt.setString(1, name);
            stmt.setString(2, symbol);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }

            // Obtém o ID gerado
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int coinId = generatedKeys.getInt(1);

                    // Insere o valor inicial no histórico
                    try (PreparedStatement stmtHistory = conn.prepareStatement(sqlHistory)) {
                        stmtHistory.setInt(1, coinId);
                        stmtHistory.setBigDecimal(2, initialValue);
                        stmtHistory.setBigDecimal(3, BigDecimal.ZERO); // Volume inicial zero
                        stmtHistory.executeUpdate();
                    }

                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<XYChart.Data<String, Number>> getHistoricoPorMoedaFiltrado(int idMoeda, String intervalo) {
        List<XYChart.Data<String, Number>> dados = new ArrayList<>();

        String intervaloSQL = switch (intervalo) {
            case "1D" -> "INTERVAL 1 DAY";
            case "1W" -> "INTERVAL 7 DAY";
            case "1M" -> "INTERVAL 1 MONTH";
            case "3M" -> "INTERVAL 3 MONTH";
            case "1Y" -> "INTERVAL 1 YEAR";
            default -> null;
        };

        String sql = "SELECT timestamp, valor FROM historico_valores WHERE id_moeda = ? ";
        if (intervaloSQL != null) {
            sql += "AND timestamp >= NOW() - " + intervaloSQL + " ";
        }
        sql += "ORDER BY timestamp ASC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMoeda);
            ResultSet rs = stmt.executeQuery();

            DateTimeFormatter horaFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dataFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("timestamp");
                BigDecimal valor = rs.getBigDecimal("valor");

                LocalDateTime dataHora = ts.toLocalDateTime();
                String xValue = switch (intervalo) {
                    case "1D" -> dataHora.toLocalTime().format(horaFormatter);
                    default -> dataHora.toLocalDate().format(dataFormatter);
                };

                dados.add(new XYChart.Data<>(xValue, valor));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dados;
    }

    public static BigDecimal calcularVariacao(BigDecimal antigo, BigDecimal atual) {
        if (antigo == null || antigo.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return atual.subtract(antigo)
                .divide(antigo, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static void gravarSnapshot(Map<Integer, Moeda> moedas) {
        String sql = "INSERT INTO historico_valores (id_moeda, valor, volume) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Moeda moeda : moedas.values()) {
                stmt.setInt(1, moeda.getIdMoeda());
                stmt.setBigDecimal(2, moeda.getValorAtual().setScale(2, RoundingMode.HALF_UP));
                stmt.setBigDecimal(3, moeda.getVolumeMercado().setScale(2, RoundingMode.HALF_UP));
                stmt.addBatch();
            }

            stmt.executeBatch();
            System.out.println("✅ Snapshot gravado na base de dados com " + moedas.size() + " moedas.");

        } catch (SQLException e) {
            System.err.println("Erro ao gravar snapshot de mercado:");
            e.printStackTrace();
        }
    }

}
