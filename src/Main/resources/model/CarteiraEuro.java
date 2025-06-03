package model;

import java.math.BigDecimal;

public class CarteiraEuro {
    private Integer idCarteiraEuro;
    private Utilizador utilizador;
    private BigDecimal saldoEur;

    // ====== Construtores, getters e setters ======
    public CarteiraEuro() { }

    public Integer getIdCarteiraEuro() {
        return idCarteiraEuro;
    }
    public void setIdCarteiraEuro(Integer idCarteiraEuro) {
        this.idCarteiraEuro = idCarteiraEuro;
    }

    public Utilizador getUtilizador() {
        return utilizador;
    }
    public void setUtilizador(Utilizador utilizador) {
        this.utilizador = utilizador;
    }

    public BigDecimal getSaldoEur() {
        return saldoEur;
    }
    public void setSaldoEur(BigDecimal saldoEur) {
        this.saldoEur = saldoEur;
    }
}
