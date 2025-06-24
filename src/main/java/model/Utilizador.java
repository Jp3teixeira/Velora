// src/model/Utilizador.java
package model;

import java.time.LocalDateTime;

public class Utilizador implements Identifiable<Integer> {
    private Integer        idUtilizador;
    private String         email;
    private String         nome;
    private String         password;
    private String         foto;
    private LocalDateTime  dataCriacao;
    private Integer        idPerfil;
    private Perfil         perfil;
    private boolean        ativo;
    private String         hashPwd;

    public Utilizador() {}

    @Override
    public Integer getId() {
        return idUtilizador;
    }

    @Override
    public void setId(Integer id) {
        this.idUtilizador = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public Integer getIdPerfil() {
        return idPerfil;
    }

    public void setIdPerfil(Integer idPerfil) {
        this.idPerfil = idPerfil;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getHashPwd() {
        return hashPwd;
    }

    public void setHashPwd(String hashPwd) {
        this.hashPwd = hashPwd;
    }
}
