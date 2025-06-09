package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PrecoMoeda {
    private Integer idPreco;
    private Moeda moeda;
    private BigDecimal precoEmEur;
    private LocalDateTime timestampHora;

    public PrecoMoeda() {}
    // getters & setters...
    public Integer getIdPreco() { return idPreco; }
    public void setIdPreco(Integer i) { this.idPreco = i; }
    public Moeda getMoeda() { return moeda; }
    public void setMoeda(Moeda m) { this.moeda = m; }
    public BigDecimal getPrecoEmEur() { return precoEmEur; }
    public void setPrecoEmEur(BigDecimal p) { this.precoEmEur = p; }
    public LocalDateTime getTimestampHora() { return timestampHora; }
    public void setTimestampHora(LocalDateTime t) { this.timestampHora = t; }
}
