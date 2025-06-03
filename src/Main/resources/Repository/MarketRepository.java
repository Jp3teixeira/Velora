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
     * Retorna todas as moedas com valor atual, variação 24h e volume 24h.
     */
    public static List<Moeda> getTodasAsMoedas() {
        List<Moeda> moedas = new ArrayList<>();
        String sql = """
            SELECT 
              m.id_moeda,
              m.nome,
              m.simbolo,
              pm_pre.preco_em_eur AS valor_atual,
              pm_old.preco_em_eur AS valor_24h,
              vm24.volume24h
            FROM Moeda m
            LEFT JOIN (
                SELECT id_moeda, preco_em_eur
                  FROM PrecoMoeda
                 WHERE timestamp_hora = (
                       SELECT MAX(timestamp_hora)
                         FROM PrecoMoeda p2
                        WHERE p2.id_moeda = PrecoMoeda.id_moeda
                   )
            ) pm_pre ON pm_pre.id_moeda = m.id_moeda
            LEFT JOIN (
                SELECT id_moeda, preco_em_eur
                  FROM PrecoMoeda
                 WHERE timestamp_hora = (
                       SELECT MAX(timestamp_hora)
                         FROM PrecoMoeda p2
                        WHERE p2.id_moeda = PrecoMoeda.id_moeda
                          AND timestamp_hora <= DATEADD(hour, -24, GETDATE())
                   )
            ) pm_old ON pm_old.id_moeda = m.id_moeda
            LEFT JOIN (
                SELECT id_moeda, SUM(volume) AS volume24h
                  FROM VolumeMercado
                 WHERE timestamp_hora >= DATEADD(hour, -24, GETDATE())
                 GROUP BY id_moeda
            ) vm24 ON vm24.id_moeda = m.id_moeda;
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id_moeda");
                String nome = rs.getString("nome");
                String simbolo = rs.getString("simbolo");

                BigDecimal valorAtual = rs.getBigDecimal("valor_atual");
                if (valorAtual == null) valorAtual = BigDecimal.ZERO;

                BigDecimal valor24h = rs.getBigDecimal("valor_24h");
                if (valor24h == null) valor24h = BigDecimal.ZERO;

                BigDecimal volume24h = rs.getBigDecimal("volume24h");
                if (volume24h == null) volume24h = BigDecimal.ZERO.setScale(2);

                BigDecimal variacao24h = calcularVariacao(valor24h, valorAtual);

                moedas.add(new Moeda(id, nome, simbolo, valorAtual, variacao24h, volume24h));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return moedas;
    }

    /**
     * Insere novo snapshot (hora a hora) em PrecoMoeda e VolumeMercado.
     * 'moedas' mapeia idMoeda→Moeda com valores preenchidos em tempo real.
     */
    public static void gravarSnapshot(Map<Integer, Moeda> moedas) {
        String sqlPreco = "INSERT INTO PrecoMoeda (id_moeda, preco_em_eur, timestamp_hora) VALUES (?, ?, ?)";
        String sqlVolume = "INSERT INTO VolumeMercado (id_moeda, volume, timestamp_hora) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmtPreco = conn.prepareStatement(sqlPreco);
             PreparedStatement stmtVolume = conn.prepareStatement(sqlVolume)) {

            Timestamp agora = Timestamp.valueOf(LocalDateTime.now());

            for (Moeda moeda : moedas.values()) {
                int idMoeda = moeda.getIdMoeda();

                // Preço
                stmtPreco.setInt(1, idMoeda);
                stmtPreco.setBigDecimal(2, moeda.getValorAtual().setScale(8, RoundingMode.HALF_UP));
                stmtPreco.setTimestamp(3, agora);
                stmtPreco.addBatch();

                // Volume
                stmtVolume.setInt(1, idMoeda);
                stmtVolume.setBigDecimal(2, moeda.getVolumeMercado().setScale(8, RoundingMode.HALF_UP));
                stmtVolume.setTimestamp(3, agora);
                stmtVolume.addBatch();
            }

            stmtPreco.executeBatch();
            stmtVolume.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adiciona nova criptomoeda ao sistema.
     * Insere em Moeda (nome, simbolo, tipo='crypto'), em PrecoMoeda (valor inicial),
     * e em VolumeMercado (volume inicial zero).
     */
    public static boolean addNewCoin(String nome, String simbolo, String imageName, BigDecimal initialValue) {
        String insertMoeda = "INSERT INTO Moeda (nome, simbolo, tipo) VALUES (?, ?, 'crypto')";
        String insertPreco = "INSERT INTO PrecoMoeda (id_moeda, preco_em_eur, timestamp_hora) VALUES (?, ?, ?)";
        String insertVolume = "INSERT INTO VolumeMercado (id_moeda, volume, timestamp_hora) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmtMoeda = conn.prepareStatement(insertMoeda, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtPreco = conn.prepareStatement(insertPreco);
             PreparedStatement stmtVolume = conn.prepareStatement(insertVolume)) {

            conn.setAutoCommit(false);

            // 1. Insere na tabela Moeda
            stmtMoeda.setString(1, nome);
            stmtMoeda.setString(2, simbolo);
            int affected = stmtMoeda.executeUpdate();
            if (affected == 0) {
                conn.rollback();
                return false;
            }

            // Obtém o ID gerado
            ResultSet rs = stmtMoeda.getGeneratedKeys();
            if (!rs.next()) {
                conn.rollback();
                return false;
            }
            int newId = rs.getInt(1);

            // 2. Insere valor inicial em PrecoMoeda
            Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
            stmtPreco.setInt(1, newId);
            stmtPreco.setBigDecimal(2, initialValue);
            stmtPreco.setTimestamp(3, agora);
            stmtPreco.executeUpdate();

            // 3. Insere volume inicial zero em VolumeMercado
            stmtVolume.setInt(1, newId);
            stmtVolume.setBigDecimal(2, BigDecimal.ZERO);
            stmtVolume.setTimestamp(3, agora);
            stmtVolume.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Atualiza dados de uma moeda:
     * - renomeia 'nome' e 'simbolo' em Moeda.
     * - insere novo preço em PrecoMoeda para refletir 'valorAtual'.
     * - insere novo volume em VolumeMercado para refletir 'volumeMercado'.
     */
    public static void updateMoeda(Moeda moeda) throws SQLException {
        String updateMoedaSql = "UPDATE Moeda SET nome = ?, simbolo = ? WHERE id_moeda = ?";
        String insertPrecoSql = "INSERT INTO PrecoMoeda (id_moeda, preco_em_eur, timestamp_hora) VALUES (?, ?, ?)";
        String insertVolumeSql = "INSERT INTO VolumeMercado (id_moeda, volume, timestamp_hora) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmtUpd = conn.prepareStatement(updateMoedaSql);
             PreparedStatement stmtPreco = conn.prepareStatement(insertPrecoSql);
             PreparedStatement stmtVolume = conn.prepareStatement(insertVolumeSql)) {

            conn.setAutoCommit(false);

            // Atualiza nome e símbolo
            stmtUpd.setString(1, moeda.getNome());
            stmtUpd.setString(2, moeda.getSimbolo());
            stmtUpd.setInt(3, moeda.getIdMoeda());
            stmtUpd.executeUpdate();

            // Insere novo preço
            Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
            stmtPreco.setInt(1, moeda.getIdMoeda());
            stmtPreco.setBigDecimal(2, moeda.getValorAtual());
            stmtPreco.setTimestamp(3, agora);
            stmtPreco.executeUpdate();

            // Insere novo volume
            stmtVolume.setInt(1, moeda.getIdMoeda());
            stmtVolume.setBigDecimal(2, moeda.getVolumeMercado());
            stmtVolume.setTimestamp(3, agora);
            stmtVolume.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * Exclui uma moeda de todas as tabelas:
     * - VolumeMercado, PrecoMoeda, Portfolio, Ordem e Transacao (se existirem),
     *   depois exclui da tabela Moeda.
     */
    public static void deleteMoeda(int idMoeda) throws SQLException {
        String deleteVolume   = "DELETE FROM VolumeMercado WHERE id_moeda = ?";
        String deletePreco    = "DELETE FROM PrecoMoeda WHERE id_moeda = ?";
        String deletePortfolio= "DELETE FROM Portfolio WHERE id_moeda = ?";
        String deleteOrdem    = "DELETE FROM Ordem WHERE id_moeda = ?";
        String deleteTransacao= "DELETE FROM TransacaoCrypto WHERE id_moeda = ?";
        String deleteMoedaSql = "DELETE FROM Moeda WHERE id_moeda = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmtVol = conn.prepareStatement(deleteVolume);
             PreparedStatement stmtPre = conn.prepareStatement(deletePreco);
             PreparedStatement stmtPort = conn.prepareStatement(deletePortfolio);
             PreparedStatement stmtOrd = conn.prepareStatement(deleteOrdem);
             PreparedStatement stmtTrans = conn.prepareStatement(deleteTransacao);
             PreparedStatement stmtMd = conn.prepareStatement(deleteMoedaSql)) {

            conn.setAutoCommit(false);

            stmtVol.setInt(1, idMoeda);
            stmtVol.executeUpdate();

            stmtPre.setInt(1, idMoeda);
            stmtPre.executeUpdate();

            stmtPort.setInt(1, idMoeda);
            stmtPort.executeUpdate();

            stmtOrd.setInt(1, idMoeda);
            stmtOrd.executeUpdate();

            stmtTrans.setInt(1, idMoeda);
            stmtTrans.executeUpdate();

            stmtMd.setInt(1, idMoeda);
            stmtMd.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * Retorna dados formatados para gráfico de linha de preços de uma moeda,
     * filtrados por intervalo: "1D", "1W", "1M", "3M", "1Y", ou "MAX".
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
            ResultSet rs = stmt.executeQuery();

            DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dataFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            while (rs.next()) {
                LocalDateTime dt = rs.getTimestamp("timestamp_hora").toLocalDateTime();
                BigDecimal preco = rs.getBigDecimal("preco_em_eur");

                String xValue = switch (intervalo) {
                    case "1D" -> dt.toLocalTime().format(horaFmt);
                    default -> dt.toLocalDate().format(dataFmt);
                };
                dados.add(new javafx.scene.chart.XYChart.Data<>(xValue, preco));
            }
            rs.close();
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
