package com.formation.dto;

public class PasswordChangeRequest {
    private String ancienMotDePasse;
    private String nouveauMotDePasse;
    private String confirmationMotDePasse;

    public String getAncienMotDePasse() { return ancienMotDePasse; }
    public void setAncienMotDePasse(String v) { this.ancienMotDePasse = v; }
    public String getNouveauMotDePasse() { return nouveauMotDePasse; }
    public void setNouveauMotDePasse(String v) { this.nouveauMotDePasse = v; }
    public String getConfirmationMotDePasse() { return confirmationMotDePasse; }
    public void setConfirmationMotDePasse(String v) { this.confirmationMotDePasse = v; }

    public boolean isMatching() {
        return nouveauMotDePasse != null && nouveauMotDePasse.equals(confirmationMotDePasse);
    }
}
