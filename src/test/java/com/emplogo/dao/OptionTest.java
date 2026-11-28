package com.emplogo.dao;

import org.junit.jupiter.api.Test;

import com.emplogo.dao.Option;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de Option (record : identifiant + texte affiché dans une liste déroulante).
 * Aucune base de données n'est nécessaire.
 */
class OptionTest {

    @Test
    void accesseurs_renvoientLIdentifiantEtLeLibelle() {
        // Arrange + Act
        Option option = new Option(7, "T7 • Agadir - Inezgane");

        // Assert
        assertEquals(7, option.id());
        assertEquals("T7 • Agadir - Inezgane", option.label());
    }

    @Test
    void toString_renvoieLeLibelle_pourLAffichageDansUneListeDeroulante() {
        Option option = new Option(3, "Ligne 3");

        assertEquals("Ligne 3", option.toString());
    }

    @Test
    void toString_avecLibelleNul_renvoieNull() {
        // Comportement actuel : toString() renvoie le libellé tel quel, même nul.
        Option option = new Option(1, null);

        assertNull(option.toString());
    }

    @Test
    void egalite_deuxOptionsIdentiques_sontEgalesAvecLeMemeHashCode() {
        Option a = new Option(1, "Bus AB-123");
        Option b = new Option(1, "Bus AB-123");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void egalite_identifiantDifferent_neSontPasEgales() {
        assertFalse(new Option(1, "Bus").equals(new Option(2, "Bus")));
    }

    @Test
    void egalite_libelleDifferent_neSontPasEgales() {
        assertFalse(new Option(1, "Bus A").equals(new Option(1, "Bus B")));
    }

    @Test
    void egalite_avecNullOuAutreType_renvoieFaux() {
        Option option = new Option(1, "Bus");

        assertFalse(option.equals(null));
        assertFalse(option.equals("Bus"));
    }

    @Test
    void contains_retrouveUneOptionParValeurDansUneListe() {
        // Utile pour re-sélectionner l'option courante d'un formulaire de modification
        List<Option> options = List.of(new Option(1, "A"), new Option(2, "B"));

        assertTrue(options.contains(new Option(2, "B")));
        assertEquals(1, options.indexOf(new Option(2, "B")));
        assertFalse(options.contains(new Option(3, "C")));
    }
}