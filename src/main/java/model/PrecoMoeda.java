package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PrecoMoeda {
    private Integer idPreco;
    private Moeda moeda;
    private BigDecimal precoEmEur;
    private LocalDateTime timestampHora;

    // ====== Construtores, getters e setters ======
    public PrecoMoeda() { }

    public Integer getIdPreco() {
        return idPreco;
    }
    public void setIdPreco(Integer idPreco) {
        this.idPreco = idPreco;
    }

    public Moeda getMoeda() {
        return moeda;
    }
    public void setMoeda(Moeda moeda) {
        this.moeda = moeda;
    }

    public BigDecimal getPrecoEmEur() {
        return precoEmEur;
    }
    public void setPrecoEmEur(BigDecimal precoEmEur) {
        this.precoEmEur = precoEmEur;
    }

    public LocalDateTime getTimestampHora() {
        return timestampHora;
    }
    public void setTimestampHora(LocalDateTime timestampHora) {
        this.timestampHora = timestampHora;
    }
}
