package com.emplogo.controller;

import java.sql.SQLException;
import java.util.List;

import com.emplogo.Ctx;
import com.emplogo.dao.AffectationDAO;
import com.emplogo.dao.BusDAO;
import com.emplogo.dao.ChauffeurDAO;
import com.emplogo.dao.EmployeDAO;
import com.emplogo.dao.IncidentDAO;
import com.emplogo.dao.InscriptionDAO;
import com.emplogo.dao.Option;
import com.emplogo.dao.TrajetDAO;
import com.emplogo.model.Affectation;
import com.emplogo.model.Bus;
import com.emplogo.model.Chauffeur;
import com.emplogo.model.Employe;
import com.emplogo.model.Incident;
import com.emplogo.model.Inscription;
import com.emplogo.model.Trajet;
import com.emplogo.model.enums.StatutPresence;

/**
 * Controller applicatif principal.
 * Fait le lien entre les pages (View) et les DAO : les pages n'appellent jamais un DAO directement.
 * Toute exception SQL est transformée en message lisible (ctx.toast en cas de succès, ctx.error en cas d'échec).
 * Les méthodes d'action retournent true si l'opération a réussi (la page peut alors se rafraîchir).
 */
public class MainController {

    private final Ctx ctx;
    private final EmployeDAO employeDAO;
    private final BusDAO busDAO;
    private final ChauffeurDAO chauffeurDAO;
    private final TrajetDAO trajetDAO;
    private final AffectationDAO affectationDAO;
    private final InscriptionDAO inscriptionDAO;
    private final IncidentDAO incidentDAO;

    // Constructor used by the real application
    public MainController(Ctx ctx) {
        this(ctx,
             new EmployeDAO(),
             new BusDAO(),
             new ChauffeurDAO(),
             new TrajetDAO());
    }

    // Constructor used for testing
    public MainController(
            Ctx ctx,
            EmployeDAO employeDAO,
            BusDAO busDAO,
            ChauffeurDAO chauffeurDAO,
            TrajetDAO trajetDAO) {
        this(ctx, employeDAO, busDAO, chauffeurDAO, trajetDAO,
             new AffectationDAO(), new InscriptionDAO(), new IncidentDAO());
    }

    public MainController(
            Ctx ctx,
            EmployeDAO employeDAO,
            BusDAO busDAO,
            ChauffeurDAO chauffeurDAO,
            TrajetDAO trajetDAO,
            AffectationDAO affectationDAO,
            InscriptionDAO inscriptionDAO,
            IncidentDAO incidentDAO) {
        this.ctx = ctx;
        this.employeDAO = employeDAO;
        this.busDAO = busDAO;
        this.chauffeurDAO = chauffeurDAO;
        this.trajetDAO = trajetDAO;
        this.affectationDAO = affectationDAO;
        this.inscriptionDAO = inscriptionDAO;
        this.incidentDAO = incidentDAO;
    }

    // ---------------------------------------------------------------- outils communs

    @FunctionalInterface
    private interface Query<T> { T run() throws SQLException; }

    @FunctionalInterface
    private interface Action { void run() throws SQLException; }

    /** Lecture : en cas d'erreur, message « prefix : détail » et valeur par défaut. */
    private <T> T read(Query<T> q, String prefix, T fallback) {
        try {
            return q.run();
        } catch (SQLException e) {
            ctx.error(prefix + " : " + e.getMessage());
            return fallback;
        }
    }

    /** Action : message de succès, ou « Échec ... : détail ». */
    private boolean act(Action a, String success, String failurePrefix) {
        try {
            a.run();
            if (success != null) ctx.toast(success);
            return true;
        } catch (SQLException e) {
            ctx.error(failurePrefix + " : " + e.getMessage());
            return false;
        }
    }

    // ---------------- Employés ----------------

    public List<Employe> chargerEmployes() {
        try {
            return employeDAO.findAll();
        } catch (SQLException e) {
            ctx.error("Erreur employés : " + e.getMessage());
            return List.of();
        }
    }

    public List<Employe> rechercherEmployes(String texte) {
        return read(() -> employeDAO.search(texte), "Erreur employés", List.of());
    }

    public Employe trouverEmploye(int id) {
        Employe e = read(() -> employeDAO.findById(id), "Erreur employés", null);
        if (e == null) ctx.error("Cet employé n'existe plus dans la base.");
        return e;
    }

    public boolean ajouterEmploye(Employe e) {
        try {
            employeDAO.insert(e);
            ctx.toast("Employé ajouté : " + e.getNomComplet());
            return true;
        } catch (SQLException ex) {
            ctx.error("Échec ajout : " + ex.getMessage());
            return false;
        }
    }

    public boolean modifierEmploye(Employe e) {
        try {
            employeDAO.update(e);
            ctx.toast("Employé modifié avec succès");
            return true;
        } catch (SQLException ex) {
            ctx.error("Échec modification : " + ex.getMessage());
            return false;
        }
    }

    public boolean supprimerEmploye(int id) {
        try {
            employeDAO.delete(id);
            ctx.toast("Employé supprimé");
            return true;
        } catch (SQLException ex) {
            ctx.error("Échec suppression : " + ex.getMessage());
            return false;
        }
    }

    public EmployeDAO.Usage usageEmploye(int id) {
        return read(() -> employeDAO.usage(id), "Erreur employés", null);
    }

    public boolean desactiverEmploye(int id) {
        return act(() -> employeDAO.desactiver(id), "Employé désactivé (historique conservé)", "Échec désactivation");
    }

    public boolean supprimerEmployeEtHistorique(int id) {
        return act(() -> employeDAO.deleteWithHistory(id), "Employé supprimé avec son historique", "Échec suppression");
    }

    // ---------------- Bus ----------------

    public List<Bus> chargerBus() {
        try {
            return busDAO.findAll();
        } catch (SQLException e) {
            ctx.error("Erreur bus : " + e.getMessage());
            return List.of();
        }
    }

    public Bus trouverBus(int id) {
        Bus b = read(() -> busDAO.findById(id), "Erreur bus", null);
        if (b == null) ctx.error("Ce bus n'existe plus dans la base.");
        return b;
    }

    public boolean ajouterBus(Bus b) {
        return act(() -> busDAO.insert(b), "Bus " + b.getImmatriculation() + " ajouté", "Échec ajout");
    }

    public boolean modifierBus(Bus b) {
        return act(() -> busDAO.update(b), "Bus " + b.getImmatriculation() + " modifié", "Échec modification");
    }

    public boolean supprimerBus(int id) {
        return act(() -> busDAO.delete(id), "Bus supprimé", "Échec suppression");
    }

    public BusDAO.Usage usageBus(int id) {
        return read(() -> busDAO.usage(id), "Erreur bus", null);
    }

    public boolean mettreBusHorsService(int id) {
        return act(() -> busDAO.mettreHorsService(id), "Bus mis hors service", "Échec");
    }

    public boolean supprimerBusEtHistorique(int id) {
        return act(() -> busDAO.deleteWithHistory(id), "Bus supprimé avec son historique de maintenance",
                "Échec suppression");
    }

    // ---------------- Chauffeurs ----------------

    public List<Chauffeur> chargerChauffeurs() {
        try {
            return chauffeurDAO.findAll();
        } catch (SQLException e) {
            ctx.error("Erreur chauffeurs : " + e.getMessage());
            return List.of();
        }
    }

    public List<Chauffeur> rechercherChauffeurs(String texte) {
        return read(() -> chauffeurDAO.search(texte), "Erreur chauffeurs", List.of());
    }

    public Chauffeur trouverChauffeur(int id) {
        Chauffeur c = read(() -> chauffeurDAO.findById(id), "Erreur chauffeurs", null);
        if (c == null) ctx.error("Ce chauffeur n'existe plus dans la base.");
        return c;
    }

    public boolean ajouterChauffeur(Chauffeur c) {
        return act(() -> chauffeurDAO.insert(c), "Chauffeur ajouté", "Échec ajout");
    }

    public boolean modifierChauffeur(Chauffeur c) {
        return act(() -> chauffeurDAO.update(c), "Chauffeur " + c.getNomComplet() + " modifié", "Échec modification");
    }

    public boolean supprimerChauffeur(int id) {
        return act(() -> chauffeurDAO.delete(id), "Chauffeur supprimé (l'employé est conservé)", "Échec suppression");
    }

    /** Nombre de trajets du chauffeur, ou -1 en cas d'erreur. */
    public int nombreTrajetsChauffeur(int id) {
        return read(() -> chauffeurDAO.countTrajets(id), "Erreur chauffeurs", -1);
    }

    public boolean desactiverChauffeur(int id) {
        return act(() -> chauffeurDAO.desactiver(id), "Chauffeur suspendu (historique conservé)", "Échec");
    }

    /** Employés pouvant devenir chauffeurs (+ celui indiqué, 0 = aucun). */
    public List<Option> employesPourChauffeur(int includeId) {
        return read(() -> chauffeurDAO.findAvailableEmployees(includeId), "Erreur employés", List.of());
    }

    // ---------------- Trajets ----------------

    public List<Trajet> chargerTrajets() {
        try {
            return trajetDAO.findAll();
        } catch (SQLException e) {
            ctx.error("Erreur trajets : " + e.getMessage());
            return List.of();
        }
    }

    public List<Trajet> planningChauffeur(int idChauffeur) {
        return read(() -> trajetDAO.findByChauffeur(idChauffeur), "Erreur planning", List.of());
    }

    // ---------------- Affectations (trajets planifiés) ----------------

    public List<Affectation> rechercherAffectations(String texte) {
        return read(() -> affectationDAO.search(texte), "Erreur trajets", List.of());
    }

    public Affectation trouverAffectation(int id) {
        Affectation a = read(() -> affectationDAO.findById(id), "Erreur trajets", null);
        if (a == null) ctx.error("Ce trajet n'existe plus dans la base.");
        return a;
    }

    public boolean ajouterAffectation(Affectation a) {
        return act(() -> affectationDAO.insert(a), null, "Affectation refusée")
                && toastOk("Trajet T" + a.getIdAffectation() + " créé");
    }

    public boolean modifierAffectation(Affectation a) {
        return act(() -> affectationDAO.update(a), "Trajet T" + a.getIdAffectation() + " modifié",
                "Modification refusée");
    }

    public boolean supprimerAffectation(int id) {
        return act(() -> affectationDAO.delete(id), "Trajet T" + id + " supprimé", "Échec suppression");
    }

    public AffectationDAO.Usage usageAffectation(int id) {
        return read(() -> affectationDAO.usage(id), "Erreur trajets", null);
    }

    public boolean annulerAffectation(int id) {
        return act(() -> affectationDAO.annuler(id), "Trajet T" + id + " annulé", "Échec annulation");
    }

    public boolean supprimerAffectationEtHistorique(int id) {
        return act(() -> affectationDAO.deleteWithHistory(id), "Trajet T" + id + " supprimé avec ses inscriptions",
                "Échec suppression");
    }

    public List<Option> lignes() {
        return read(affectationDAO::findLignes, "Erreur lignes", List.of());
    }

    public List<Option> busDisponibles(int includeId) {
        return read(() -> affectationDAO.findAvailableBuses(includeId), "Erreur bus", List.of());
    }

    public List<Option> chauffeursDisponibles(int includeId) {
        return read(() -> affectationDAO.findAvailableDrivers(includeId), "Erreur chauffeurs", List.of());
    }

    // ---------------- Inscriptions et présences ----------------

    public List<Inscription> rechercherInscriptions(String texte) {
        return read(() -> inscriptionDAO.search(texte), "Erreur inscriptions", List.of());
    }

    public Inscription trouverInscription(int id) {
        Inscription i = read(() -> inscriptionDAO.findById(id), "Erreur inscriptions", null);
        if (i == null) ctx.error("Cette inscription n'existe plus dans la base.");
        return i;
    }

    public boolean ajouterInscription(Inscription i) {
        return act(() -> inscriptionDAO.insert(i), null, "Inscription refusée")
                && toastOk("Inscription I" + i.getIdInscription() + " ajoutée");
    }

    public boolean modifierInscription(Inscription i) {
        return act(() -> inscriptionDAO.update(i), "Inscription I" + i.getIdInscription() + " mise à jour",
                "Modification refusée");
    }

    public boolean supprimerInscription(int id) {
        return act(() -> inscriptionDAO.delete(id), "Inscription I" + id + " supprimée", "Échec suppression");
    }

    /** Nombre de présences de l'inscription, ou -1 en cas d'erreur. */
    public int nombrePresences(int id) {
        return read(() -> inscriptionDAO.countPresences(id), "Erreur présences", -1);
    }

    public boolean annulerInscription(int id) {
        return act(() -> inscriptionDAO.annuler(id), "Inscription I" + id + " annulée", "Échec annulation");
    }

    public boolean supprimerInscriptionEtHistorique(int id) {
        return act(() -> inscriptionDAO.deleteWithHistory(id), "Inscription I" + id + " supprimée", "Échec suppression");
    }

    public boolean enregistrerPresence(Inscription i, StatutPresence statut) {
        boolean ok = act(() -> inscriptionDAO.enregistrerPresence(i.getIdInscription(), statut),
                "Présence enregistrée : " + statut.libelle(), "Échec présence");
        if (ok) i.enregistrerPresence(statut);
        return ok;
    }

    public List<Option> employesActifs(int includeId) {
        return read(() -> inscriptionDAO.findEmployes(includeId), "Erreur employés", List.of());
    }

    public List<Option> trajetsOuverts(int includeId) {
        return read(() -> inscriptionDAO.findAffectations(includeId), "Erreur trajets", List.of());
    }

    // ---------------- Incidents ----------------

    public List<Incident> rechercherIncidents(String texte) {
        return read(() -> incidentDAO.search(texte), "Erreur incidents", List.of());
    }

    public Incident trouverIncident(int id) {
        Incident i = read(() -> incidentDAO.findById(id), "Erreur incidents", null);
        if (i == null) ctx.error("Cet incident n'existe plus dans la base.");
        return i;
    }

    public boolean ajouterIncident(Incident i) {
        return act(() -> incidentDAO.insert(i), null, "Échec enregistrement")
                && toastOk("Incident INC" + i.getIdIncident() + " enregistré");
    }

    public boolean modifierIncident(Incident i) {
        return act(() -> incidentDAO.update(i), "Incident INC" + i.getIdIncident() + " modifié", "Échec modification");
    }

    public boolean supprimerIncident(int id) {
        return act(() -> incidentDAO.delete(id), "Incident INC" + id + " supprimé", "Échec suppression");
    }

    public List<String> gravitesIncident() {
        return read(incidentDAO::findGravites, "Erreur incidents", List.of());
    }

    public List<String> statutsIncident() {
        return read(incidentDAO::findStatuts, "Erreur incidents", List.of());
    }

    public List<String> modesDeclaration() {
        return read(incidentDAO::findModes, "Erreur incidents", List.of());
    }

    public List<Option> trajetsPourIncident(int includeId) {
        return read(() -> incidentDAO.findAffectations(includeId), "Erreur trajets", List.of());
    }

    private boolean toastOk(String message) {
        ctx.toast(message);
        return true;
    }
}
