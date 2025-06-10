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

    // Parâmetros do OU
    private static final double THETA = 0.3;         // velocidade de reversão
    private static final double SIGMA = 0.02;        // volatilidade
    private static final double DT = 1.0 / (24 * 60); // passo: 1 minuto em fração de dia

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
                        continue;
                    }
                }
                // Flutuação OU em memória
                BigDecimal valorAnterior = moeda.getValorAtual();
                // μ = preço médio; aqui usamos o próprio último valor como pivô
                BigDecimal mu = valorAnterior;
                BigDecimal novoValor = aplicarVariacaoOU(valorAnterior, mu, THETA, SIGMA, DT);
                BigDecimal novoVolume = gerarVolume();

                moeda.setValorAtual(novoValor);
                moeda.setVolumeMercado(novoVolume);
                moedasAtuais.put(id, moeda);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Carrega do banco os dados iniciais de uma moeda:
     *  - nome, símbolo
     *  - último preço (valorAtual)
     *  - preço de 24h atrás (valor24h)
     *  - volume negociado nas últimas 24h
     *  - variação percentual 24h (variacao24h)
     */
    private static Moeda carregarUltimosDadosDaMoeda(Connection conn, int idMoeda) throws SQLException {
        String sqlMeta      = "SELECT nome, simbolo FROM Moeda WHERE id_moeda = ?";
        String sqlPreco     = """
        SELECT TOP 1 preco_em_eur
          FROM PrecoMoeda
         WHERE id_moeda = ?
         ORDER BY timestamp_hora DESC
        """;
        String sqlPreco24h  = """
        SELECT preco_em_eur AS valor_24h
          FROM (
            SELECT preco_em_eur,
                   ROW_NUMBER() OVER (ORDER BY timestamp_hora DESC) AS rn
              FROM PrecoMoeda
             WHERE id_moeda = ?
               AND timestamp_hora <= DATEADD(hour, -24, GETDATE())
          ) t
         WHERE rn = 1
        """;
        String sqlVolume24h = """
        SELECT COALESCE(SUM(quantidade * preco_unitario_eur), 0) AS volume24h
          FROM Transacao
         WHERE id_moeda = ?
           AND data_hora >= DATEADD(hour, -24, GETDATE())
        """;

        String nome    = "";
        String simbolo = "";
        BigDecimal valorAtual  = BigDecimal.ZERO;
        BigDecimal valor24h    = BigDecimal.ZERO;
        BigDecimal volume24h   = BigDecimal.ZERO;

        // 1) Nome e símbolo
        try (PreparedStatement st = conn.prepareStatement(sqlMeta)) {
            st.setInt(1, idMoeda);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    nome    = rs.getString("nome");
                    simbolo = rs.getString("simbolo");
                }
            }
        }

        // 2) Último preço
        try (PreparedStatement st = conn.prepareStatement(sqlPreco)) {
            st.setInt(1, idMoeda);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next() && rs.getBigDecimal("preco_em_eur") != null) {
                    valorAtual = rs.getBigDecimal("preco_em_eur");
                }
            }
        }

        // 3) Preço 24h atrás
        try (PreparedStatement st = conn.prepareStatement(sqlPreco24h)) {
            st.setInt(1, idMoeda);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next() && rs.getBigDecimal("valor_24h") != null) {
                    valor24h = rs.getBigDecimal("valor_24h");
                }
            }
        }

        // 4) Volume 24h
        try (PreparedStatement st = conn.prepareStatement(sqlVolume24h)) {
            st.setInt(1, idMoeda);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    volume24h = rs.getBigDecimal("volume24h");
                }
            }
        }

        // 5) Cálculo da variação percentual 24h
        BigDecimal variacao24h = BigDecimal.ZERO;
        if (valor24h.compareTo(BigDecimal.ZERO) > 0) {
            variacao24h = valorAtual
                    .subtract(valor24h)
                    .divide(valor24h, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 6) Retorna objeto Moeda populado
        return new Moeda(
                idMoeda,
                nome,
                simbolo,
                valorAtual.setScale(2, RoundingMode.HALF_UP),
                variacao24h,
                volume24h.setScale(2, RoundingMode.HALF_UP)
        );
    }

    /**
     * Ornstein-Uhlenbeck: dX = θ(μ - X)dt + σ√dt·Z
     */
    private static BigDecimal aplicarVariacaoOU(
            BigDecimal valorAnterior,
            BigDecimal mu,
            double theta,
            double sigma,
            double dt
    ) {
        double x = valorAnterior.doubleValue();
        double m = mu.doubleValue();
        double drift = theta * (m - x) * dt;
        double diffusion = sigma * Math.sqrt(dt) * rand.nextGaussian();
        double novo = x + drift + diffusion;
        return BigDecimal.valueOf(novo).setScale(2, RoundingMode.HALF_UP);
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
