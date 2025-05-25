package utils;

import Database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MarketSimulator {

    public static void simularValores() {
        String select = "SELECT id_moeda, valor_atual FROM moeda";
        String insert = "INSERT INTO historico_valores (id_moeda, valor) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmtSelect = conn.prepareStatement(select);
             ResultSet rs = stmtSelect.executeQuery();
             PreparedStatement stmtInsert = conn.prepareStatement(insert)) {

            Random rand = new Random();

            while (rs.next()) {
                int id = rs.getInt("id_moeda");
                double valorAtual = rs.getDouble("valor_atual");

                // Variação entre -1% e +1%
                double variacao = 1 + (rand.nextDouble() - 0.5) / 50;
                double novoValor = Math.round(valorAtual * variacao * 10000.0) / 10000.0;

                stmtInsert.setInt(1, id);
                stmtInsert.setDouble(2, novoValor);
                stmtInsert.executeUpdate();
            }

            System.out.println("✔ Simulação de valores concluída com sucesso.");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public static void iniciarAgendador() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            simularValores();
        }, 0, 1, TimeUnit.HOURS);  // executa agora e depois a cada 1 hora

        System.out.println("⏳ Agendador de mercado iniciado.");
    }

}
