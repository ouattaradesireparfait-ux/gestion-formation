package com.formation.enums;

public enum StatutDemande {
    EN_ATTENTE("En attente"),
    EN_INSTRUCTION("En instruction"),
    ACCEPTEE("Acceptée"),
    REFUSEE("Refusée"),
    ANNULEE("Annulée"),
    VALIDEE("Validée");

    private final String libelle;
    StatutDemande(String libelle) { this.libelle = libelle; }
    public String getLibelle() { return libelle; }
}
