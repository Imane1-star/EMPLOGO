# TransportPro — interface JavaFX

Version JavaFX (Java pur + CSS) de la maquette Figma Make « TransportPro Desktop App ».

## Lancer
Prérequis : JDK 17+ et Maven.

    mvn javafx:run

Connexion de démonstration : `transport.resp` / `transportpro` (mot de passe `erreur` = test de l'état d'erreur).
Le sélecteur en haut à droite permet de changer de profil (Responsable / Administrateur / Chauffeur).

## Structure
- `Main` : fenêtre + bascule connexion / application
- `LoginView` : écran de connexion
- `AppShell` : barre latérale, en-tête, modale, notifications
- `DashboardPage`, `ListPages` (Employés, Bus, Chauffeurs, Trajets, Incidents, Comptes), `AssignmentsPage`,
  `RegistrationsPage`, `AttendancePage`, `ReportsPage`, `RolesPage`, `PlanningPage`
- `Ui` : composants réutilisables, `Data` : données de démonstration
- `src/main/resources/styles.css` : couleurs et styles (palette #190019 / #2B124C / #522B5B / #854F6C / #DFB6B2 / #FBE4D8)
