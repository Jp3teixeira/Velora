// MarketRepository.java
package Repository;

import static Database.DBConnection.getConnection;
import Database.DataAccessException;
import model.Moeda;
import utils.TradeService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import javafx.scene.chart.XYChart;

/**
 * DAO para Moeda, com CRUD e métodos utilitários estáticos.
 */
public class MarketRepository implements DAO<Moeda, Integer> {

    public MarketRepository() {}

    // --- CRUD via DAO<Moeda,Integer> ---

    @Override
    public Optional<Moeda> get(Integer id) {
        String sql = """
            SELECT id_moeda, nome, simbolo, tipo, valor_atual, variacao_24h, volume24h
              FROM Moeda
             WHERE id_moeda = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToMoeda(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao buscar Moeda por ID " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Moeda> getAll() {
        // Para simplicidade, retorna todas ordenadas por valor atual
        try {
            return getMoedasOrdenadas("", "Valor Atual", true);
        } catch (DataAccessException e) {
            throw new DataAccessException("Erro ao listar todas as Moedas", e);
        }
    }

    @Override
    public boolean save(Moeda m) {
        try {
            OptionalInt opt = addNewCoinReturnId(
                    m.getNome(), m.getSimbolo(), m.getFoto(), m.getValorAtual()
            );
            if (opt.isPresent()) {
                m.setId(opt.getAsInt());
                return true;
            }
            return false;
        } catch (DataAccessException e) {
            throw new DataAccessException("Erro ao salvar nova Moeda", e);
        }
    }

    @Override
    public boolean update(Moeda m) {
        try {
            updateMoeda(m);
            return true;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao atualizar Moeda ID " + m.getId(), e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        try {
            deleteMoeda(id);
            return true;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao eliminar Moeda ID " + id, e);
        }
    }

    // --- Métodos utilitários estáticos ---

    public static List<Moeda> getMoedasOrdenadas(String termo, String campo, boolean asc) {
        List<Moeda> moedas = new ArrayList<>();
        String coluna = colunaParaCampo(campo);
        String sql = """
            SELECT r.id_moeda, r.nome, r.simbolo,
                   r.valor_atual, r.variacao_24h, r.volume24h
              FROM dbo.v_MoedaResumo24h r
             WHERE LOWER(r.nome) LIKE ?
                OR LOWER(r.simbolo) LIKE ?
             ORDER BY r.""" + coluna + " " + (asc ? "ASC" : "DESC") + ", r.nome";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + termo.toLowerCase() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    moedas.add(mapRowToMoeda(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao buscar moedas ordenadas", e);
        }
        return moedas;
    }

    public static void gravarSnapshot(java.util.Map<Integer, Moeda> moedas) {
        String sql = "INSERT INTO PrecoMoeda (id_moeda, preco_em_eur, timestamp_hora) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
            for (Moeda m : moedas.values()) {
                ps.setInt(1, m.getId());
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
            throw new DataAccessException("Erro ao gravar snapshot de preços", e);
        }
    }

    public static List<String[]> getHistoricoCompletoParaCSV(int idMoeda) {
        List<String[]> dados = new ArrayList<>();
        String sql = """
            SELECT timestamp_hora, preco_em_eur
              FROM PrecoMoeda
             WHERE id_moeda = ?
             ORDER BY timestamp_hora ASC
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMoeda);
            try (ResultSet rs = ps.executeQuery()) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    String data = rs.getTimestamp("timestamp_hora")
                            .toLocalDateTime().format(fmt);
                    String valor = rs.getBigDecimal("preco_em_eur")
                            .setScale(8, RoundingMode.HALF_UP)
                            .toPlainString();
                    dados.add(new String[]{data, valor});
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao obter histórico completo para CSV", e);
        }
        return dados;
    }

    public static OptionalInt addNewCoinReturnId(String nome, String simbolo,
                                                 String imageName, BigDecimal initialValue) {
        String call = "{ CALL dbo.sp_AddNewCoin(?, ?, ?, ?, ?) }";
        try (Connection conn = getConnection();
             CallableStatement cstmt = conn.prepareCall(call)) {
            cstmt.setString(1, nome);
            cstmt.setString(2, simbolo);
            cstmt.setString(3, imageName);
            cstmt.setBigDecimal(4, initialValue.setScale(8, RoundingMode.HALF_UP));
            cstmt.registerOutParameter(5, Types.INTEGER);
            cstmt.execute();
            int newId = cstmt.getInt(5);
            return newId > 0 ? OptionalInt.of(newId) : OptionalInt.empty();
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao adicionar nova moeda", e);
        }
    }

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
            pUp.setInt(4, m.getId());
            pUp.executeUpdate();

            pIn.setInt(1, m.getId());
            pIn.setBigDecimal(2, m.getValorAtual().setScale(8, RoundingMode.HALF_UP));
            pIn.executeUpdate();

            conn.commit();
        }
    }

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

    public static List<XYChart.Data<String, Number>> getHistoricoPorMoedaFiltrado(
            int idMoeda, String intervalo) {
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
                clause
        );
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMoeda);
            try (ResultSet rs = ps.executeQuery()) {
                DateTimeFormatter fh = DateTimeFormatter.ofPattern("HH:mm");
                DateTimeFormatter fdt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    LocalDateTime dt = rs.getTimestamp("timestamp_hora").toLocalDateTime();
                    BigDecimal preco = rs.getBigDecimal("preco_em_eur");
                    String x = intervalo.equals("1D")
                            ? dt.toLocalTime().format(fh)
                            : dt.format(fdt);
                    dados.add(new XYChart.Data<>(x, preco));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao obter histórico filtrado de preços", e);
        }
        return dados;
    }

    private static String colunaParaCampo(String campo) {
        return switch (campo) {
            case "Variação 24h" -> "variacao_24h";
            case "Valor Atual"  -> "valor_atual";
            default              -> "valor_atual";
        };
    }

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
