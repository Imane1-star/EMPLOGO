package com.emplogo;

/** Services partagés que les pages peuvent appeler (navigation, fenêtre modale, notification). */
public interface Ctx {
    void navigate(Page page);
    void openModal(String title);
    void toast(String message);

    /** Message d'erreur ; par défaut affiché comme une notification (AppShell l'affiche dans une fenêtre). */
    default void error(String message) { toast(message); }

    /** Profil courant : « Responsable », « Administrateur » ou « Chauffeur ». */
    default String role() { return "Responsable"; }

    default boolean isDriver() { return "Chauffeur".equals(role()); }
}
