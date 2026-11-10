-- =====================================================================
-- EmployGo - Gestion du transport des employés
-- Script MySQL complet : base, tables, contraintes, triggers, données de test
-- Compatible : MySQL 8.0.16 ou plus (les CHECK sont appliqués), MySQL Workbench
--
-- Ordre du script :
-- 1. Création de la base
-- 2. Création des 13 tables (dans l'ordre des dépendances)
-- 3. Procédures de contrôle + triggers (règles de gestion)
-- 4. Données de test
--
-- Pour repartir de zéro, décommentez la ligne DROP DATABASE ci-dessous
-- (ATTENTION : elle supprime toute la base employgo et ses données).
-- =====================================================================
-- DROP DATABASE IF EXISTS employgo;
CREATE DATABASE IF NOT EXISTS employgo
 CHARACTER SET utf8mb4
 COLLATE utf8mb4_unicode_ci;
USE employgo;
-- =====================================================================
-- 2. TABLES
-- Ordre : une table doit exister avant qu'une autre ne la référence.
-- =====================================================================
-- ---------------------------------------------------------------------
-- 2.1 Sécurité : rôles, permissions, liaison
-- ---------------------------------------------------------------------
CREATE TABLE role (
 id_role INT UNSIGNED NOT NULL AUTO_INCREMENT,
 nom VARCHAR(50) NOT NULL,
 description VARCHAR(255) NULL,
 CONSTRAINT pk_role PRIMARY KEY (id_role),
 CONSTRAINT uq_role_nom UNIQUE (nom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE permission (
 id_permission INT UNSIGNED NOT NULL AUTO_INCREMENT,
 code VARCHAR(80) NOT NULL,
 description VARCHAR(255) NULL,
 CONSTRAINT pk_permission PRIMARY KEY (id_permission),
 CONSTRAINT uq_permission_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE role_permission (
 id_role INT UNSIGNED NOT NULL,
 id_permission INT UNSIGNED NOT NULL,
 CONSTRAINT pk_role_permission PRIMARY KEY (id_role, id_permission),
 CONSTRAINT fk_rp_role
 FOREIGN KEY (id_role) REFERENCES role (id_role)
 ON DELETE CASCADE ON UPDATE CASCADE,
 CONSTRAINT fk_rp_permission
 FOREIGN KEY (id_permission) REFERENCES permission (id_permission)
 ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- ---------------------------------------------------------------------
-- 2.2 Personnel : employés, comptes, chauffeurs
-- ---------------------------------------------------------------------
CREATE TABLE employe (
 id_employe INT UNSIGNED NOT NULL AUTO_INCREMENT,
 matricule VARCHAR(20) NOT NULL,
 nom VARCHAR(60) NOT NULL,
 prenom VARCHAR(60) NOT NULL,
 email VARCHAR(120) NOT NULL,
 telephone VARCHAR(20) NULL,
 adresse VARCHAR(255) NULL,
 departement VARCHAR(80) NULL,
 date_embauche DATE NULL,
 actif TINYINT(1) NOT NULL DEFAULT 1,
 CONSTRAINT pk_employe PRIMARY KEY (id_employe),
 CONSTRAINT uq_employe_matricule UNIQUE (matricule),
 CONSTRAINT uq_employe_email UNIQUE (email),
 CONSTRAINT ck_employe_actif CHECK (actif IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- id_employe est facultatif (compte sans employé possible) et UNIQUE
-- (un employé a au plus un compte) : cardinalité (0,1)-(0,1).
CREATE TABLE compte_utilisateur (
 id_compte INT UNSIGNED NOT NULL AUTO_INCREMENT,
 login VARCHAR(60) NOT NULL,
 mot_de_passe_hash VARCHAR(255) NOT NULL,
 actif TINYINT(1) NOT NULL DEFAULT 1,
 derniere_connexion DATETIME NULL,
 id_role INT UNSIGNED NOT NULL,
 id_employe INT UNSIGNED NULL,
 CONSTRAINT pk_compte_utilisateur PRIMARY KEY (id_compte),
 CONSTRAINT uq_compte_login UNIQUE (login),
 CONSTRAINT uq_compte_employe UNIQUE (id_employe),
 CONSTRAINT ck_compte_actif CHECK (actif IN (0, 1)),
 CONSTRAINT fk_compte_role
 FOREIGN KEY (id_role) REFERENCES role (id_role)
 ON DELETE RESTRICT ON UPDATE CASCADE,
 CONSTRAINT fk_compte_employe
 FOREIGN KEY (id_employe) REFERENCES employe (id_employe)
 ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Un chauffeur EST un employé : id_employe NOT NULL + UNIQUE = (1,1)-(0,1).
-- Aucune donnée personnelle n'est dupliquée (nom, prénom... restent dans employe).
CREATE TABLE chauffeur (
 id_chauffeur INT UNSIGNED NOT NULL AUTO_INCREMENT,
 id_employe INT UNSIGNED NOT NULL,
 numero_permis VARCHAR(30) NOT NULL,
 validite_permis DATE NOT NULL,
 statut ENUM('ACTIF','EN_CONGE','SUSPENDU') NOT NULL DEFAULT 'ACTIF',
 CONSTRAINT pk_chauffeur PRIMARY KEY (id_chauffeur),
 CONSTRAINT uq_chauffeur_employe UNIQUE (id_employe),
 CONSTRAINT uq_chauffeur_permis UNIQUE (numero_permis),
 CONSTRAINT fk_chauffeur_employe
 FOREIGN KEY (id_employe) REFERENCES employe (id_employe)
 ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- ---------------------------------------------------------------------
-- 2.3 Flotte : bus, maintenances
-- ---------------------------------------------------------------------
CREATE TABLE bus (
 id_bus INT UNSIGNED NOT NULL AUTO_INCREMENT,
 immatriculation VARCHAR(20) NOT NULL,
 marque VARCHAR(50) NULL,
 modele VARCHAR(50) NULL,
 capacite SMALLINT UNSIGNED NOT NULL,
 statut ENUM('DISPONIBLE','EN_MAINTENANCE','HORS_SERVICE') NOT NULL DEFAULT 'DISPONIBLE',
 CONSTRAINT pk_bus PRIMARY KEY (id_bus),
 CONSTRAINT uq_bus_immatriculation UNIQUE (immatriculation),
 CONSTRAINT ck_bus_capacite CHECK (capacite > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE maintenance (
 id_maintenance INT UNSIGNED NOT NULL AUTO_INCREMENT,
 id_bus INT UNSIGNED NOT NULL,
 type ENUM('PREVENTIVE','CORRECTIVE') NOT NULL,
 date_debut DATE NOT NULL,
 date_fin DATE NULL,
 statut ENUM('PLANIFIEE','EN_COURS','TERMINEE','ANNULEE') NOT NULL,
 CONSTRAINT pk_maintenance PRIMARY KEY (id_maintenance),
 CONSTRAINT ck_maintenance_dates CHECK (date_fin IS NULL OR date_fin >= date_debut),
 CONSTRAINT fk_maintenance_bus
 FOREIGN KEY (id_bus) REFERENCES bus (id_bus)
 ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- ---------------------------------------------------------------------
-- 2.4 Exploitation : trajets, affectations, inscriptions, présences, incidents
-- ---------------------------------------------------------------------
-- Un trajet est un ITINÉRAIRE (aucun horaire ici).
CREATE TABLE trajet (
 id_trajet INT UNSIGNED NOT NULL AUTO_INCREMENT,
 nom VARCHAR(100) NOT NULL,
 lieu_depart VARCHAR(120) NOT NULL,
 lieu_arrivee VARCHAR(120) NOT NULL,
 CONSTRAINT pk_trajet PRIMARY KEY (id_trajet),
 CONSTRAINT uq_trajet_nom UNIQUE (nom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Une affectation = un trajet programmé à une date, avec un bus et un chauffeur.
-- Pas de UNIQUE sur (bus, date, heure) : les chevauchements sont contrôlés par trigger
-- (en ignorant les affectations ANNULEE).
CREATE TABLE affectation (
 id_affectation INT UNSIGNED NOT NULL AUTO_INCREMENT,
 id_trajet INT UNSIGNED NOT NULL,
 id_bus INT UNSIGNED NOT NULL,
 id_chauffeur INT UNSIGNED NOT NULL,
 date_trajet DATE NOT NULL,
 heure_depart TIME NOT NULL,
 heure_arrivee TIME NOT NULL,
 statut ENUM('PLANIFIEE','EN_COURS','TERMINEE','ANNULEE') NOT NULL DEFAULT 'PLANIFIEE',
 CONSTRAINT pk_affectation PRIMARY KEY (id_affectation),
 CONSTRAINT ck_affectation_heures CHECK (heure_arrivee > heure_depart),
 CONSTRAINT fk_affectation_trajet
 FOREIGN KEY (id_trajet) REFERENCES trajet (id_trajet)
 ON DELETE RESTRICT ON UPDATE CASCADE,
 CONSTRAINT fk_affectation_bus
 FOREIGN KEY (id_bus) REFERENCES bus (id_bus)
 ON DELETE RESTRICT ON UPDATE CASCADE,
 CONSTRAINT fk_affectation_chauffeur
 FOREIGN KEY (id_chauffeur) REFERENCES chauffeur (id_chauffeur)
 ON DELETE RESTRICT ON UPDATE CASCADE,
 INDEX idx_affectation_bus_date (id_bus, date_trajet),
 INDEX idx_affectation_chauffeur_date (id_chauffeur, date_trajet),
 INDEX idx_affectation_trajet_date (id_trajet, date_trajet)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Une inscription est liée à une AFFECTATION (donc à un trajet et à une date).
CREATE TABLE inscription (
 id_inscription INT UNSIGNED NOT NULL AUTO_INCREMENT,
 id_employe INT UNSIGNED NOT NULL,
 id_affectation INT UNSIGNED NOT NULL,
 date_inscription DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 statut ENUM('CONFIRMEE','ANNULEE') NOT NULL DEFAULT 'CONFIRMEE',
 CONSTRAINT pk_inscription PRIMARY KEY (id_inscription),
 CONSTRAINT uq_inscription_employe_affectation UNIQUE (id_employe, id_affectation),
 CONSTRAINT fk_inscription_employe
 FOREIGN KEY (id_employe) REFERENCES employe (id_employe)
 ON DELETE RESTRICT ON UPDATE CASCADE,
 CONSTRAINT fk_inscription_affectation
 FOREIGN KEY (id_affectation) REFERENCES affectation (id_affectation)
 ON DELETE RESTRICT ON UPDATE CASCADE,
 INDEX idx_inscription_affectation (id_affectation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Présence facultative au départ : la ligne n'existe qu'une fois la présence saisie.
-- UNIQUE sur id_inscription = au plus une présence par inscription : (0,1)-(1,1).
CREATE TABLE presence (
 id_presence INT UNSIGNED NOT NULL AUTO_INCREMENT,
 id_inscription INT UNSIGNED NOT NULL,
 statut ENUM('PRESENT','ABSENT','RETARD') NOT NULL,
 heure_montee TIME NULL,
 remarque VARCHAR(255) NULL,
 CONSTRAINT pk_presence PRIMARY KEY (id_presence),
 CONSTRAINT uq_presence_inscription UNIQUE (id_inscription),
 CONSTRAINT fk_presence_inscription
 FOREIGN KEY (id_inscription) REFERENCES inscription (id_inscription)
 ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- id_compte est facultatif : un incident peut être enregistré sans compte déclarant.
CREATE TABLE incident (
 id_incident INT UNSIGNED NOT NULL AUTO_INCREMENT,
 id_affectation INT UNSIGNED NOT NULL,
 id_compte INT UNSIGNED NULL,
 type ENUM('PANNE','ACCIDENT','RETARD','COMPORTEMENT','AUTRE') NOT NULL,
 gravite ENUM('MINEURE','MODEREE','GRAVE','CRITIQUE') NOT NULL,
 description TEXT NULL,
 date_incident DATETIME NOT NULL,
 statut ENUM('OUVERT','EN_COURS','RESOLU','CLOTURE') NOT NULL DEFAULT 'OUVERT',
 mode_declaration ENUM('APPLICATION','TELEPHONE','RAPPORT_PAPIER','SIGNALEMENT_CHAUFFEUR') NOT NULL,
 CONSTRAINT pk_incident PRIMARY KEY (id_incident),
 CONSTRAINT fk_incident_affectation
 FOREIGN KEY (id_affectation) REFERENCES affectation (id_affectation)
 ON DELETE RESTRICT ON UPDATE CASCADE,
 CONSTRAINT fk_incident_compte
 FOREIGN KEY (id_compte) REFERENCES compte_utilisateur (id_compte)
 ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- =====================================================================
-- 3. PROCÉDURES DE CONTRÔLE ET TRIGGERS (règles de gestion)
-- DELIMITER $$ permet d'écrire plusieurs instructions dans un même bloc.
-- Les messages d'erreur (SIGNAL) s'affichent tels quels dans Workbench
-- et remontent en SQLException dans Java.
-- =====================================================================
DELIMITER $$
-- ---------------------------------------------------------------------
-- 3.1 Contrôles d'une AFFECTATION (appelés par les triggers)
-- - bus DISPONIBLE
-- - bus non affecté sur un créneau qui se chevauche
-- - bus pas en maintenance planifiée ou en cours à cette date
-- - chauffeur ACTIF, employé du chauffeur actif, permis valide à la date
-- - chauffeur non affecté sur un créneau qui se chevauche
-- Deux créneaux se chevauchent si : début1 < fin2 ET début2 < fin1
-- (un bus qui arrive à 08:00 peut donc repartir à 08:00).
-- ---------------------------------------------------------------------
CREATE PROCEDURE sp_verifier_affectation(
 IN p_id_affectation INT UNSIGNED, -- 0 lors d'un INSERT
 IN p_id_bus INT UNSIGNED,
 IN p_id_chauffeur INT UNSIGNED,
 IN p_date_trajet DATE,
 IN p_heure_depart TIME,
  IN p_heure_arrivee TIME
)
BEGIN
 DECLARE v_statut_bus VARCHAR(20);
 DECLARE v_statut_chauffeur VARCHAR(20);
 DECLARE v_actif_employe TINYINT(1);
 DECLARE v_validite_permis DATE;
 -- Bus en maintenance ou hors service
 SELECT statut INTO v_statut_bus FROM bus WHERE id_bus = p_id_bus;
 IF v_statut_bus <> 'DISPONIBLE' THEN
 SIGNAL SQLSTATE '45000'
 SET MESSAGE_TEXT = 'Affectation refusée : le bus n''est pas DISPONIBLE (maintenance ou hors service).';
 END IF;
 -- Bus déjà pris sur un créneau qui se chevauche
 IF EXISTS (
 SELECT 1 FROM affectation a
 WHERE a.id_bus = p_id_bus
 AND a.date_trajet = p_date_trajet
 AND a.statut <> 'ANNULEE'
 AND a.id_affectation <> p_id_affectation
 AND a.heure_depart < p_heure_arrivee
 AND p_heure_depart < a.heure_arrivee
 ) THEN
 SIGNAL SQLSTATE '45000'
 SET MESSAGE_TEXT = 'Affectation refusée : ce bus est déjà affecté à un trajet qui se chevauche.';
 END IF;
 -- Bus en maintenance planifiée ou en cours à cette date
 IF EXISTS (
 SELECT 1 FROM maintenance m
 WHERE m.id_bus = p_id_bus
 AND m.statut IN ('PLANIFIEE','EN_COURS')
 AND p_date_trajet >= m.date_debut
 AND (m.date_fin IS NULL OR p_date_trajet <= m.date_fin)
 ) THEN
 SIGNAL SQLSTATE '45000'
 SET MESSAGE_TEXT = 'Affectation refusée : le bus est en maintenance (planifiée ou en cours) à cette date.';
 END IF;
-- Chauffeur : statut, employé actif, permis valide
 SELECT c.statut, e.actif, c.validite_permis
 INTO v_statut_chauffeur, v_actif_employe, v_validite_permis
 FROM chauffeur c
 JOIN employe e ON e.id_employe = c.id_employe
 WHERE c.id_chauffeur = p_id_chauffeur;
 IF v_statut_chauffeur <> 'ACTIF' THEN
 SIGNAL SQLSTATE '45000'
 SET MESSAGE_TEXT = 'Affectation refusée : le chauffeur n''est pas ACTIF.';
 END IF;
 IF v_actif_employe = 0 THEN
 SIGNAL SQLSTATE '45000'
 SET MESSAGE_TEXT = 'Affectation refusée : l''employé associé au chauffeur est désactivé.';
 END IF;
 IF v_validite_permis < p_date_trajet THEN
 SIGNAL SQLSTATE '45000'
 SET MESSAGE_TEXT = 'Affectation refusée : le permis du chauffeur est expiré à la date du trajet.';
 END IF;
 -- Chauffeur déjà pris sur un créneau qui se chevauche
 IF EXISTS (
 SELECT 1 FROM affectation a
 WHERE a.id_chauffeur = p_id_chauffeur
 AND a.date_trajet = p_date_trajet
 AND a.statut <> 'ANNULEE'
 AND a.id_affectation <> p_id_affectation
 AND a.heure_depart < p_heure_arrivee
 AND p_heure_depart < a.heure_arrivee
 ) THEN
 SIGNAL SQLSTATE '45000'
 SET MESSAGE_TEXT = 'Affectation refusée : ce chauffeur conduit déjà un trajet qui se chevauche.';
 END IF;
END$$
-- Les contrôles ne se font que pour les affectations non annulées.
-- À la modification, on ne recontrôle que si un élément pertinent change
-- (ou si l'affectation sort de l'état ANNULEE), afin de pouvoir encore
-- passer une ancienne affectation à TERMINEE même si le bus est
-- aujourd'hui en maintenance.
CREATE TRIGGER trg_affectation_bi
BEFORE INSERT ON affectation
FOR EACH ROW
BEGIN
 IF NEW.statut <> 'ANNULEE' THEN
 CALL sp_verifier_affectation(0, NEW.id_bus, NEW.id_chauffeur,
 NEW.date_trajet, NEW.heure_depart, NEW.heure_arrivee);
 END IF;
END$$
CREATE TRIGGER trg_affectation_bu
BEFORE UPDATE ON affectation
FOR EACH ROW
BEGIN
 DECLARE v_capacite SMALLINT UNSIGNED;
 DECLARE v_nb_inscrits INT;
 IF NEW.statut <> 'ANNULEE' THEN
 IF OLD.statut = 'ANNULEE'
 OR NEW.id_bus <> OLD.id_bus
 OR NEW.id_chauffeur <> OLD.id_chauffeur
 OR NEW.date_trajet <> OLD.date_trajet
 OR NEW.heure_depart <> OLD.heure_depart
 OR NEW.heure_arrivee <> OLD.heure_arrivee THEN
 CALL sp_verifier_affectation(OLD.id_affectation, NEW.id_bus, NEW.id_chauffeur,
 NEW.date_trajet, NEW.heure_depart, NEW.heure_arrivee);
 END IF;
 -- Changement de bus : le nouveau bus doit pouvoir accueillir les inscrits
 IF NEW.id_bus <> OLD.id_bus THEN
 SELECT capacite INTO v_capacite FROM bus WHERE id_bus = NEW.id_bus;
 SELECT COUNT(*) INTO v_nb_inscrits
 FROM inscription
 WHERE id_affectation = OLD.id_affectation AND statut = 'CONFIRMEE';
 IF v_nb_inscrits > v_capacite THEN
 SIGNAL SQLSTATE '45000'
 SET MESSAGE_TEXT = 'Changement de bus refusé : la capacité du nouveau bus est inférieure au nombre d''inscrits.';
 END IF;
 END IF;
 END IF;
END$$
-- ---------------------------------------------------------------------
-- 3.2 Contrôles d'une INSCRIPTION (appelés par les triggers)
-- - employé actif
-- - capacité du bus non dépassée
-- - pas d'inscription à deux affectations du même trajet le même jour
-- Le verrou FOR UPDATE sur l'affectation évite que deux inscriptions
-- simultanées dépassent ensemble la capacité.
-- ---------------------------------------------------------------------
CREATE PROCEDURE sp_verifier_inscription(
 IN p_id_inscription INT UNSIGNED, -- 0 lors d'un INSERT
 IN p_id_employe INT UNSIGNED,
 IN p_id_affectation INT UNSIGNED
)
BEGIN
 DECLARE v_actif TINYINT(1);
 DECLARE v_id_trajet INT UNSIGNED;
 DECLARE v_date_trajet DATE;
 DECLARE v_capacite SMALLINT UNSIGNED;
 DECLARE v_nb_inscrits INT;
 -- Employé désactivé
 SELECT actif INTO v_actif FROM employe WHERE id_employe = p_id_employe;
 IF v_actif = 0 THEN
 SIGNAL SQLSTATE '45000'
 SET MESSAGE_TEXT = 'Inscription refusée : l''employé est désactivé.';
 END IF;
 -- Infos de l'affectation et capacité du bus (ligne verrouillée)
 SELECT a.id_trajet, a.date_trajet, b.capacite
 INTO v_id_trajet, v_date_trajet, v_capacite
 FROM affectation a
 JOIN bus b ON b.id_bus = a.id_bus
 WHERE a.id_affectation = p_id_affectation
 FOR UPDATE;
 -- Capacité du bus
 SELECT COUNT(*) INTO v_nb_inscrits
 FROM inscription
 WHERE id_affectation = p_id_affectation
 AND statut = 'CONFIRMEE'
 AND id_inscription <> p_id_inscription;
 IF v_nb_inscrits >= v_capacite THEN
 SIGNAL SQLSTATE '45000'
 SET MESSAGE_TEXT = 'Inscription refusée : la capacité du bus est atteinte.';
 END IF;
 -- Même employé, même trajet, même date
 IF EXISTS (
 SELECT 1
 FROM inscription i
 JOIN affectation a ON a.id_affectation = i.id_affectation
 WHERE i.id_employe = p_id_employe
 AND i.statut = 'CONFIRMEE'
 AND i.id_inscription <> p_id_inscription
 AND a.statut <> 'ANNULEE'
 AND a.id_trajet = v_id_trajet
 AND a.date_trajet = v_date_trajet
 ) THEN
 SIGNAL SQLSTATE '45000'
 SET MESSAGE_TEXT = 'Inscription refusée : cet employé est déjà inscrit à ce trajet à cette date.';
 END IF;
 END$$
-- Seules les inscriptions CONFIRMEE sont contrôlées. À la modification,
-- on ne recontrôle que si l'inscription devient CONFIRMEE ou change
-- d'employé / d'affectation : l'historique d'un employé désactivé reste
-- donc modifiable sans erreur.
CREATE TRIGGER trg_inscription_bi
BEFORE INSERT ON inscription
FOR EACH ROW
BEGIN
 IF NEW.statut = 'CONFIRMEE' THEN
 CALL sp_verifier_inscription(0, NEW.id_employe, NEW.id_affectation);
 END IF;
END$$
CREATE TRIGGER trg_inscription_bu
BEFORE UPDATE ON inscription
FOR EACH ROW
BEGIN
 IF NEW.statut = 'CONFIRMEE'
 AND (OLD.statut <> 'CONFIRMEE'
 OR NEW.id_employe <> OLD.id_employe
 OR NEW.id_affectation <> OLD.id_affectation) THEN
 CALL sp_verifier_inscription(OLD.id_inscription, NEW.id_employe, NEW.id_affectation);
 END IF;
END$$
-- ---------------------------------------------------------------------
-- 3.3 Synchronisation du statut du bus avec la maintenance
-- - maintenance EN_COURS -> bus EN_MAINTENANCE (si le bus était DISPONIBLE)
-- - fin de la dernière maintenance EN_COURS -> bus DISPONIBLE
-- Un bus HORS_SERVICE n'est jamais modifié automatiquement.
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_maintenance_ai
AFTER INSERT ON maintenance
FOR EACH ROW
BEGIN
 IF NEW.statut = 'EN_COURS' THEN
 UPDATE bus SET statut = 'EN_MAINTENANCE'
 WHERE id_bus = NEW.id_bus AND statut = 'DISPONIBLE';
 END IF;
END$$
CREATE TRIGGER trg_maintenance_au
AFTER UPDATE ON maintenance
FOR EACH ROW
BEGIN
 IF NEW.statut = 'EN_COURS' AND OLD.statut <> 'EN_COURS' THEN
 UPDATE bus SET statut = 'EN_MAINTENANCE'
 WHERE id_bus = NEW.id_bus AND statut = 'DISPONIBLE';
 ELSEIF OLD.statut = 'EN_COURS' AND NEW.statut <> 'EN_COURS' THEN
 IF NOT EXISTS (
 SELECT 1 FROM maintenance
 WHERE id_bus = NEW.id_bus
 AND statut = 'EN_COURS'
 AND id_maintenance <> NEW.id_maintenance
 ) THEN
 UPDATE bus SET statut = 'DISPONIBLE'
 WHERE id_bus = NEW.id_bus AND statut = 'EN_MAINTENANCE';
 END IF;
 END IF;
END$$
DELIMITER ;
-- =====================================================================
-- 4. DONNÉES DE TEST
-- Ordre : tables sans dépendance d'abord, puis celles qui en dépendent.
-- Les affectations et inscriptions sont insérées AVANT les maintenances
-- pour que le bus 5 puisse passer EN_MAINTENANCE sans bloquer l'historique.
-- Les mots de passe ci-dessous sont des PLACEHOLDERS : ils ne correspondent
-- à aucun mot de passe réel. Les vrais hash (BCrypt) seront générés par
-- l'application Java.
-- =====================================================================
-- 4.1 Rôles
INSERT INTO role (id_role, nom, description) VALUES
(1, 'ADMIN', 'Administrateur : accès complet, gestion des comptes et des rôles'),
(2, 'GESTIONNAIRE', 'Gestionnaire transport : planning, bus, chauffeurs, affectations, rapports'),
(3, 'CHAUFFEUR', 'Chauffeur : consultation du planning, saisie des présences, déclaration d''incidents'),
(4, 'EMPLOYE', 'Employé : consultation du planning et inscription aux trajets');
-- 4.2 Permissions
INSERT INTO permission (id_permission, code, description) VALUES
(1, 'EMPLOYE_LIRE', 'Consulter la liste des employés'),
(2, 'EMPLOYE_GERER', 'Créer, modifier, désactiver des employés'),
(3, 'BUS_LIRE', 'Consulter les bus'),
(4, 'BUS_GERER', 'Créer et modifier les bus'),
(5, 'MAINTENANCE_GERER', 'Gérer les maintenances des bus'),
(6, 'CHAUFFEUR_GERER', 'Gérer les chauffeurs'),
(7, 'TRAJET_GERER', 'Gérer les trajets (itinéraires)'),
(8, 'AFFECTATION_GERER', 'Programmer et modifier les affectations'),
(9, 'PLANNING_CONSULTER', 'Consulter le planning'),
(10, 'INSCRIPTION_CREER', 'Sinscrire à un trajet programmé'),
(11, 'INSCRIPTION_GERER', 'Gérer les inscriptions de tous les employés'),
(12, 'PRESENCE_SAISIR', 'Saisir les présences'),
(13, 'INCIDENT_DECLARER', 'Déclarer un incident'),
(14, 'INCIDENT_GERER', 'Traiter et clôturer les incidents'),
(15, 'RAPPORT_CONSULTER', 'Consulter les statistiques et rapports'),
(16, 'COMPTE_GERER', 'Gérer les comptes utilisateurs, rôles et permissions');
-- 4.3 Permissions par rôle
INSERT INTO role_permission (id_role, id_permission) VALUES
(1, 1),
(1, 2),
(1, 3),
(1, 4),
(1, 5),
(1, 6),
(1, 7),
(1, 8),
(1, 9),
(1, 10),
(1, 11),
(1, 12),
(1, 13),
(1, 14),
(1, 15),
(1, 16),
(2, 1),
(2, 2),
(2, 3),
(2, 4),
(2, 5),
(2, 6),
(2, 7),
(2, 8),
(2, 9),
(2, 10),
(2, 11),
(2, 12),
(2, 13),
(2, 14),
(2, 15),
(3, 9),
(3, 12),
(3, 13),
(4, 9),
(4, 10),
(4, 13);
-- 4.4 Employés (Hassan Idrissi sera désactivé plus bas, après ses inscriptions)
INSERT INTO employe (id_employe, matricule, nom, prenom, email, telephone, adresse, departement, date_embauche, actif) VALUES
(1, 'EMP001', 'Benali', 'Karim', 'karim.benali@employgo.test', '0600000001', '12 rue des Acacias', 'Direction', '2015-03-02', 1),
(2, 'EMP002', 'Martin', 'Sophie', 'sophie.martin@employgo.test', '0600000002', '8 avenue de la Gare', 'Ressources humaines', '2017-09-11'),
(3, 'EMP003', 'Haddad', 'Youssef', 'youssef.haddad@employgo.test', '0600000003', '27 boulevard du Port', 'Transport', '2018-01-15', 1),
(4, 'EMP004', 'Dubois', 'Marc', 'marc.dubois@employgo.test', '0600000004', '3 impasse des Lilas', 'Transport', '2016-06-20', 1),
(5, 'EMP005', 'El Idrissi', 'Nadia', 'nadia.elidrissi@employgo.test', '0600000005', '45 rue Victor Hugo', 'Transport', '2019-04-08', 1),
(6, 'EMP006', 'Lambert', 'Pierre', 'pierre.lambert@employgo.test', '0600000006', '16 rue du Marché', 'Transport', '2014-11-03', 1),
(7, 'EMP007', 'Berrada', 'Amina', 'amina.berrada@employgo.test', '0600000007', '21 résidence Les Orangers', 'Comptabilité', '2020-02-17'),
(8, 'EMP008', 'Moreau', 'Julien', 'julien.moreau@employgo.test', '0600000008', '9 rue des Écoles', 'Informatique', '2021-05-03', 1),
(9, 'EMP009', 'Cherkaoui', 'Salma', 'salma.cherkaoui@employgo.test', '0600000009', '34 avenue Mohammed V', 'Production', '2019-10-14'), 
(10, 'EMP010', 'Leroy', 'Thomas', 'thomas.leroy@employgo.test', '0600000010', '5 allée des Pins', 'Logistique', '2022-01-10', 1),
(11, 'EMP011', 'Benjelloun', 'Lina', 'lina.benjelloun@employgo.test', '0600000011', '18 rue de la Paix', 'Qualité', '2020-09-01', 1),
(12, 'EMP012', 'Tazi', 'Omar', 'omar.tazi@employgo.test', '0600000012', '62 rue du Stade', 'Production', '2018-07-23', 1),
(13, 'EMP013', 'Petit', 'Camille', 'camille.petit@employgo.test', '0600000013', '11 chemin des Vignes', 'Commercial', '2023-03-06', 1),
(14, 'EMP014', 'Idrissi', 'Hassan', 'hassan.idrissi@employgo.test', '0600000014', '7 rue des Oliviers', 'Production', '2017-02-13', 1),
(15, 'EMP015', 'Fontaine', 'Léa', 'lea.fontaine@employgo.test', '0600000015', '29 rue Pasteur', 'Maintenance', '2021-11-22', 1),
(16, 'EMP016', 'Rachidi', 'Mehdi', 'mehdi.rachidi@employgo.test', '0600000016', '14 rue des Jardins', 'Transport', '2013-05-27', 1);
-- 4.5 Comptes utilisateurs (le compte 7 n'est lié à aucun employé)
INSERT INTO compte_utilisateur (id_compte, login, mot_de_passe_hash, actif, derniere_connexion, id_role, id_employe) VALUES
(1, 'karim.benali', '$2a$10$PLACEHOLDERplaceholderPLACEHOLDERplaceholderPLACEHOLDERplac', 1, '2026-11-06 08:11:00', 1, 1),
(2, 'sophie.martin', '$2a$10$PLACEHOLDERplaceholderPLACEHOLDERplaceholderPLACEHOLDERplac', 1, '2026-11-06 08:12:00', 2, 2),
(3, 'youssef.haddad', '$2a$10$PLACEHOLDERplaceholderPLACEHOLDERplaceholderPLACEHOLDERplac', 1, '2026-11-06 08:13:00', 3, 3),
(4, 'marc.dubois', '$2a$10$PLACEHOLDERplaceholderPLACEHOLDERplaceholderPLACEHOLDERplac', 1, '2026-11-06 08:14:00', 3, 4),
(5, 'amina.berrada', '$2a$10$PLACEHOLDERplaceholderPLACEHOLDERplaceholderPLACEHOLDERplac', 1, NULL, 4, 7),
(6, 'julien.moreau', '$2a$10$PLACEHOLDERplaceholderPLACEHOLDERplaceholderPLACEHOLDERplac', 1, NULL, 4, 8),
(7, 'admin.technique', '$2a$10$PLACEHOLDERplaceholderPLACEHOLDERplaceholderPLACEHOLDERplac', 1, NULL, 1, NULL);
-- 4.6 Chauffeurs (chaque chauffeur est un employé ; le chauffeur 5 a un permis expiré)
INSERT INTO chauffeur (id_chauffeur, id_employe, numero_permis, validite_permis, statut) VALUES
(1, 3, 'PERM-2019-0345', '2029-03-14', 'ACTIF'),
(2, 4, 'PERM-2016-1187', '2028-09-30', 'ACTIF'),
(3, 5, 'PERM-2020-2260', '2030-01-20', 'ACTIF'),
(4, 6, 'PERM-2014-0921', '2028-05-10', 'EN_CONGE'),
(5, 16, 'PERM-2013-0457', '2025-06-30', 'SUSPENDU');
-- 4.7 Bus (le bus 5 passera EN_MAINTENANCE via la maintenance en cours ; le bus 6 est hors service)
INSERT INTO bus (id_bus, immatriculation, marque, modele, capacite, statut) VALUES
(1, 'AB-123-CD', 'Mercedes-Benz', 'Tourismo', 50, 'DISPONIBLE'),
(2, 'EF-456-GH', 'Iveco', 'Crossway', 45, 'DISPONIBLE'),
(3, 'IJ-789-KL', 'Renault', 'Master minibus', 20, 'DISPONIBLE'),
(4, 'MN-012-OP', 'Mercedes-Benz', 'Sprinter', 18, 'DISPONIBLE'),
(5, 'QR-345-ST', 'Volvo', '9700', 55, 'DISPONIBLE'),
(6, 'UV-678-WX', 'Setra', 'S 315 HD', 49, 'HORS_SERVICE');
-- 4.8 Trajets (itinéraires, sans horaires)
INSERT INTO trajet (id_trajet, nom, lieu_depart, lieu_arrivee) VALUES
(1, 'Ligne 1 - Centre-ville vers Siège', 'Centre-ville', 'Siège social'),
(2, 'Ligne 2 - Gare vers Zone industrielle', 'Gare centrale', 'Zone industrielle'),
(3, 'Ligne 3 - Les Orangers vers Siège', 'Quartier Les Orangers', 'Siège social'),
(4, 'Ligne 4 - Les Palmiers vers Zone industrielle','Résidence Les Palmiers','Zone industrielle'),
(5, 'Ligne 5 - Retour du soir (Siège vers Gare)', 'Siège social', 'Gare centrale');
-- 4.9 Affectations : 5 trajets par jour sur 4 jours (2 jours passés TERMINEE, 2 jours à venir PLANIFIEE)
-- L'affectation 21 est ANNULEE : elle chevauche l'affectation 18 (même chauffeur)
-- mais reste autorisée, car les affectations annulées sont ignorées.
INSERT INTO affectation (id_affectation, id_trajet, id_bus, id_chauffeur, date_trajet, heure_depart, heure_arrivee, statut) VALUES
(1, 1, 1, 1, '2026-11-05', '07:00:00', '08:00:00', 'TERMINEE'),
(2, 2, 2, 2, '2026-11-05', '07:15:00', '08:15:00', 'TERMINEE'),
(3, 3, 3, 3, '2026-11-05', '07:30:00', '08:30:00', 'TERMINEE'),
(4, 5, 1, 1, '2026-11-05', '17:00:00', '18:00:00', 'TERMINEE'),
(5, 5, 2, 2, '2026-11-05', '17:15:00', '18:15:00', 'TERMINEE'),
(6, 1, 1, 1, '2026-11-06', '07:00:00', '08:00:00', 'TERMINEE'),
(7, 2, 2, 2, '2026-11-06', '07:15:00', '08:15:00', 'TERMINEE'),
(8, 3, 3, 3, '2026-11-06', '07:30:00', '08:30:00', 'TERMINEE'),
(9, 5, 1, 1, '2026-11-06', '17:00:00', '18:00:00', 'TERMINEE'),
(10, 5, 2, 2, '2026-11-06', '17:15:00', '18:15:00', 'TERMINEE'),
(11, 1, 1, 1, '2026-11-09', '07:00:00', '08:00:00', 'PLANIFIEE'),
(12, 2, 2, 2, '2026-11-09', '07:15:00', '08:15:00', 'PLANIFIEE'),
(13, 3, 3, 3, '2026-11-09', '07:30:00', '08:30:00', 'PLANIFIEE'),
(14, 5, 1, 1, '2026-11-09', '17:00:00', '18:00:00', 'PLANIFIEE'),
(15, 5, 2, 2, '2026-11-09', '17:15:00', '18:15:00', 'PLANIFIEE'),
(16, 1, 1, 1, '2026-11-10', '07:00:00', '08:00:00', 'PLANIFIEE'),
(17, 2, 2, 2, '2026-11-10', '07:15:00', '08:15:00', 'PLANIFIEE'),
(18, 3, 3, 3, '2026-11-10', '07:30:00', '08:30:00', 'PLANIFIEE'),
(19, 5, 1, 1, '2026-11-10', '17:00:00', '18:00:00', 'PLANIFIEE'),
(20, 5, 2, 2, '2026-11-10', '17:15:00', '18:15:00', 'PLANIFIEE'),
(21, 4, 4, 3, '2026-11-10', '07:30:00', '08:30:00', 'ANNULEE');
-- 4.10 Inscriptions (liées aux affectations). Quelques inscriptions annulées.
-- Hassan Idrissi (employé 14) est inscrit sur les jours passés avant d'être désactivé.
INSERT INTO inscription (id_inscription, id_employe, id_affectation, date_inscription, statut) VALUES
(1, 7, 1, '2026-11-03 08:03:00', 'CONFIRMEE'),
(2, 8, 1, '2026-11-03 08:06:00', 'CONFIRMEE'),
(3, 9, 1, '2026-11-03 08:09:00', 'CONFIRMEE'),
(4, 14, 1, '2026-11-03 08:12:00', 'CONFIRMEE'),
(5, 10, 2, '2026-11-03 08:15:00', 'CONFIRMEE'),
(6, 11, 2, '2026-11-03 08:18:00', 'CONFIRMEE'),
(7, 15, 2, '2026-11-03 08:21:00', 'CONFIRMEE'),
(8, 12, 3, '2026-11-03 08:24:00', 'CONFIRMEE'),
(9, 13, 3, '2026-11-03 08:27:00', 'CONFIRMEE'),
(10, 7, 4, '2026-11-03 08:30:00', 'CONFIRMEE'),
(11, 8, 4, '2026-11-03 08:33:00', 'CONFIRMEE'),
(12, 10, 4, '2026-11-03 08:36:00', 'CONFIRMEE'),
(13, 13, 4, '2026-11-03 08:39:00', 'ANNULEE'),
(14, 15, 4, '2026-11-03 08:42:00', 'CONFIRMEE'),
(15, 9, 5, '2026-11-03 08:45:00', 'CONFIRMEE'),
(16, 11, 5, '2026-11-03 08:48:00', 'CONFIRMEE'),
(17, 12, 5, '2026-11-03 08:51:00', 'CONFIRMEE'),
(18, 7, 6, '2026-11-04 08:54:00', 'CONFIRMEE'),
(19, 8, 6, '2026-11-04 08:57:00', 'CONFIRMEE'),
(20, 9, 6, '2026-11-04 08:00:00', 'CONFIRMEE'),
(21, 14, 6, '2026-11-04 08:03:00', 'CONFIRMEE'),
(22, 10, 7, '2026-11-04 08:06:00', 'CONFIRMEE'),
(23, 11, 7, '2026-11-04 08:09:00', 'CONFIRMEE'),
(24, 15, 7, '2026-11-04 08:12:00', 'CONFIRMEE'),
(25, 12, 8, '2026-11-04 08:15:00', 'CONFIRMEE'),
(26, 13, 8, '2026-11-04 08:18:00', 'CONFIRMEE'),
(27, 7, 9, '2026-11-04 08:21:00', 'CONFIRMEE'),
(28, 8, 9, '2026-11-04 08:24:00', 'CONFIRMEE'),
(29, 10, 9, '2026-11-04 08:27:00', 'CONFIRMEE'),
(30, 13, 9, '2026-11-04 08:30:00', 'CONFIRMEE'),
(31, 15, 9, '2026-11-04 08:33:00', 'CONFIRMEE'),
(32, 9, 10, '2026-11-04 08:36:00', 'CONFIRMEE'),
(33, 11, 10, '2026-11-04 08:39:00', 'CONFIRMEE'),
(34, 12, 10, '2026-11-04 08:42:00', 'CONFIRMEE'),
(35, 7, 11, '2026-11-07 08:45:00', 'CONFIRMEE'),
(36, 8, 11, '2026-11-07 08:48:00', 'CONFIRMEE'),
(37, 9, 11, '2026-11-07 08:51:00', 'CONFIRMEE'),
(38, 10, 12, '2026-11-07 08:54:00', 'CONFIRMEE'),
(39, 11, 12, '2026-11-07 08:57:00', 'CONFIRMEE'),
(40, 15, 12, '2026-11-07 08:00:00', 'ANNULEE'),
(41, 12, 13, '2026-11-07 08:03:00', 'CONFIRMEE'),
(42, 13, 13, '2026-11-07 08:06:00', 'CONFIRMEE'),
(43, 7, 14, '2026-11-07 08:09:00', 'CONFIRMEE'),
(44, 8, 14, '2026-11-07 08:12:00', 'CONFIRMEE'),
(45, 10, 14, '2026-11-07 08:15:00', 'CONFIRMEE'),
(46, 13, 14, '2026-11-07 08:18:00', 'CONFIRMEE'),
(47, 15, 14, '2026-11-07 08:21:00', 'CONFIRMEE'),
(48, 9, 15, '2026-11-07 08:24:00', 'CONFIRMEE'),
(49, 11, 15, '2026-11-07 08:27:00', 'CONFIRMEE'),
(50, 12, 15, '2026-11-07 08:30:00', 'CONFIRMEE'),
(51, 7, 16, '2026-11-08 08:33:00', 'CONFIRMEE'),
(52, 8, 16, '2026-11-08 08:36:00', 'CONFIRMEE'),
(53, 9, 16, '2026-11-08 08:39:00', 'CONFIRMEE'),
(54, 10, 17, '2026-11-08 08:42:00', 'CONFIRMEE'),
(55, 11, 17, '2026-11-08 08:45:00', 'CONFIRMEE'),
(56, 15, 17, '2026-11-08 08:48:00', 'CONFIRMEE'),
(57, 12, 18, '2026-11-08 08:51:00', 'CONFIRMEE'),
(58, 13, 18, '2026-11-08 08:54:00', 'CONFIRMEE'),
(59, 7, 19, '2026-11-08 08:57:00', 'CONFIRMEE'),
(60, 8, 19, '2026-11-08 08:00:00', 'CONFIRMEE'),
(61, 10, 19, '2026-11-08 08:03:00', 'CONFIRMEE'),
(62, 13, 19, '2026-11-08 08:06:00', 'CONFIRMEE'),
(63, 15, 19, '2026-11-08 08:09:00', 'CONFIRMEE'),
(64, 9, 20, '2026-11-08 08:12:00', 'CONFIRMEE'),
(65, 11, 20, '2026-11-08 08:15:00', 'CONFIRMEE'),
(66, 12, 20, '2026-11-08 08:18:00', 'CONFIRMEE');
-- 4.11 Présences : saisies uniquement pour les jours passés.
-- Les inscriptions des jours à venir n'ont volontairement pas encore de présence.
INSERT INTO presence (id_presence, id_inscription, statut, heure_montee, remarque) VALUES
(1, 1, 'PRESENT', '07:00:00', NULL),
(2, 2, 'PRESENT', '07:00:00', NULL),
(3, 3, 'PRESENT', '07:00:00', NULL),
(4, 4, 'PRESENT', '07:00:00', NULL),
(5, 5, 'PRESENT', '07:10:00', NULL),
(6, 6, 'PRESENT', '07:10:00', NULL),
(7, 7, 'RETARD', '07:23:00', 'Monté au dernier arrêt'),
(8, 8, 'PRESENT', '07:25:00', NULL),
(9, 9, 'PRESENT', '07:25:00', NULL),
(10, 10, 'PRESENT', '17:00:00', NULL),
(11, 11, 'ABSENT', NULL, 'Absence non justifiée'),
(12, 12, 'PRESENT', '17:00:00', NULL),
(13, 14, 'PRESENT', '17:00:00', NULL),
(14, 15, 'RETARD', '17:23:00', 'Monté au dernier arrêt'),
(15, 16, 'PRESENT', '17:10:00', NULL),
(16, 17, 'PRESENT', '17:10:00', NULL),
(17, 18, 'PRESENT', '07:00:00', NULL),
(18, 19, 'PRESENT', '07:00:00', NULL),
(19, 20, 'PRESENT', '07:00:00', NULL),
(20, 21, 'PRESENT', '07:00:00', NULL),
(21, 22, 'RETARD', '07:23:00', 'Monté au dernier arrêt'),
(22, 23, 'ABSENT', NULL, 'Absence non justifiée'),
(23, 24, 'PRESENT', '07:10:00', NULL),
(24, 25, 'PRESENT', '07:25:00', NULL),
(25, 26, 'PRESENT', '07:25:00', NULL),
(26, 27, 'PRESENT', '17:00:00', NULL),
(27, 28, 'PRESENT', '17:00:00', NULL),
(28, 29, 'RETARD', '17:08:00', 'Monté au dernier arrêt'),
(29, 30, 'PRESENT', '17:00:00', NULL),
(30, 31, 'PRESENT', '17:00:00', NULL),
(31, 32, 'PRESENT', '17:10:00', NULL),
(32, 33, 'PRESENT', '17:10:00', NULL),
(33, 34, 'ABSENT', NULL, 'Absence non justifiée');
-- 4.12 Désactivation de Hassan Idrissi : ses anciennes inscriptions et présences restent en base
UPDATE employe SET actif = 0 WHERE id_employe = 14;
-- 4.13 Maintenances (la maintenance EN_COURS du bus 5 le passe automatiquement EN_MAINTENANCE)
INSERT INTO maintenance (id_maintenance, id_bus, type, date_debut, date_fin, statut) VALUES
(1, 5, 'CORRECTIVE', '2026-11-03', NULL, 'EN_COURS'),
(2, 1, 'PREVENTIVE', '2026-09-14', '2026-09-15', 'TERMINEE'),
(3, 3, 'PREVENTIVE', '2026-11-20', '2026-11-22', 'PLANIFIEE'),
(4, 2, 'CORRECTIVE', '2026-08-18', '2026-08-19', 'TERMINEE');
-- 4.14 Incidents : les quatre modes de déclaration, avec et sans compte déclarant
INSERT INTO incident (id_incident, id_affectation, id_compte, type, gravite, description, date_incident, statut, mode_declaration) 
VALUE(1, 1, 5, 'RETARD', 'MINEURE', 'Retard d''environ 10 minutes dû à la circulation.', '2026-11-05 07:40:00'), 
(2, 3, NULL, 'PANNE', 'MODEREE', 'Voyant moteur allumé, vérification demandée à l''atelier.', '2026-11-05 07:55:00'), 
(3, 7, 3, 'COMPORTEMENT', 'MINEURE', 'Passager bruyant, rappel des règles effectué par le chauffeur.', '2026-11-06 07:30:00'), 
(4, 9, NULL, 'AUTRE', 'MINEURE', 'Siège endommagé signalé sur formulaire papier.', '2026-11-06 17:20:00'), 
(5, 8, 2, 'ACCIDENT', 'GRAVE', 'Accrochage léger au rond-point, constat amiable établi.', '2026-11-06 08:05:00') 