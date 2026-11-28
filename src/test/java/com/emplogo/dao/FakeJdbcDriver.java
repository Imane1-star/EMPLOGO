package com.emplogo.dao;

import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Faux pilote JDBC utilisé par les tests de DAO : aucune base de données n'est nécessaire.
 *
 * Il accepte les URL "jdbc:fake:..." et renvoie de fausses connexions qui notent
 * dans {@link #events} tout ce que le DAO leur demande (requête préparée, paramètres,
 * exécution, fermeture...). On peut aussi lui faire renvoyer des lignes ou des erreurs SQL.
 */
class FakeJdbcDriver implements Driver {

    /** Journal des appels, dans l'ordre. Exemple : "prepare:SELECT ...", "setInt:1=5". */
    List<String> events = new ArrayList<>();

    /** Nombre de lignes que le ResultSet fait semblant de contenir (si rowData est vide). */
    int rows = 0;
    /**
     * Lignes détaillées : une Map "nom de colonne -> valeur" par ligne
     * (Integer pour getInt, String pour getString, java.sql.Date, java.sql.Time...).
     * Si cette liste n'est pas vide, elle remplace "rows".
     */
    List<Map<String, Object>> rowData = new ArrayList<>();
    /** Valeur renvoyée par rs.getString(...). */
    String stringValue;
    /** Valeur renvoyée par rs.getInt(...). */
    int intValue;
    /** Valeur renvoyée par ps.executeUpdate(). */
    int updateResult;
    /** Si non nul : erreur levée à la connexion. */
    SQLException failOnConnect;
    /** Si non nul : erreur levée à executeQuery() / executeUpdate(). */
    SQLException failOnExecute;

    private int nextCalls;

    void reset() {
        events = new ArrayList<>();
        rows = 0;
        rowData = new ArrayList<>();
        stringValue = null;
        intValue = 0;
        updateResult = 0;
        failOnConnect = null;
        failOnExecute = null;
        nextCalls = 0;
    }

    @FunctionalInterface
    private interface Handler {
        Object handle(String methode, Object[] args) throws Throwable;
    }

    @Override
    public boolean acceptsURL(String url) {
        return url != null && url.startsWith("jdbc:fake:");
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        if (failOnConnect != null) {
            throw failOnConnect;
        }
        events.add("connect");

        return proxy(Connection.class, (m, args) -> switch (m) {
            case "prepareStatement" -> {
                events.add("prepare:" + args[0]);
                yield fakeStatement();
            }
            case "setAutoCommit" -> {
                events.add("setAutoCommit:" + args[0]);
                yield null;
            }
            case "commit", "rollback", "close" -> {
                events.add(m);
                yield null;
            }
            default -> null;
        });
    }

    private PreparedStatement fakeStatement() {
        return proxy(PreparedStatement.class, (m, args) -> switch (m) {
            case "setInt" -> {
                events.add("setInt:" + args[0] + "=" + args[1]);
                yield null;
            }
            case "setString" -> {
                events.add("setString:" + args[0] + "=" + args[1]);
                yield null;
            }
            case "executeQuery" -> {
                events.add("executeQuery");
                if (failOnExecute != null) {
                    throw failOnExecute;
                }
                yield fakeResultSet();
            }
            case "executeUpdate" -> {
                events.add("executeUpdate");
                if (failOnExecute != null) {
                    throw failOnExecute;
                }
                yield updateResult;
            }
            case "close" -> {
                events.add("closeStatement");
                yield null;
            }
            default -> null;
        });
    }

    private int totalRows() {
        return rowData.isEmpty() ? rows : rowData.size();
    }

    /** Valeur d'une colonne de la ligne courante (par nom), ou valeur par défaut (par numéro). */
    private Object valeur(String methode, Object colonne) {
        if (colonne instanceof String nom) {
            boolean ligneCourante = nextCalls >= 1 && nextCalls <= rowData.size();
            return ligneCourante ? rowData.get(nextCalls - 1).get(nom) : null;
        }
        return switch (methode) {
            case "getString" -> stringValue;
            case "getInt" -> intValue;
            default -> null;
        };
    }

    private ResultSet fakeResultSet() {
        nextCalls = 0;
        return proxy(ResultSet.class, (m, args) -> switch (m) {
            case "next" -> nextCalls++ < totalRows();
            case "getString", "getInt", "getDate", "getTime", "getTimestamp" -> valeur(m, args[0]);
            case "close" -> {
                events.add("closeResultSet");
                yield null;
            }
            default -> null;
        });
    }

    /** Fabrique un objet qui implémente l'interface JDBC demandée. */
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[] { type },
                (p, method, args) -> {
                    Object resultat = handler.handle(method.getName(), args);
                    Class<?> retour = method.getReturnType();
                    if (resultat == null && retour.isPrimitive() && retour != void.class) {
                        // valeur par défaut (0 / false) pour les méthodes non simulées
                        return Array.get(Array.newInstance(retour, 1), 0);
                    }
                    return resultat;
                });
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }
}