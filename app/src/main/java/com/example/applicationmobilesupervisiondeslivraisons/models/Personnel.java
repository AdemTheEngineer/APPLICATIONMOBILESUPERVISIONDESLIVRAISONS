package com.example.applicationmobilesupervisiondeslivraisons.models;

public class Personnel {

    private String id;
    private String nompers;
    private String prenompers;
    private String adrpers;
    private String villepers;
    private String telpers;
    private String d_embauche;
    private String login;
    private String motP;
    private String codeposte;
    private String role;

    public Personnel() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNompers() { return nompers; }
    public void setNompers(String nompers) { this.nompers = nompers; }

    public String getPrenompers() { return prenompers; }
    public void setPrenompers(String prenompers) { this.prenompers = prenompers; }

    public String getAdrpers() { return adrpers; }
    public void setAdrpers(String adrpers) { this.adrpers = adrpers; }

    public String getVillepers() { return villepers; }
    public void setVillepers(String villepers) { this.villepers = villepers; }

    public String getTelpers() { return telpers; }
    public void setTelpers(String telpers) { this.telpers = telpers; }

    public String getD_embauche() { return d_embauche; }
    public void setD_embauche(String d_embauche) { this.d_embauche = d_embauche; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getMotP() { return motP; }
    public void setMotP(String motP) { this.motP = motP; }

    public String getCodeposte() { return codeposte; }
    public void setCodeposte(String codeposte) { this.codeposte = codeposte; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}