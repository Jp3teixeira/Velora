package model;


import java.time.LocalDateTime;


public class Utilizador {
    private Integer idUtilizador;
    private String email, nome, password, foto;
    private LocalDateTime dataCriacao;
    private Integer idPerfil;
    private String perfil;
    private boolean ativo;
    private String hashPwd;



    public Utilizador() {}

    // getters & setters...
    public Integer getIdUtilizador() { return idUtilizador; }
    public void setIdUtilizador(Integer idUtilizador) { this.idUtilizador = idUtilizador; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getIdPerfil() { return idPerfil; }
    public void setIdPerfil(Integer idPerfil) { this.idPerfil = idPerfil; }
    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getHashPwd() {
        return hashPwd;
    }

    public void setHashPwd(String hashPwd) {
        this.hashPwd = hashPwd;
    }







}
