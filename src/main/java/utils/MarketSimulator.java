package utils;

import model.Moeda;
import Repository.MarketRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class MarketSimulator {

    // Mapa em memória com o estado atual das moedas
    private static final Map<Integer, Moeda> moedasAtuais = new ConcurrentHashMap<>();
    private static final Random rand = new Random();
    // Volatilidade (0,5% por minuto)
    private static final double VOLATILIDADE = 0.005;

    // Scheduler com duas threads: uma para simular, outra para gravar
    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);
    private static boolean iniciado = false;

    /**
     * Inicia o simulador: carrega preços iniciais, simula em memória a cada minuto
     * e grava snapshot horário no BD.
     */
    public static synchronized void startSimulador() {
        if (iniciado) return;
        iniciado = true;

        // 1) Carrega dados iniciais de todas as moedas
        List<Moeda> lista = MarketRepository.getMoedasOrdenadas(
                "", "Valor Atual", false);
        for (Moeda m : lista) {
            moedasAtuais.put(m.getIdMoeda(), m);
        }

        // 2) Agendamento minuto a minuto (simulação em memória)
        scheduler.scheduleAtFixedRate(() -> {
            moedasAtuais.values().forEach(m -> {
                BigDecimal anterior = m.getValorAtual();
                BigDecimal novo = aplicarRandomWalk(anterior);
                m.setValorAtual(novo);
            });
            System.out.println("🔄 Preços simulados em memória.");
        }, 0, 1, MINUTES);

        // 3) Agendamento hora a hora (persistência em batch)
        scheduler.scheduleAtFixedRate(() -> {
            MarketRepository.gravarSnapshot(moedasAtuais);
            System.out.println("🕒 Snapshot horário gravado na BD.");
        }, 1, 1, TimeUnit.HOURS);

        System.out.println("🚀 Simulador de mercado iniciado.");
    }

    /**
     * Aplica um random walk multiplicativo simples.
     */
    private static BigDecimal aplicarRandomWalk(BigDecimal anterior) {
        double factor = 1 + (rand.nextDouble() * 2 - 1) * VOLATILIDADE;
        return anterior
                .multiply(BigDecimal.valueOf(factor))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Para o simulador e limpa recursos.
     */
    public static synchronized void stopSimulador() {
        if (!iniciado) return;
        scheduler.shutdownNow();
        iniciado = false;
        moedasAtuais.clear();
        System.out.println("🛑 Simulador de mercado parado.");
    }

    /**
     * Retorna o mapa de moedas simuladas (para consumo por UI/testing).
     */
    public static Map<Integer, Moeda> getMoedasSimuladas() {
        return moedasAtuais;
    }
}