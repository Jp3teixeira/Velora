
import model.Moeda;
import model.Carteira;
import model.Utilizador;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

class ProjetoTestes {

    // ====== TESTES À CLASSE MOEDA ======
    @Test
    void moeda_GuardaCamposCorretamente() {
        BigDecimal valor   = new BigDecimal("25000.00");
        BigDecimal var24   = new BigDecimal("5.0");
        BigDecimal volume  = new BigDecimal("1000000");

        Moeda btc = new Moeda(1, "Bitcoin", "BTC", valor, var24, volume);

        assertAll("Campos de Moeda",
            () -> assertEquals("Bitcoin", btc.getNome()),
            () -> assertEquals("BTC",     btc.getSimbolo()),
            () -> assertEquals(0, btc.getValorAtual().compareTo(valor))
        );
    }

    @Test
    void moeda_AtualizaValor() {
        Moeda eth = new Moeda();
        eth.setValorAtual(new BigDecimal("2100.00"));
        assertEquals(0, eth.getValorAtual().compareTo(new BigDecimal("2100.00")));
    }

    // ====== TESTES À CLASSE CARTEIRA ======
    @Test
    void carteira_AssociaUtilizadorESaldo() {
        Carteira carteira = new Carteira();
        Utilizador user   = new Utilizador();
        user.setId(42);
        user.setNome("Rodrigo");

        carteira.setUtilizador(user);
        carteira.setSaldoEur(new BigDecimal("1500.00"));

        assertAll("Carteira",
            () -> assertEquals(user, carteira.getUtilizador()),
            () -> assertEquals(0, carteira.getSaldoEur().compareTo(new BigDecimal("1500.00")))
        );
    }

    // ====== TESTES À CLASSE UTILIZADOR ======
    @Test
    void utilizador_CamposBasicos() {
        Utilizador u = new Utilizador();
        u.setNome("Ana");
        u.setEmail("ana@mail.com");
        u.setDataCriacao(LocalDateTime.now());
        u.setAtivo(true);

        assertAll("Utilizador",
            () -> assertEquals("Ana", u.getNome()),
            () -> assertEquals("ana@mail.com", u.getEmail()),
            () -> assertTrue(u.isAtivo())
        );
    }
}
