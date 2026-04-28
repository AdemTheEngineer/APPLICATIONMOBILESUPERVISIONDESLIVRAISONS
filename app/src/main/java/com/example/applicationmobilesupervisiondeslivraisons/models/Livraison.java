package com.example.applicationmobilesupervisiondeslivraisons.models;

public class Livraison {

    private String id; // Supabase row ID (set manually after fetch)

    private String idCommande;    // nocde
    private String clientNom;     // nomclient
    private String clientTelephone; // telclient
    private String clientVille;   // villeclient
    private String clientAdresse; // adrclient
    private double montant;       // montant
    private String dateLivraison; // dateliv
    private String livreurNom;    // livreur (UID or name)
    private String etat;          // etatliv
    private String remarques;     // remarque
    private int    ordrePassage;  // ordrepassage
    private int    nombreArticles;// nbarticles
    private String modePaiement;  // modepaiement

    // Required empty constructor
    public Livraison() {}

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getId()               { return id; }
    public String getIdCommande()       { return idCommande; }
    public String getClientNom()        { return clientNom; }
    public String getClientTelephone()  { return clientTelephone; }
    public String getClientVille()      { return clientVille; }
    public String getClientAdresse()    { return clientAdresse; }
    public double getMontant()          { return montant; }
    public String getDateLivraison()    { return dateLivraison; }
    public String getLivreurNom()       { return livreurNom; }
    public String getEtat()             { return etat; }
    public String getRemarques()        { return remarques; }
    public int    getOrdrePassage()     { return ordrePassage; }
    public int    getNombreArticles()   { return nombreArticles; }
    public String getModePaiement()     { return modePaiement; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setId(String id)                       { this.id = id; }
    public void setIdCommande(String idCommande)       { this.idCommande = idCommande; }
    public void setClientNom(String clientNom)         { this.clientNom = clientNom; }
    public void setClientTelephone(String t)           { this.clientTelephone = t; }
    public void setClientVille(String clientVille)     { this.clientVille = clientVille; }
    public void setClientAdresse(String clientAdresse) { this.clientAdresse = clientAdresse; }
    public void setMontant(double montant)             { this.montant = montant; }
    public void setDateLivraison(String dateLivraison) { this.dateLivraison = dateLivraison; }
    public void setLivreurNom(String livreurNom)       { this.livreurNom = livreurNom; }
    public void setEtat(String etat)                   { this.etat = etat; }
    public void setRemarques(String remarques)         { this.remarques = remarques; }
    public void setOrdrePassage(int ordrePassage)      { this.ordrePassage = ordrePassage; }
    public void setNombreArticles(int nombreArticles)  { this.nombreArticles = nombreArticles; }
    public void setModePaiement(String modePaiement)   { this.modePaiement = modePaiement; }
}