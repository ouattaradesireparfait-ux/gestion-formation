package com.formation.model;

import com.formation.enums.StatutInscription;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateInscription;

    @Enumerated(EnumType.STRING)
    private StatutInscription statut = StatutInscription.CONFIRMEE;

    private String appreciation;
    private String motifEmpechement;
    private LocalDate dateEmpechement;

    // Chemin du document de fin de formation transmis par l'employé
    private String documentFinFormation;
    private String nomDocumentFinFormation;
    private LocalDate dateTransmissionDocument;
    private boolean documentLu = false;

    // ManyToOne pour permettre plusieurs inscriptions par demande (après empêchement)
    @ManyToOne
    @JoinColumn(name = "demande_id")
    private DemandeFormation demande;

    // Pour les inscriptions directes (sans demande), on lie directement l'employé
    @ManyToOne
    @JoinColumn(name = "employe_id")
    private Employe employe;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;

    // Retourne l'employé quelle que soit la source (demande ou inscription directe)
    public Employe getEmployeEffectif() {
        if (demande != null) return demande.getEmploye();
        return employe;
    }

    public void signalerEmpechement(String motif) {
        this.statut = StatutInscription.EMPECHEMENT;
        this.motifEmpechement = motif;
        this.dateEmpechement = LocalDate.now();
        if (session != null) session.libererPlace();
    }

    public void annuler() {
        this.statut = StatutInscription.ANNULEE;
        if (session != null) session.libererPlace();
    }
}
