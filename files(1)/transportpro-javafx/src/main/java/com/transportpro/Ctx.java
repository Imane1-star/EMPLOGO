package com.transportpro;

/** Services partagés que les pages peuvent appeler (navigation, fenêtre modale, notification). */
public interface Ctx {
    void navigate(Page page);
    void openModal(String title);
    void toast(String message);
}
