package com.formation.enums;

public enum StatutInscription {
    EN_ATTENTE("En attente"),
    CONFIRMEE("Confirmée"),
    ANNULEE("Annulée"),
    EMPECHEMENT("Empêchement signalé"),
    TERMINEE("Terminée");

    private final String libelle;
    StatutInscription(String libelle) { this.libelle = libelle; }
    public String getLibelle() { return libelle; }
}
