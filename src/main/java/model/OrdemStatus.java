package model;

public class OrdemStatus {
    private Integer idStatus;
    private String status; // ativa | executada | expirada
    public OrdemStatus() {}
    public Integer getIdStatus() { return idStatus; }
    public void setIdStatus(Integer i) { this.idStatus = i; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
}
