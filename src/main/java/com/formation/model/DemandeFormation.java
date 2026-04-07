package com.formation.model;

import com.formation.enums.StatutDemande;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class DemandeFormation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String numeroDemande;

    private LocalDate dateDemande;
    private String motivation;

    @Enumerated(EnumType.STRING)
    private StatutDemande statut = StatutDemande.EN_ATTENTE;

    private String commentaireResponsable;

    // Montant saisi par le responsable lors de l'acceptation — utilisé pour générer la facture
    private Double montantFormation;

    @ManyToOne
    @JoinColumn(name = "employe_id")
    private Employe employe;

    @ManyToOne
    @JoinColumn(name = "formation_id")
    private Formation formation;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session sessionChoisie;

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL)
    private java.util.List<Inscription> inscriptions;
}
