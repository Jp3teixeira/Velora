package model;

public class Perfil {
    private Integer idPerfil;
    private String perfil; // user | admin

    public Perfil() {}
    public Integer getIdPerfil() { return idPerfil; }
    public void setIdPerfil(Integer idPerfil) { this.idPerfil = idPerfil; }
    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
}
