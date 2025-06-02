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
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id_moeda FROM moeda");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id_moeda");

                Moeda moeda = moedasAtuais.getOrDefault(id, carregarUltimosDadosDaMoeda(conn, id));
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
        String sql = """
            SELECT m.nome, m.simbolo, hv.valor, hv.volume
            FROM moeda m
            LEFT JOIN historico_valores hv ON m.id_moeda = hv.id_moeda
            WHERE m.id_moeda = ?
            ORDER BY hv.timestamp DESC
            LIMIT 1
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idMoeda);
            ResultSet rs = stmt.executeQuery();

            String nome = "";
            String simbolo = "";
            BigDecimal valor = BigDecimal.valueOf(100 + rand.nextDouble() * 100);
            BigDecimal volume = BigDecimal.valueOf(1000 + rand.nextDouble() * 9000);

            if (rs.next()) {
                nome = rs.getString("nome");
                simbolo = rs.getString("simbolo");
                BigDecimal dbValor = rs.getBigDecimal("valor");
                BigDecimal dbVolume = rs.getBigDecimal("volume");

                if (dbValor != null) valor = dbValor;
                if (dbVolume != null) volume = dbVolume;
            }

            return new Moeda(idMoeda, nome, simbolo, valor, BigDecimal.ZERO, volume);
        }
    }

    private static BigDecimal aplicarVariacao(BigDecimal valorAnterior) {
        double variacao = 1 + ((rand.nextDouble() * 6 - 3) / 100.0);
        return valorAnterior.multiply(BigDecimal.valueOf(variacao)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal gerarVolume() {
        return BigDecimal.valueOf(1000 + rand.nextDouble() * 9000).setScale(2, RoundingMode.HALF_UP);
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
