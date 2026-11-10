package com.transportpro.model;

import com.transportpro.model.enums.Role;

import java.util.Objects;

/**
 * Classe de base représentant un utilisateur du système.
 * Héritée par Administrateur et ResponsableTransport.
 */
public class Utilisateur {

    private int idUtilisateur;
    private String login;
    private String motDePasseHache;
    private boolean actif;
    private Role role;   // rôle courant (possède)

    public Utilisateur() {
    }

    public Utilisateur(int idUtilisateur, String login, String motDePasseHache, boolean actif, Role role) {
        this.idUtilisateur = idUtilisateur;
        this.login = login;
        this.motDePasseHache = motDePasseHache;
        this.actif = actif;
        this.role = role;
    }

    // ---- Méthodes métier ----

    /**
     * Vérifie les identifiants.
     * NOTE : la vérification réelle (hash) sera déléguée à un service/DAO.
     */
    public boolean seConnecter(String login, String mdp) {
        if (!this.actif) {
            return false;
        }
        return Objects.equals(this.login, login)
                && Objects.equals(this.motDePasseHache, mdp);
    }

    public void seDeconnecter() {
        // Logique de déconnexion (session) à compléter par le Controller
   }

    // ---- Getters / Setters ----

    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getMotDePasseHache() { return motDePasseHache; }
    public void setMotDePasseHache(String motDePasseHache) { this.motDePasseHache = motDePasseHache; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

}
