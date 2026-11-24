package com.transportpro.dao;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Erreur d'accès aux données avec un message lisible par l'utilisateur (en français).
 * Hérite de SQLException : les méthodes des DAO gardent la signature « throws SQLException »
 * attendue par le controller et les tests, et l'interface peut afficher getMessage() tel quel.
 */
public class DaoException extends SQLException {

    public DaoException(String message) {
        super(message);
    }

    public DaoException(String message, SQLException cause) {
        super(message, cause == null ? null : cause.getSQLState(), cause == null ? 0 : cause.getErrorCode(), cause);
    }

    /**
     * Traduit une erreur MySQL/MariaDB en message clair.
     * @param duplicate message pour une valeur UNIQUE déjà utilisée (erreur 1062)
     * @param inUse     message quand la ligne est encore utilisée ailleurs (erreur 1451, clé étrangère)
     * @param defaut    début du message pour les autres erreurs
     */
    public static DaoException translate(SQLException e, String duplicate, String inUse, String defaut) {
        if (e instanceof DaoException d) return d;
        String state = e.getSQLState();
        if (state != null && state.startsWith("08"))
            return new DaoException("Connexion à la base impossible. Vérifiez que MySQL/MariaDB est démarré (XAMPP).", e);
        if ("45000".equals(state))   // règle métier refusée par un déclencheur de la base
            return new DaoException(cleanTriggerMessage(e.getMessage()), e);
        return switch (e.getErrorCode()) {
            case 1062 -> new DaoException(duplicate != null ? duplicate : "Cette valeur existe déjà.", e);
            case 1451 -> new DaoException(inUse != null ? inUse
                    : "Cette ligne est utilisée par d'autres données : suppression impossible.", e);
            case 1452 -> new DaoException("Un élément choisi (employé, bus, trajet...) n'existe pas dans la base.", e);
            case 1406 -> new DaoException("Une valeur est trop longue pour la base de données.", e);
            case 1265, 1366 -> new DaoException("Une valeur choisie n'est pas acceptée par la base.", e);
            case 1292 -> new DaoException("La date ou l'heure saisie n'est pas valide.", e);
            case 1054, 1146 -> new DaoException("La base de données n'est pas à jour (colonne ou table manquante). "
                    + "Réimportez src/main/java/com/transportpro/dao/dataBase.sql. Détail : " + e.getMessage(), e);
            default -> new DaoException(defaut + " (" + e.getMessage() + ")", e);
        };
    }

    /** Message d'un déclencheur : sans préfixe de connexion ni texte de requête. */
    static String cleanTriggerMessage(String raw) {
        String m = raw == null ? "" : raw;
        int nl = m.indexOf('\n');
        if (nl >= 0) m = m.substring(0, nl);
        Matcher matcher = Pattern.compile("^\\(conn=\\d+\\)\\s*").matcher(m);
        m = matcher.replaceFirst("").trim();
        return m.isEmpty() ? "Opération refusée par la base de données." : m;
    }
}
