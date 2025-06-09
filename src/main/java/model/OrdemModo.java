package model;

public class OrdemModo {
    private Integer idModo;
    private String modo; // market | limit
    public OrdemModo() {}
    public Integer getIdModo() { return idModo; }
    public void setIdModo(Integer i) { this.idModo = i; }
    public String getModo() { return modo; }
    public void setModo(String m) { this.modo = m; }
}
