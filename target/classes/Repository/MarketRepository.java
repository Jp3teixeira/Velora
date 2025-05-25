package Repository;

import Database.DBConnection;
import javafx.scene.chart.XYChart;
import model.Moeda;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MarketRepository {

    public static List<Moeda> getTodasAsMoedas() {
        List<Moeda> moedas = new ArrayList<>();

        String query = "SELECT * FROM moeda";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Moeda m = new Moeda(
                        rs.getInt("id_moeda"),
                        rs.getString("nome"),
                        rs.getString("simbolo"),
                        rs.getBigDecimal("valor_atual"),
                        rs.getBigDecimal("variacao_24h"),
                        rs.getBigDecimal("volume_mercado")
                );
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
                        .substring(0, 5); // exemplo: "14:00"
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

                // XValue ajustado conforme o intervalo
                String xValue = switch (intervalo) {
                    case "1D" -> dataHora.toLocalTime().toString().substring(0, 5); // ex: 14:00
                    default -> dataHora.toLocalDate().toString();                   // ex: 2025-05-25
                };

                dados.add(new XYChart.Data<>(xValue, valor));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dados;
    }

}
