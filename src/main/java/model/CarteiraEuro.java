package model;

import java.math.BigDecimal;

public class CarteiraEuro {
    private Integer idCarteiraEuro;
    private Utilizador utilizador;
    private BigDecimal saldoEur;

    public CarteiraEuro() {}

    public Integer getIdCarteiraEuro() { return idCarteiraEuro; }
    public void setIdCarteiraEuro(Integer id) { this.idCarteiraEuro = id; }
    public Utilizador getUtilizador() { return utilizador; }
    public void setUtilizador(Utilizador u) { this.utilizador = u; }
    public BigDecimal getSaldoEur() { return saldoEur; }
    public void setSaldoEur(BigDecimal s) { this.saldoEur = s; }
}
