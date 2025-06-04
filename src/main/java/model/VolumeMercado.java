package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VolumeMercado {
    private Integer idVolume;           // PK, mapeia VolumeMercado.id_volume
    private Moeda moeda;                // FK → Moeda
    private BigDecimal volume;          // mapeia VolumeMercado.volume
    private LocalDateTime timestampHora;// mapeia VolumeMercado.timestamp_hora

    // ====== Construtores, getters e setters ======
    public VolumeMercado() { }

    public Integer getIdVolume() {
        return idVolume;
    }
    public void setIdVolume(Integer idVolume) {
        this.idVolume = idVolume;
    }

    public Moeda getMoeda() {
        return moeda;
    }
    public void setMoeda(Moeda moeda) {
        this.moeda = moeda;
    }

    public BigDecimal getVolume() {
        return volume;
    }
    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public LocalDateTime getTimestampHora() {
        return timestampHora;
    }
    public void setTimestampHora(LocalDateTime timestampHora) {
        this.timestampHora = timestampHora;
    }
}
