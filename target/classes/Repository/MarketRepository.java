package Repository;

import Database.DBConnection;
import javafx.scene.chart.XYChart;
import model.Moeda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MarketRepository {

    public static List<Moeda> getTodasAsMoedas() {
        List<Moeda> moedas = new ArrayList<>();

        String query = "SELECT id_moeda, nome, simbolo FROM moeda";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int id = rs.getInt("id_moeda");
                String nome = rs.getString("nome");
                String simbolo = rs.getString("simbolo");

                BigDecimal valorAtual = getValorAtual(id);
                BigDecimal valor24h = getValor24hAtras(id);
                BigDecimal variacao24h = calcularVariacao(valor24h, valorAtual);
                BigDecimal volume24h = getVolumeUltimas24h(id);

                Moeda m = new Moeda(id, nome, simbolo, valorAtual, variacao24h, volume24h);
                moedas.add(m);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return moedas;
    }

    public static List<XYChart.Data<String, Number>> getHistoricoPorMoeda(int idMoeda) {
        List<XYChart.Data<String, Number>> dados = new ArrayList<>();
        String sql = "SELECT timestamp, valor FROM historico_valores WHERE id_moeda = ? ORDER BY timestamp ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMoeda);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String hora = rs.getTimestamp("timestamp")
                        .toLocalDateTime()
                        .toLocalTime()
                        .toString()
                        .substring(0, 5); // ex: "14:00"
                BigDecimal valor = rs.getBigDecimal("valor");
                dados.add(new XYChart.Data<>(hora, valor));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dados;
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

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("timestamp");
                BigDecimal valor = rs.getBigDecimal("valor");

                LocalDateTime dataHora = ts.toLocalDateTime();

                String xValue = switch (intervalo) {
                    case "1D" -> dataHora.toLocalTime().toString().substring(0, 5);
                    default -> dataHora.toLocalDate().toString();
                };

                dados.add(new XYChart.Data<>(xValue, valor));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dados;
    }

    public static BigDecimal getValorAtual(int idMoeda) {
        String sql = "SELECT valor FROM historico_valores WHERE id_moeda = ? ORDER BY timestamp DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMoeda);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("valor");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return BigDecimal.ZERO;
    }

    public static BigDecimal getValor24hAtras(int idMoeda) {
        String sql = "SELECT valor FROM historico_valores " +
                "WHERE id_moeda = ? AND timestamp <= NOW() - INTERVAL 24 HOUR " +
                "ORDER BY timestamp DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMoeda);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("valor");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return BigDecimal.ZERO;
    }

    public static BigDecimal getVolumeUltimas24h(int idMoeda) {
        String sql = "SELECT SUM(volume) AS total_volume FROM historico_valores " +
                "WHERE id_moeda = ? AND timestamp >= NOW() - INTERVAL 24 HOUR";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMoeda);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("total_volume") != null ?
                        rs.getBigDecimal("total_volume").setScale(2, RoundingMode.HALF_UP) :
                        BigDecimal.ZERO;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return BigDecimal.ZERO;
    }

    public static BigDecimal calcularVariacao(BigDecimal antigo, BigDecimal atual) {
        if (antigo == null || antigo.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return atual.subtract(antigo)
                .divide(antigo, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
