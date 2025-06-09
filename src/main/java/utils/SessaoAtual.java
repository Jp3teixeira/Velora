package utils;

import model.Utilizador;
import java.math.BigDecimal;

public class SessaoAtual {
    public static int utilizadorId;
    public static String nome;
    public static String email;
    public static String  tipo;
    public static boolean isSuperAdmin;
    public static BigDecimal saldoCarteira;

    public static String emailRecuperacao;
    public static int utilizadorRecuperacao;

    private static Utilizador utilizador;

    public static void setUtilizador(Utilizador u) {
        utilizador = u;
    }


    public static Utilizador getUtilizador() {
        return utilizador;
    }

    public static void limparSessao() {
        utilizadorId = 0;
        nome = null;
        email = null;
        tipo = null;
        isSuperAdmin = false;
        saldoCarteira = null;
        utilizador = null;
    }
}
