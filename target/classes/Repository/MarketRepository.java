package Repository;

import Database.DBConnection;
import javafx.scene.chart.XYChart;
import model.Moeda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

        try (Connection conn = DBConnection.getConnection();
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


    public static boolean addNewCoin(String name, String symbol, String imageName, BigDecimal initialValue) {
        String sql = "INSERT INTO moeda (nome, simbolo) VALUES (?, ?)";
        String sqlHistory = "INSERT INTO historico_valores (id_moeda, valor, volume, timestamp) VALUES (?, ?, ?, NOW())";

        try (Connection conn = DBConnection.getConnection();
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

        try (Connection conn = DBConnection.getConnection();
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
}
