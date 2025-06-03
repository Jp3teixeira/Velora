package utils;

import Database.DBConnection;
import model.Moeda;
import Repository.MarketRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

public class MarketSimulator {

    private static final Map<Integer, Moeda> moedasAtuais = new ConcurrentHashMap<>();
    private static LocalDateTime ultimaGravacao = LocalDateTime.now();
    private static final Random rand = new Random();
    private static boolean agendadorIniciado = false;

    public static void startSimulador() {
        if (agendadorIniciado) return;
        agendadorIniciado = true;

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            simularValoresEmMemoria();
            verificarGravacaoBD();
        }, 0, 1, TimeUnit.SECONDS);

        System.out.println("🚀 Simulador de mercado iniciado (atualização por segundo).");
    }

    private static void simularValoresEmMemoria() {
        String sqlMoedas = "SELECT id_moeda FROM Moeda";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmtMoedas = conn.prepareStatement(sqlMoedas);
             ResultSet rsMoedas = stmtMoedas.executeQuery()) {

            while (rsMoedas.next()) {
                int id = rsMoedas.getInt("id_moeda");
                Moeda moeda = moedasAtuais.get(id);
                if (moeda == null) {
                    moeda = carregarUltimosDadosDaMoeda(conn, id);
                }
                BigDecimal valorAnterior = moeda.getValorAtual();
                BigDecimal novoValor = aplicarVariacao(valorAnterior);
                BigDecimal novoVolume = gerarVolume();

                moeda.setValorAtual(novoValor);
                moeda.setVolumeMercado(novoVolume);
                moedasAtuais.put(id, moeda);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Moeda carregarUltimosDadosDaMoeda(Connection conn, int idMoeda) throws SQLException {
        String sqlPreco = """
            SELECT TOP 1 preco_em_eur
              FROM PrecoMoeda
             WHERE id_moeda = ?
             ORDER BY timestamp_hora DESC
            """;
        String sqlVolume = """
            SELECT TOP 1 volume
              FROM VolumeMercado
             WHERE id_moeda = ?
             ORDER BY timestamp_hora DESC
            """;
        String sqlMoeda = "SELECT nome, simbolo FROM Moeda WHERE id_moeda = ?";

        String nome = "";
        String simbolo = "";
        BigDecimal valor = BigDecimal.valueOf(100 + rand.nextDouble() * 100);
        BigDecimal volume = BigDecimal.valueOf(1000 + rand.nextDouble() * 9000);

        try (PreparedStatement stmtMoeda = conn.prepareStatement(sqlMoeda)) {
            stmtMoeda.setInt(1, idMoeda);
            try (ResultSet rs = stmtMoeda.executeQuery()) {
                if (rs.next()) {
                    nome = rs.getString("nome");
                    simbolo = rs.getString("simbolo");
                }
            }
        }

        try (PreparedStatement stmtPreco = conn.prepareStatement(sqlPreco)) {
            stmtPreco.setInt(1, idMoeda);
            try (ResultSet rsPreco = stmtPreco.executeQuery()) {
                if (rsPreco.next()) {
                    BigDecimal dbValor = rsPreco.getBigDecimal("preco_em_eur");
                    if (dbValor != null) valor = dbValor;
                }
            }
        }

        try (PreparedStatement stmtVolume = conn.prepareStatement(sqlVolume)) {
            stmtVolume.setInt(1, idMoeda);
            try (ResultSet rsVol = stmtVolume.executeQuery()) {
                if (rsVol.next()) {
                    BigDecimal dbVol = rsVol.getBigDecimal("volume");
                    if (dbVol != null) volume = dbVol;
                }
            }
        }

        return new Moeda(idMoeda, nome, simbolo, valor, BigDecimal.ZERO, volume);
    }

    private static BigDecimal aplicarVariacao(BigDecimal valorAnterior) {
        double variacao = 1 + ((rand.nextDouble() * 6 - 3) / 100.0);
        return valorAnterior.multiply(BigDecimal.valueOf(variacao))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal gerarVolume() {
        return BigDecimal.valueOf(1000 + rand.nextDouble() * 9000)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static void verificarGravacaoBD() {
        if (Duration.between(ultimaGravacao, LocalDateTime.now()).toHours() >= 1) {
            MarketRepository.gravarSnapshot(moedasAtuais);
            ultimaGravacao = LocalDateTime.now();
            System.out.println("🕒 Snapshot de mercado guardado na BD.");
        }
    }

    public static Map<Integer, Moeda> getMoedasAtuais() {
        return moedasAtuais;
    }
}
