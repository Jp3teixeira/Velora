package model;

import java.math.BigDecimal;

public class Carteira {
    private Integer idCarteira;
    private Utilizador utilizador;
    private BigDecimal saldoEur;
    // getters/setters...

    public Carteira() {}
    public Integer getIdCarteira() { return idCarteira; }
    public void setIdCarteira(Integer id) { this.idCarteira = id; }
    public Utilizador getUtilizador() { return utilizador; }
    public void setUtilizador(Utilizador u) { this.utilizador = u; }
    public BigDecimal getSaldoEur() { return saldoEur; }
    public void setSaldoEur(BigDecimal s) { this.saldoEur = s; }

}
