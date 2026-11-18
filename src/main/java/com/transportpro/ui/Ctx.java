package com.transportpro.ui;

/** Services partagés que les pages peuvent appeler (navigation, fenêtre modale, notification). */
public interface Ctx {
    void navigate(Page page);
    void openModal(String title);
    void toast(String message);

    /** Profil courant : « Responsable », « Administrateur » ou « Chauffeur ». */
    default String role() { return "Responsable"; }

    default boolean isDriver() { return "Chauffeur".equals(role()); }
}
