package com.transportpro.integration;

import com.transportpro.dao.IncidentDAO;
import com.transportpro.dao.Option;
import com.transportpro.model.Incident;
import com.transportpro.model.enums.TypeIncident;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vérification manuelle du module Incidents (BF20 à BF22).
 * MODIFIE LA BASE : à lancer sur une copie (-Demplogo.db.url=jdbc:mysql://localhost:3306/employgo_test).
 */
public class IncidentCheck {

    static int pass = 0, fail = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (ok) pass++; else fail++;
    }

    public static void main(String[] args) throws Exception {
        Guard.requireTestDatabase();
        IncidentDAO dao = new IncidentDAO();

        int before = dao.findAll().size();
        check("liste : " + before + " incident(s) lus avec jointures", before > 0
                && dao.findAll().stream().allMatch(i -> !i.getImmatriculation().isEmpty() && !i.getChauffeurNom().isEmpty()));
        check("tous les types de la base existent dans l'enum TypeIncident",
                dao.findEnum("type").stream().allMatch(t -> { try { TypeIncident.valueOf(t); return true; }
                                                              catch (IllegalArgumentException e) { return false; } }));
        check("gravités = " + dao.findGravites(), dao.findGravites().contains("MINEURE"));
        check("statuts = " + dao.findStatuts(), dao.findStatuts().containsAll(List.of("OUVERT", "CLOTURE")));
        check("modes = " + dao.findModes(), dao.findModes().contains("SIGNALEMENT_CHAUFFEUR"));

        List<Option> trajets = dao.findAffectations(0);
        check("trajets proposés : " + trajets.size(), !trajets.isEmpty());

        Incident i = new Incident(0, TypeIncident.PANNE, "Test automatique : crevaison pneu avant zyxw",
                LocalDateTime.now().minusHours(1).withSecond(0).withNano(0));
        i.setIdAffectation(trajets.get(0).id());
        i.setGravite("MODEREE");
        i.setModeDeclaration("APPLICATION");
        dao.insert(i);   // creer() met le statut OUVERT par défaut
        check("ajout : INC" + i.getIdIncident() + " (statut OUVERT par défaut)",
                i.getIdIncident() > 0 && dao.findAll().size() == before + 1 && "OUVERT".equals(i.getStatut()));

        Incident r = dao.findById(i.getIdIncident());
        check("lecture : valeurs relues", r.getType() == TypeIncident.PANNE && r.getIdCompte() == null
                && r.getDescription().contains("crevaison"));

        r.setStatut("RESOLU");
        r.setGravite("GRAVE");
        dao.update(r);
        Incident r2 = dao.findById(i.getIdIncident());
        check("modification : statut et gravité", "RESOLU".equals(r2.getStatut()) && "GRAVE".equals(r2.getGravite()));

        check("recherche par description", dao.search("zyxw").size() == 1);
        check("recherche par numéro INC", dao.search("INC" + i.getIdIncident()).stream()
                .anyMatch(x -> x.getIdIncident() == i.getIdIncident()));
        check("recherche par plaque de bus", dao.search(r.getImmatriculation()).stream()
                .anyMatch(x -> x.getIdIncident() == i.getIdIncident()));

        Incident bad = new Incident(0, TypeIncident.RETARD, "futur", LocalDateTime.now().plusDays(2));
        bad.setIdAffectation(trajets.get(0).id());
        bad.setGravite("MINEURE");
        bad.setModeDeclaration("APPLICATION");
        boolean refused = false;
        try { dao.insert(bad); } catch (Exception e) { refused = true; }
        check("validation : date future refusée", refused);
        bad.setDateHeure(LocalDateTime.now().minusMinutes(10));
        bad.setDescription("  ");
        refused = false;
        try { dao.insert(bad); } catch (Exception e) { refused = true; }
        check("validation : description vide refusée", refused);

        dao.delete(i.getIdIncident());
        check("suppression", dao.findAll().size() == before);

        System.out.println("\nPASS=" + pass + " FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
