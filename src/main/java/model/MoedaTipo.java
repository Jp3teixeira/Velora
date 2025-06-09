package model;

public class MoedaTipo {
    private Integer idTipo;
    private String tipo;  // fiat | crypto

    public MoedaTipo() {}
    public Integer getIdTipo() { return idTipo; }
    public void setIdTipo(Integer id) { this.idTipo = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String t) { this.tipo = t; }
}
