package utils;

import java.math.BigDecimal;

public class SessaoAtual {
    public static int utilizadorId;
    public static String nome;
    public static String email;
    public static String tipo;
    public static boolean isSuperAdmin;
    public static BigDecimal saldoCarteira;

    // Para recuperação de senha
    public static String emailRecuperacao;
    public static int utilizadorRecuperacao;

    public static void limparSessao() {
        utilizadorId = 0;
        nome = null;
        email = null;
        tipo = null;
        isSuperAdmin = false;
        saldoCarteira = null;
    }
}
