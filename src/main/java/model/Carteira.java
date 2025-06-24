package model;

import java.math.BigDecimal;

public class Carteira implements Identifiable<Integer> {
    private Integer idCarteira;
    private Utilizador utilizador;
    private BigDecimal saldoEur;

    public Carteira() {}

    @Override
    public Integer getId() {
        return idCarteira;
    }

    @Override
    public void setId(Integer id) {
        this.idCarteira = id;
    }

    // Getters & Setters
    public Utilizador getUtilizador() { return utilizador; }
    public void setUtilizador(Utilizador utilizador) { this.utilizador = utilizador; }

    public BigDecimal getSaldoEur() { return saldoEur; }
    public void setSaldoEur(BigDecimal saldoEur) { this.saldoEur = saldoEur; }
}