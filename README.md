# TransportPro / EMPLOGO — application JavaFX + MySQL

Application de gestion du transport des employés (architecture MVC : model / dao / controller / view).

## Prérequis
- JDK 21+ (JavaFX 21), Maven 3.9+
- MySQL ou MariaDB (XAMPP) sur `localhost:3306`, utilisateur `root` sans mot de passe

## Base de données
Le script `src/main/java/com/transportpro/dao/dataBase.sql` crée la base **`employgo`**
(13 tables, procédures `sp_verifier_*`, déclencheurs et données de test) :

    mysql -u root < src/main/java/com/transportpro/dao/dataBase.sql

(ou phpMyAdmin → Importer). Une autre base peut être utilisée au lancement :
`-Demplogo.db.url=jdbc:mysql://localhost:3306/employgo_test` (voir `dao/DatabaseConnection`).

## Lancer

    mvn javafx:run

Connexion de démonstration : `transport.resp` / `transportpro` (mot de passe `erreur` = test de l'état d'erreur).
Le profil (Responsable / Administrateur / Chauffeur) se choisit sur l'écran de connexion.

## Structure (MVC)
- `model/` : Utilisateur (← Administrateur, ResponsableTransport, Chauffeur), Employe, Bus, Trajet, Affectation,
  Arret, Inscription, Incident, Rapport + `enums/` (Role, StatutBus, StatutTrajet, StatutPresence, TypeIncident)
- `dao/` : DatabaseConnection, EmployeDAO, BusDAO, ChauffeurDAO, TrajetDAO, AffectationDAO, InscriptionDAO,
  IncidentDAO, DaoException (messages d'erreur lisibles), Option (listes déroulantes)
- `controller/MainController` : seul point d'entrée des vues vers les DAO (View → Controller → DAO)
- `view/` : AppShell, LoginView, pages (Dashboard, ListPages, Assignments, Registrations, Attendance, Reports,
  Roles, Planning), fenêtres de saisie (EmployeDialog, BusDialog, ChauffeurDialog, AffectationDialog,
  IncidentDialog), DeleteDialog, Ui
- `Main`, `Ctx`, `Page`, `Data` (données de démonstration des pages pas encore reliées à la base)
- `src/main/resources/styles.css` : couleurs et styles (palette #190019 / #2B124C / #522B5B / #854F6C / #DFB6B2 / #FBE4D8)

Un « trajet » daté correspond à une ligne de la table `affectation` (ligne de transport + date + bus + chauffeur) ;
la table `trajet` ne contient que les lignes (nom, départ, arrivée).

## Suppression
Les clés étrangères protègent l'historique. Si la ligne est liée à d'autres données, l'application propose
l'action qui conserve l'historique (désactiver l'employé, suspendre le chauffeur, mettre le bus hors service,
annuler le trajet ou l'inscription) ou « Supprimer tout » (en une transaction) quand c'est possible.

## Tests
- `mvn test` : tests JUnit 5 (`src/test/java/.../model`, `dao`, `controller`) sur la base `employgo`.
- Vérifications manuelles qui **modifient la base** (à lancer sur une copie `employgo_test`) :
  `integration/CrudCheck`, `DeleteCheck`, `IncidentCheck` ; `SmokeCheck` ouvre chaque page (lecture seule).

      mvn test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
      java -Demplogo.db.url=jdbc:mysql://localhost:3306/employgo_test -cp "target/classes;target/test-classes;<contenu de target/cp.txt>" com.transportpro.integration.CrudCheck
