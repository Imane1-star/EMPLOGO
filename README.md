# 🚌 EMPLOGO (Employee Go)

**EMPLOGO** est un projet universitaire de gestion du transport du personnel. Il consiste à développer une application desktop permettant de faciliter l'organisation et le suivi du transport des employés d'une entreprise.

L'application centralise les informations relatives aux employés, aux bus, aux chauffeurs, aux trajets, aux inscriptions et aux incidents, tout en facilitant le travail du responsable du transport.

## 🎯 Objectifs

- Simplifier la planification des trajets.
- Faciliter la gestion des employés, des bus et des chauffeurs.
- Organiser les inscriptions et le suivi des présences.
- Vérifier la disponibilité des ressources et respecter les règles de gestion.
- Assurer le suivi des incidents.
- Fournir des statistiques utiles au suivi du transport.

## ✨ Fonctionnalités principales

- **Gestion des employés :** ajout, modification, recherche et suppression, avec gestion de l'historique (désactivation d'un employé lié à d'autres données).
- **Gestion des bus :** suivi des informations, de la capacité et du statut des bus (disponible, en maintenance, hors service).
- **Gestion des chauffeurs :** gestion des informations, des permis et de la disponibilité (chaque chauffeur est un employé).
- **Gestion des trajets et des affectations :** création, modification, recherche des lignes de transport, puis affectation datée d'un bus et d'un chauffeur avec contrôle des conflits.
- **Gestion des inscriptions :** inscription des employés aux trajets, annulation et suivi des inscriptions.
- **Gestion des présences :** enregistrement de la présence de l'employé (présent, absent, en retard) depuis sa fiche d'inscription.
- **Gestion des incidents :** déclaration, consultation, modification et suivi des incidents (retard, panne, accident, comportement, autre).
- **Gestion des rôles :** profils Responsable, Administrateur et Chauffeur, avec un menu adapté à chaque profil.
- **Tableau de bord :** présentation d'indicateurs et d'informations sur l'activité du transport.

L'application intègre également des règles de gestion pour contrôler les affectations, les disponibilités et les contraintes liées aux ressources. Ces règles sont appliquées directement dans la base de données (procédures de contrôle et déclencheurs).

## 🚀 Technologies et outils utilisés

| Technologie | Utilisation |
|---|---|
| Java 17 | Langage de programmation |
| JavaFX 21 | Création des interfaces graphiques |
| Maven | Gestion du projet et des dépendances |
| MySQL | Stockage et gestion des données |
| JDBC (MySQL Connector/J 8.0.33) | Communication entre Java et MySQL |
| JUnit 5 | Réalisation des tests |
| Figma | Création des maquettes |
| UML | Modélisation du système |
| Git & GitHub | Gestion des versions et collaboration |

## 🏗️ Architecture du projet

Le projet adopte une architecture **MVC (Model – View – Controller)** associée à une couche DAO pour séparer l'interface graphique, la logique de coordination et l'accès aux données.

- **Model :** contient les classes représentant les entités métier et les énumérations (rôles, statuts, types d'incident).
- **View :** contient les interfaces JavaFX, les pages et les fenêtres de saisie de l'application.
- **Controller :** point d'entrée unique des vues vers les DAO (View → Controller → DAO) ; il coordonne les actions de l'utilisateur et les opérations métier.
- **DAO :** assure les opérations d'accès à la base de données MySQL et traduit les erreurs SQL en messages lisibles.

Cette organisation permet de faciliter la maintenance, les tests et l'évolution de l'application.

Un « trajet » daté correspond à une ligne de la table `affectation` (ligne de transport + date + bus + chauffeur) ; la table `trajet` ne contient que les lignes de transport (nom, lieu de départ, lieu d'arrivée).

## 📁 Structure du projet

```text
EMPLOGO/
│
├── pom.xml
├── .gitignore
├── db.properties
│
├── docs/
│   ├── Rapport_EMPLOGO.pdf
│   └── EMPLOGO_Guide.pdf
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── emplogo/
    │   │           ├── Main.java
    │   │           ├── Ctx.java
    │   │           ├── Page.java
    │   │           ├── Data.java
    │   │           │
    │   │           ├── controller/
    │   │           │   └── MainController.java
    │   │           │
    │   │           ├── dao/
    │   │           │   ├── dataBase.sql
    │   │           │   ├── DatabaseConnection.java
    │   │           │   ├── DaoException.java
    │   │           │   ├── Option.java
    │   │           │   ├── AffectationDAO.java
    │   │           │   ├── BusDAO.java
    │   │           │   ├── ChauffeurDAO.java
    │   │           │   ├── EmployeDAO.java
    │   │           │   ├── IncidentDAO.java
    │   │           │   ├── InscriptionDAO.java
    │   │           │   └── TrajetDAO.java
    │   │           │
    │   │           ├── model/
    │   │           │   ├── Utilisateur.java
    │   │           │   ├── Administrateur.java
    │   │           │   ├── ResponsableTransport.java
    │   │           │   ├── Chauffeur.java
    │   │           │   ├── Employe.java
    │   │           │   ├── Bus.java
    │   │           │   ├── Trajet.java
    │   │           │   ├── Arret.java
    │   │           │   ├── Affectation.java
    │   │           │   ├── Inscription.java
    │   │           │   ├── Incident.java
    │   │           │   ├── Rapport.java
    │   │           │   └── enums/
    │   │           │       ├── Role.java
    │   │           │       ├── StatutBus.java
    │   │           │       ├── StatutTrajet.java
    │   │           │       ├── StatutPresence.java
    │   │           │       └── TypeIncident.java
    │   │           │
    │   │           └── view/
    │   │               ├── AppShell.java
    │   │               ├── LoginView.java
    │   │               ├── DashboardPage.java
    │   │               ├── ListPages.java
    │   │               ├── AssignmentsPage.java
    │   │               ├── RegistrationsPage.java
    │   │               ├── AttendancePage.java
    │   │               ├── ReportsPage.java
    │   │               ├── RolesPage.java
    │   │               ├── PlanningPage.java
    │   │               ├── EmployeDialog.java
    │   │               ├── BusDialog.java
    │   │               ├── ChauffeurDialog.java
    │   │               ├── AffectationDialog.java
    │   │               ├── IncidentDialog.java
    │   │               ├── DeleteDialog.java
    │   │               ├── DialogKit.java
    │   │               └── Ui.java
    │   │
    │   └── resources/
    │       ├── styles.css
    │       ├── emplogo.png
    │       └── images/
    │           └── emplogo.png
    │
    └── test/
        └── java/
            └── com/
                └── emplogo/
                    ├── model/
                    ├── dao/
                    └── controller/
```

*Remarque : cette arborescence présente les principaux packages et fichiers de l'application. Elle n'est pas une liste exhaustive de toutes les classes.*

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

**Établissement :** Faculté des Sciences Appliquées de Taroudant

**Formation :** Licence Génie Informatique – Semestre 5

**Année universitaire :** 2026/2027

---

## 🛠️ Installation et utilisation

EMPLOGO est une application desktop développée avec JavaFX et Maven, utilisant MySQL comme système de gestion de base de données.

### Prérequis

- JDK 17 ou supérieur (JavaFX 21).
- Maven 3.9 ou supérieur.
- MySQL 8.0.16 ou supérieur, ou MariaDB (par exemple via XAMPP).
- Un IDE Java (IntelliJ IDEA, Eclipse ou Visual Studio Code).

### 1. Récupérer le projet

```bash
git clone https://github.com/Imane1-star/EMPLOGO.git
cd EMPLOGO
```

### 2. Configurer la base de données

1. Démarrer le serveur MySQL ou MariaDB (par défaut sur `localhost:3306`).
2. Importer le script SQL `src/main/java/com/emplogo/dao/dataBase.sql`. Il crée la base `employgo`, ses 13 tables, les procédures de contrôle `sp_verifier_*`, les déclencheurs et des données de test :

   ```bash
   mysql -u root < src/main/java/com/emplogo/dao/dataBase.sql
   ```

   (ou, depuis phpMyAdmin ou MySQL Workbench, utiliser la fonction d'importation).
3. Configurer les paramètres de connexion (URL, utilisateur, mot de passe) dans le fichier local `db.properties` à la racine du projet, selon la configuration de votre ordinateur.

Le fichier local `db.properties` contient les paramètres de connexion et ne doit pas être partagé sur GitHub (il est ignoré par Git).

Il est aussi possible d'utiliser une autre base au lancement, par exemple une copie de test :

```bash
-Demplogo.db.url=jdbc:mysql://localhost:3306/employgo_test
```

### 3. Compiler et lancer l'application

Pour compiler le projet :

```bash
mvn compile
```

Pour exécuter les tests :

```bash
mvn clean test
```

Pour lancer l'application JavaFX :

```bash
mvn javafx:run
```

L'écran de connexion propose un profil de démonstration : le profil (Responsable, Administrateur ou Chauffeur) se choisit sur cet écran et détermine les pages accessibles. Un compte de démonstration est fourni : `transport.resp` / `transportpro` (le mot de passe `erreur` permet de tester l'état d'erreur).

## 🧪 Tests

Le projet utilise **JUnit 5** pour vérifier le fonctionnement de certaines classes et opérations.

Les tests sont organisés en trois groupes (`model`, `dao`, `controller`) et permettent notamment de contrôler :

- Le comportement des classes métier (employé, bus, trajet, inscription).
- Les opérations d'accès aux données de chaque DAO.
- La connexion à la base et la gestion des erreurs (`DatabaseConnection`, `DaoException`).
- Les opérations CRUD (Create, Read, Update, Delete).
- Le comportement du contrôleur principal.
- Certaines règles de gestion et contraintes métier.

Pour exécuter les tests avec Maven :

```bash
mvn clean test
```

**Important :** les tests qui interagissent directement avec MySQL doivent être exécutés dans un environnement de test adapté (par exemple une base `employgo_test`) afin d'éviter de modifier les données de production.

## ⚙️ Gestion des données et règles métier

L'application utilise des contraintes et des règles de gestion pour préserver la cohérence des données. Les contrôles sont réalisés dans MySQL par des procédures `sp_verifier_*` appelées par des déclencheurs, et leurs messages d'erreur sont affichés dans l'interface.

Par exemple :

- Un bus indisponible, ou en maintenance planifiée ou en cours à la date du trajet, ne peut pas être affecté à un trajet.
- Un chauffeur doit être actif et respecter les conditions de validité de son permis à la date du trajet.
- Les conflits d'affectation (bus ou chauffeur déjà affecté sur un créneau qui se chevauche) sont contrôlés.
- Les contrôles sur les inscriptions sont appliqués à la création et à la modification.
- Le statut d'un bus est synchronisé automatiquement avec ses maintenances.
- Les clés étrangères protègent les relations entre les différentes tables.
- Certaines suppressions sont remplacées par une désactivation ou une annulation afin de conserver l'historique : désactiver l'employé, suspendre le chauffeur, mettre le bus hors service, annuler le trajet ou l'inscription. Lorsque c'est possible, l'application propose aussi « Supprimer tout » (en une seule transaction).

## 🚧 État actuel du développement

Les fonctionnalités suivantes sont connectées à la base de données MySQL et permettent les opérations de gestion correspondantes :

- La gestion des employés, des bus et des chauffeurs.
- La gestion des trajets (lignes) et des affectations.
- La gestion des inscriptions et l'enregistrement des présences depuis les inscriptions.
- La gestion des incidents.

D'autres écrans utilisent encore des données de démonstration et nécessitent une intégration supplémentaire, notamment :

- La page de suivi des présences.
- L'authentification et la gestion des comptes (le profil est choisi sur l'écran de connexion).
- Les rapports et statistiques.
- La gestion des rôles et des autorisations.
- Le planning du chauffeur.
- Certains indicateurs du tableau de bord.

Ces éléments seront progressivement améliorés au fur et à mesure de l'évolution du projet.

## 📚 Documentation

Le dossier `docs/` contient les documents relatifs au projet, notamment :

- Le rapport de conception et de réalisation.
- Le guide d'utilisation de l'application.

---

**EMPLOGO – Employee Go**  
*Une solution de gestion et d'organisation du transport du personnel.*
