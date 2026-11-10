package com.transportpro.model;

import com.transportpro.model.enums.Role;

public class Administrateur extends Utilisateur {

    public Administrateur() {
        super();
        setRole(Role.ADMINISTRATEUR);
    }

    public Administrateur(int idUtilisateur, String login, String motDePasseHache, boolean actif) {
        super(idUtilisateur, login, motDePasseHache, actif, Role.ADMINISTRATEUR);
    }

    public void gererComptes() {
        // TODO : déléguer à un service / DAO
    }

    public void gererRoles() {
        // TODO : déléguer à un service / DAO
    }
}