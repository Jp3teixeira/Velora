package utils;

import Database.DBConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MarketSimulator {

    private static boolean agendadorIniciado = false;

    public static void simularValores() {
        String select = "SELECT id_moeda FROM moeda";
        String selectUltimo = "SELECT valor FROM historico_valores WHERE id_moeda = ? ORDER BY timestamp DESC LIMIT 1";
        String insert = "INSERT INTO historico_valores (id_moeda, valor, volume) VALUES (?, ?, ?)";

        int moedasProcessadas = 0;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmtSelectMoedas = conn.prepareStatement(select);
             ResultSet rsMoedas = stmtSelectMoedas.executeQuery()) {

            Random rand = new Random();

            while (rsMoedas.next()) {
                int idMoeda = rsMoedas.getInt("id_moeda");
                moedasProcessadas++;

                double valorAnterior;

                try (PreparedStatement stmtUltimo = conn.prepareStatement(selectUltimo)) {
                    stmtUltimo.setInt(1, idMoeda);
                    ResultSet rsUltimo = stmtUltimo.executeQuery();
                    if (rsUltimo.next()) {
                        valorAnterior = rsUltimo.getDouble("valor");
                    } else {
                        valorAnterior = 100 + rand.nextDouble() * 100; // valor inicial aleatório
                    }
                }

                // Variação entre -3% e +3%
                double variacao = 1 + ((rand.nextDouble() * 6 - 3) / 100.0);
                double novoValor = Math.round(valorAnterior * variacao * 100.0) / 100.0;

                // Volume entre 1000 e 10000
                double volume = Math.round((1000 + rand.nextDouble() * 9000) * 100.0) / 100.0;

                try (PreparedStatement stmtInsert = conn.prepareStatement(insert)) {
                    stmtInsert.setInt(1, idMoeda);
                    stmtInsert.setBigDecimal(2, BigDecimal.valueOf(novoValor).setScale(2, RoundingMode.HALF_UP));
                    stmtInsert.setBigDecimal(3, BigDecimal.valueOf(volume).setScale(2, RoundingMode.HALF_UP));
                    stmtInsert.executeUpdate();
                }
            }

            // 🔥 Limpar registos com mais de 6 meses
            try (PreparedStatement stmtLimpar = conn.prepareStatement(
                    "DELETE FROM historico_valores WHERE timestamp < NOW() - INTERVAL 6 MONTH")) {
                int apagados = stmtLimpar.executeUpdate();
                System.out.println("🧹 Registos antigos apagados: " + apagados);
            }

            System.out.println("✔ Simulação concluída. Moedas processadas: " + moedasProcessadas);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void iniciarAgendador() {
        if (agendadorIniciado) return;
        agendadorIniciado = true;

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(MarketSimulator::simularValores, 0, 1, TimeUnit.HOURS);

        System.out.println("⏳ Agendador de simulação de mercado iniciado.");
    }
}
