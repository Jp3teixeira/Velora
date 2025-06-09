package utils;

import Database.DBConnection;
import Repository.MarketRepository;
import model.Moeda;

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
        }, 0, 1, TimeUnit.MINUTES);

        System.out.println("🚀 Simulador de mercado iniciado (atualização por minuto).");
    }

    private static void simularValoresEmMemoria() {
        String sqlMoedas = "SELECT id_moeda FROM Moeda";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlMoedas);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id_moeda");
                Moeda moeda = moedasAtuais.get(id);
                if (moeda == null) {
                    try {
                        moeda = carregarUltimosDadosDaMoeda(conn, id);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        continue; // pula essa moeda
                    }
                }
                // simula flutuação de preço e volume em memória
                BigDecimal valorAnterior = moeda.getValorAtual();
                BigDecimal novoValor = aplicarVariacao(valorAnterior);
                BigDecimal novoVolume = gerarVolume();

                moeda.setValorAtual(novoValor);
                moeda.setVolumeMercado(novoVolume);
                moedasAtuais.put(id, moeda);
            }

        } catch (SQLException e) {
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
        String sqlMeta = "SELECT nome, simbolo FROM Moeda WHERE id_moeda = ?";
        String sqlVolume24h = """
            SELECT volume24h
              FROM vw_MoedaVolume24h
             WHERE id_moeda = ?
            """;

        String nome = "";
        String simbolo = "";
        BigDecimal valor = BigDecimal.valueOf(100 + rand.nextDouble() * 100);
        BigDecimal volume = BigDecimal.ZERO;

        // carrega nome/símbolo
        try (PreparedStatement stm = conn.prepareStatement(sqlMeta)) {
            stm.setInt(1, idMoeda);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    nome = rs.getString("nome");
                    simbolo = rs.getString("simbolo");
                }
            }
        }

        // carrega preço mais recente
        try (PreparedStatement stm = conn.prepareStatement(sqlPreco)) {
            stm.setInt(1, idMoeda);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    BigDecimal db = rs.getBigDecimal("preco_em_eur");
                    if (db != null) valor = db;
                }
            }
        }

        // carrega volume 24h via view
        try (PreparedStatement stm = conn.prepareStatement(sqlVolume24h)) {
            stm.setInt(1, idMoeda);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    BigDecimal db = rs.getBigDecimal("volume24h");
                    if (db != null) volume = db;
                }
            }
        } catch (SQLException e) {
            // se a view não existir, deixa volume = 0
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

    public static Map<Integer, Moeda> getMoedasSimuladas() {
        return moedasAtuais;
    }
}
