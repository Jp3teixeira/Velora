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

    public static void simularValores() {
        String select = "SELECT id_moeda FROM moeda";
        String selectUltimo = "SELECT valor FROM historico_valores WHERE id_moeda = ? ORDER BY timestamp DESC LIMIT 1";
        String insert = "INSERT INTO historico_valores (id_moeda, valor, volume) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmtSelectMoedas = conn.prepareStatement(select);
             ResultSet rsMoedas = stmtSelectMoedas.executeQuery()) {

            Random rand = new Random();

            while (rsMoedas.next()) {
                int idMoeda = rsMoedas.getInt("id_moeda");

                double valorAnterior = 0;

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

            System.out.println("✔ Simulação de mercado concluída com sucesso.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void iniciarAgendador() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            simularValores();
        }, 0, 1, TimeUnit.HOURS);  // executa agora e depois a cada 1 hora

        System.out.println("⏳ Agendador de simulação de mercado iniciado.");
    }

}
