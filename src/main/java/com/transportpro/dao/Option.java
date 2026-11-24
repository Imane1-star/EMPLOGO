package com.transportpro.dao;

/** Élément d'une liste déroulante : un identifiant de la base et le texte affiché. */
public record Option(int id, String label) {
    @Override
    public String toString() { return label; }
}
