package com.transportpro;

import com.transportpro.incident.Incident;
import com.transportpro.incident.IncidentDAO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Module Incidents (BF20 à BF22) : listes ENUM, ajout, lecture, modification, recherche,
 * validation et suppression. MODIFIE LA BASE : à lancer sur une copie (-Demplogo.db.url=...emplogo_test).
 */
public class IncidentTest {

    static int pass = 0, fail = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (ok) pass++; else fail++;
    }

    public static void main(String[] args) throws Exception {
        if (!System.getProperty("emplogo.db.url", "").contains("_test")) {
            System.out.println("Refusé : lancez ce test sur une base *_test (-Demplogo.db.url=...).");
            return;
        }
        IncidentDAO dao = new IncidentDAO();

        int before = dao.findAll().size();
        check("liste : " + before + " incident(s) lus avec jointures", before > 0
                && dao.findAll().stream().allMatch(i -> !i.getImmatriculation().isEmpty() && !i.getChauffeurNom().isEmpty()));
        check("ENUM types = " + dao.findTypes(), dao.findTypes().containsAll(List.of("PANNE", "RETARD", "AUTRE")));
        check("ENUM gravités = " + dao.findGravites(), dao.findGravites().contains("FAIBLE"));
        check("ENUM statuts = " + dao.findStatuts(), dao.findStatuts().containsAll(List.of("OUVERT", "CLOS")));
        check("ENUM modes = " + dao.findModes(), dao.findModes().contains("APPLICATION"));

        List<IncidentDAO.Option> trajets = dao.findAffectations(0);
        check("trajets proposés : " + trajets.size(), !trajets.isEmpty());

        Incident i = new Incident();
        i.setIdAffectation(trajets.get(0).id());
        i.setTypeIncident("PANNE");
        i.setGravite("MOYENNE");
        i.setDateIncident(LocalDateTime.now().minusHours(1).withSecond(0).withNano(0));
        i.setModeDeclaration("APPLICATION");
        i.setStatutTraitement("OUVERT");
        i.setDescription("Test automatique : crevaison pneu avant zyxw");
        dao.insert(i);
        check("ajout : INC" + i.getIdIncident(), i.getIdIncident() > 0 && dao.findAll().size() == before + 1);

        Incident r = dao.findById(i.getIdIncident());
        check("lecture : valeurs relues", "PANNE".equals(r.getTypeIncident()) && r.getIdCompte() == null
                && r.getDescription().contains("crevaison"));

        r.setStatutTraitement("RESOLU");
        r.setGravite("ELEVEE");
        dao.update(r);
        Incident r2 = dao.findById(i.getIdIncident());
        check("modification : statut et gravité", "RESOLU".equals(r2.getStatutTraitement()) && "ELEVEE".equals(r2.getGravite()));

        check("recherche par description", dao.search("zyxw").size() == 1);
        check("recherche par numéro INC", dao.search("INC" + i.getIdIncident()).stream()
                .anyMatch(x -> x.getIdIncident() == i.getIdIncident()));
        check("recherche par plaque de bus", dao.search(r.getImmatriculation()).stream()
                .anyMatch(x -> x.getIdIncident() == i.getIdIncident()));

        Incident bad = new Incident();
        bad.setIdAffectation(trajets.get(0).id());
        bad.setTypeIncident("RETARD");
        bad.setGravite("FAIBLE");
        bad.setModeDeclaration("APPLICATION");
        bad.setStatutTraitement("OUVERT");
        bad.setDateIncident(LocalDateTime.now().plusDays(2));
        bad.setDescription("futur");
        boolean refused = false;
        try { dao.insert(bad); } catch (IncidentDAO.IncidentException e) { refused = true; }
        check("validation : date future refusée", refused);
        bad.setDateIncident(LocalDateTime.now().minusMinutes(10));
        bad.setDescription("  ");
        refused = false;
        try { dao.insert(bad); } catch (IncidentDAO.IncidentException e) { refused = true; }
        check("validation : description vide refusée", refused);

        dao.delete(i.getIdIncident());
        check("suppression", dao.findAll().size() == before);

        System.out.println("\nPASS=" + pass + " FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
