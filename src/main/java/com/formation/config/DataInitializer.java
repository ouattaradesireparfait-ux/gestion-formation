package com.formation.config;

import com.formation.model.*;
import com.formation.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UtilisateurRepository utilisateurRepo;
    @Autowired private EmployeRepository employeRepo;
    @Autowired private ResponsableRepository responsableRepo;
    @Autowired private FormationRepository formationRepo;
    @Autowired private SessionRepository sessionRepo;
    @Autowired private PasswordEncoder encoder;
    @Autowired private DataSource dataSource;

    @Override
    public void run(String... args) {
        System.out.println("===== INITIALISATION DES DONNEES =====");

        // Supprimer la contrainte UNIQUE sur demande_id si elle existe
        // (nécessaire pour permettre plusieurs inscriptions par demande après empêchement)
        try (Connection conn = dataSource.getConnection()) {
            // H2 : chercher via information_schema
            try (var stmt = conn.createStatement();
                 var result = stmt.executeQuery(
                     "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                     "WHERE TABLE_NAME = 'INSCRIPTION' AND CONSTRAINT_TYPE = 'UNIQUE' " +
                     "AND CONSTRAINT_NAME LIKE '%DEMANDE%'")) {
                while (result.next()) {
                    String constraintName = result.getString("CONSTRAINT_NAME");
                    conn.createStatement().execute(
                        "ALTER TABLE INSCRIPTION DROP CONSTRAINT IF EXISTS \"" + constraintName + "\"");
                    System.out.println("✅ Contrainte supprimée : " + constraintName);
                }
            } catch (Exception ignored) {
                // Contrainte déjà absente ou base PostgreSQL — ignoré
            }
        } catch (Exception e) {
            System.out.println("Info: suppression contrainte ignorée — " + e.getMessage());
        }

        // Responsable
        if (!utilisateurRepo.existsByEmail("broupatrice@gmail.com")) {
            ResponsableFormation r = new ResponsableFormation();
            r.setNom("Brou"); r.setPrenom("Patrice");
            r.setEmail("broupatrice@gmail.com");
            r.setMotDePasse(encoder.encode("AdminDemo"));
            r.setTelephone("+225 07 00 00 01");
            r.setAdresse("Abidjan, Plateau");
            r.setMatricule("RESP001");
            r.setDateNomination(LocalDate.of(2023, 1, 15));
            r.setDecisionNomination("Arrêté n°001/2023");
            r.setActif(true); r.setDateCreation(LocalDateTime.now());
            responsableRepo.save(r);
            System.out.println("✅ Responsable créé");
        }

        // Employé 1
        if (!utilisateurRepo.existsByEmail("ouattaradesireparfait@gmail.com")) {
            Employe e = new Employe();
            e.setNom("Ouattara"); e.setPrenom("Désiré Parfait");
            e.setEmail("ouattaradesireparfait@gmail.com");
            e.setMotDePasse(encoder.encode("EmployeDemo"));
            e.setTelephone("+225 01 71 27 53 81");
            e.setAdresse("Cocody, Abidjan");
            e.setMatricule("EMP001"); e.setGrade("Senior");
            e.setSalaire(700000.0); e.setDepartement("Informatique");
            e.setPoste("Développeur Full Stack");
            e.setNombreFormationsSuivies(0);
            e.setActif(true); e.setDateCreation(LocalDateTime.now());
            employeRepo.save(e);
            System.out.println("✅ Employé 1 créé");
        }

        // Employé 2
        if (!utilisateurRepo.existsByEmail("guedeangeevelyne@gmail.com")) {
            Employe e = new Employe();
            e.setNom("Guede"); e.setPrenom("Ange-Evelyne");
            e.setEmail("guedeangeevelyne@gmail.com");
            e.setMotDePasse(encoder.encode("EmployeDemo"));
            e.setTelephone("+225 01 53 05 66 22");
            e.setAdresse("Cocody, Abidjan");
            e.setMatricule("EMP002"); e.setGrade("Junior");
            e.setSalaire(650000.0); e.setDepartement("Informatique");
            e.setPoste("Ingénieure Réseau");
            e.setNombreFormationsSuivies(0);
            e.setActif(true); e.setDateCreation(LocalDateTime.now());
            employeRepo.save(e);
            System.out.println("✅ Employé 2 créé");
        }

        // Employé 3
        if (!utilisateurRepo.existsByEmail("horodesire@gmail.com")) {
            Employe e = new Employe();
            e.setNom("Horo"); e.setPrenom("Désiré");
            e.setEmail("horodesire@gmail.com");
            e.setMotDePasse(encoder.encode("EmployeDemo"));
            e.setTelephone("+225 07 68 99 18 64");
            e.setAdresse("Cocody, Abidjan");
            e.setMatricule("EMP003"); e.setGrade("Confirmé");
            e.setSalaire(650000.0); e.setDepartement("Informatique");
            e.setPoste("Ingénieur Réseau");
            e.setNombreFormationsSuivies(0);
            e.setActif(true); e.setDateCreation(LocalDateTime.now());
            employeRepo.save(e);
            System.out.println("✅ Employé 3 créé");
        }

        // Formations
        if (formationRepo.count() == 0) {
            Formation f1 = new Formation();
            f1.setCodeFormation("F-001");
            f1.setNomFormation("Développement Web Full Stack");
            f1.setDescription("Formation complète sur HTML, CSS, JavaScript, React et Spring Boot");
            f1.setDureeHeures(120);
            f1.setObjectifs("Maîtriser les technologies front-end et back-end");
            f1.setPrerequisites("Bases de la programmation");
            formationRepo.save(f1);

            Formation f2 = new Formation();
            f2.setCodeFormation("F-002");
            f2.setNomFormation("Gestion de Projet Agile (Scrum)");
            f2.setDescription("Formation sur les méthodologies agiles et Scrum");
            f2.setDureeHeures(35);
            f2.setObjectifs("Obtenir la certification Scrum Master");
            f2.setPrerequisites("Expérience en gestion de projet");
            formationRepo.save(f2);

            Formation f3 = new Formation();
            f3.setCodeFormation("F-003");
            f3.setNomFormation("Anglais Professionnel");
            f3.setDescription("Perfectionnement en anglais des affaires");
            f3.setDureeHeures(60);
            f3.setObjectifs("Atteindre le niveau B2 en anglais");
            f3.setPrerequisites("Niveau A2 minimum");
            formationRepo.save(f3);

            System.out.println("✅ 3 formations créées");
        }

        // Sessions exemple
        /*
        if (sessionRepo.count() == 0) {
            formationRepo.findByCodeFormation("F-001").ifPresent(f -> {
                Session s = new Session();
                s.setFormation(f);
                s.setDateDebut(LocalDate.now().plusDays(10));
                s.setDateFin(LocalDate.now().plusDays(50));
                s.setLieu("Cocody, Abidjan"); s.setPlacesMax(15);
                s.setPlacesDisponibles(15); s.setStatut(StatutInscription.CONFIRMEE);
                sessionRepo.save(s);
            });
            formationRepo.findByCodeFormation("F-002").ifPresent(f -> {
                Session s = new Session();
                s.setFormation(f);
                s.setDateDebut(LocalDate.now().plusDays(5));
                s.setDateFin(LocalDate.now().plusDays(12));
                s.setLieu("En ligne (Zoom)"); s.setPlacesMax(20);
                s.setPlacesDisponibles(20); s.setStatut(StatutInscription.CONFIRMEE);
                sessionRepo.save(s);
            });
            formationRepo.findByCodeFormation("F-003").ifPresent(f -> {
                Session s = new Session();
                s.setFormation(f);
                s.setDateDebut(LocalDate.now().plusDays(30));
                s.setDateFin(LocalDate.now().plusDays(60));
                s.setLieu("Plateau, Abidjan"); s.setPlacesMax(12);
                s.setPlacesDisponibles(12); s.setStatut(StatutInscription.CONFIRMEE);
                sessionRepo.save(s);
            });
            System.out.println("✅ Sessions exemple créées");
        }
        */
        System.out.println("\n========================================");
        System.out.println("\n          SERVEUR DEMARRE             \n");
        System.out.println("========================================\n");
    }
}
