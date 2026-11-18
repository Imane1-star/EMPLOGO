# 🚌 EMPLOGO (Employee Go)

**EMPLOGO** est un projet universitaire de gestion du transport du personnel ,visant à développer une application desktop permettant de faciliter l'organisation et le suivi du transport des employés d'une entreprise.

L'application a pour objectif de centraliser les informations relatives aux employés, aux bus, aux chauffeurs, aux trajets et aux inscriptions, tout en facilitant le travail du responsable du transport.

## 🎯 Objectifs

- Simplifier la planification des trajets.
- Faciliter la gestion des employés, des bus et des chauffeurs.
- Organiser les inscriptions et le suivi des présences.
- Vérifier la disponibilité des ressources et respecter les règles de gestion.
- Fournir des statistiques utiles au suivi du transport.

## ✨ Fonctionnalités principales

- Gestion des employés, des bus et des chauffeurs.
- Planification des trajets et gestion des arrêts.
- Affectation des bus et des chauffeurs.
- Gestion des inscriptions et des présences.
- Suivi des incidents.
- Consultation de statistiques et génération de rapports.
- Authentification et gestion des rôles.

*Les fonctionnalités seront précisées et adaptées au fur et à mesure de l'évolution du projet.*

## 🚀 Technologies et outils utilisés

| Technologie | Utilisation |
| --- | --- |
| Java  | Langage de programmation |
| JavaFX | Création des interfaces graphiques |
| Maven | Gestion du projet et des dépendances |
| Scene Builder | Conception des interfaces |
| Figma | Création des maquettes |
| UML | Modélisation du système |
| Git & GitHub | Gestion des versions et collaboration |

## 🎨 Maquettes Figma

Les maquettes de l'application sont disponibles sur Figma :

🔗 Consulter les maquettes EMPLOGO

## 👩‍💻 Équipe du projet

Projet réalisé par :

- **Imane KANOUNI**
- **Sara SERBOUT**
- **Bouchra ABLALLAH**
- **Aya EL HALLABI**
- **Najwa BOUKAFTANE**
- **Laila ELMAJDI**

**Encadrant :** Pr. Youssef ES-SAADY

**Établissement :** Faculté  de sciences Appliquées Taroudant

**Formation :** Licence Génie Informatique – Semestre 5

**Année universitaire :** 2026/2027


---

## 🛠️ Installation et utilisation

Application desktop JavaFX + MariaDB/MySQL (projet POO avancée, FSA Taroudant).
Le cahier des charges et la conception sont dans `docs/Rapport_EMPLOGO.pdf`.

### Prérequis
- JDK 21 ou plus (JavaFX 22 exige Java 21+)
- Maven 3.9+
- MariaDB (XAMPP) ou MySQL sur `localhost:3306`, utilisateur `root` sans mot de passe
  (modifiable dans `db/DatabaseConnection.java`)

### Base de données
1. Démarrer MySQL/MariaDB (panneau XAMPP → *MySQL* → *Start*).
2. Si la base n'existe pas encore : créer la base `emplogo` puis importer `database/emplogo.sql`
   (phpMyAdmin → Importer, ou `mysql -u root -e "CREATE DATABASE emplogo"` puis
   `mysql -u root emplogo < database/emplogo.sql`).

3. Base créée avec une **ancienne version** du schéma (colonne `chauffeur.validite_permis`) :
   exécuter une fois, dans l’ordre, `database/migrations/001_align_live_schema.sql` puis
   `002_align_incident_maintenance.sql` (`mysql -u root emplogo < fichier.sql`)
   (les données sont conservées ; faites d'abord une sauvegarde depuis phpMyAdmin).

### Suppression
Les clés étrangères protègent l'historique. Quand on supprime une ligne liée à d'autres données,
l'application l'explique et propose :
- l'action qui **conserve l'historique** (recommandée) : désactiver l'employé ou le chauffeur,
  mettre le bus hors service, annuler le trajet ou l'inscription ;
- **Supprimer tout** quand c'est possible sans toucher aux données des autres
  (ex. employé passager + ses inscriptions et présences ; trajet + ses inscriptions et incidents).
  Un chauffeur ou un bus qui a déjà des trajets ne peut pas être supprimé.

### Tests
Les tests `CrudTest` et `DeleteTest` **modifient la base** : lancez-les sur une copie `emplogo_test`.

    mvn test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
    java -Demplogo.db.url=jdbc:mysql://localhost:3306/emplogo_test -cp "target/classes;target/test-classes;<contenu de target/cp.txt>" com.transportpro.CrudTest

- `SmokeTest` : ouvre chaque page pour chaque profil (lecture seule)
- `CrudTest` : ajout / modification / recherche + règles métier RG1–RG7
- `DeleteTest` : suppression, désactivation, annulation de chaque module
- `IncidentTest` : incidents (ajout, modification, recherche, validation, suppression)

### Compiler et lancer

    mvn compile          # compile
    mvn javafx:run       # lance l'application

Dans IntelliJ : fenêtre *Maven* → `Plugins` → `javafx` → `javafx:run`.

Connexion de démonstration : `transport.resp` / `transportpro` (mot de passe `erreur` = test de l'état d'erreur).
Le profil (Responsable / Administrateur / Chauffeur) se choisit sur l'écran de connexion.

### Structure

```
src/main/java/com/transportpro/
├── Main.java              point d'entrée (fenêtre, bascule connexion / application)
├── db/                    DatabaseConnection (JDBC)
├── employee/              Employee, EmployeeDAO, EmployeeDialog
├── bus/                   Bus, BusDAO, BusDialog
├── chauffeur/             Chauffeur, ChauffeurDAO, ChauffeurDialog
├── trajet/                Trajet, TrajetDAO, TrajetDialog   (table affectation)
├── inscription/           Inscription, InscriptionDAO
├── presence/              Presence, PresenceDAO
├── incident/              Incident, IncidentDAO, IncidentDialog
└── ui/                    AppShell, LoginView, pages (Dashboard, ListPages, Assignments,
                           Registrations, Attendance, Reports, Roles, Planning), Ui, Data, Page, Ctx
src/main/resources/        styles.css, images/emplogo.png
src/test/java/             SmokeTest, CrudTest, DeleteTest, IncidentTest, TestEmployees
database/emplogo.sql       schéma de référence + procédures + triggers + données
database/migrations/       mises à jour d’une base existante
docs/                      rapport + EMPLOGO_Guide.pdf (guide pas à pas)
```

Pages encore sur données de démonstration (`ui/Data.java`, formulaires non enregistrés) : présences,
comptes, rapports, rôles, planning chauffeur (le tableau de bord affiche aussi des chiffres de démonstration).
