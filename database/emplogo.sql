-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : lun. 09 nov. 2026 à 12:36
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `emplogo`
--

DELIMITER $$
--
-- Procédures
--
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_controle_affectation` (IN `p_id_affectation` INT UNSIGNED, IN `p_id_bus` INT UNSIGNED, IN `p_id_chauffeur` INT UNSIGNED, IN `p_date_trajet` DATE, IN `p_heure_depart` TIME, IN `p_heure_arrivee` TIME)   BEGIN
    DECLARE v_statut_bus       VARCHAR(20) DEFAULT NULL;
    DECLARE v_statut_chauffeur VARCHAR(20) DEFAULT NULL;
    DECLARE v_validite_permis  DATE        DEFAULT NULL;
    DECLARE v_employe_actif    TINYINT     DEFAULT NULL;
    DECLARE v_nb               INT         DEFAULT 0;

    -- Bus : existe et DISPONIBLE ?
    SET v_statut_bus = (SELECT statut FROM bus WHERE id_bus = p_id_bus);
    IF v_statut_bus IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50000, MESSAGE_TEXT = 'Affectation refusée : bus introuvable.';
    END IF;
    IF v_statut_bus <> 'DISPONIBLE' THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50001,
                MESSAGE_TEXT = 'Affectation refusée : le bus n''est pas DISPONIBLE (maintenance ou hors service).';
    END IF;

    -- Bus : pas de maintenance planifiée ou en cours à la date du trajet
    SELECT COUNT(*) INTO v_nb
      FROM maintenance m
     WHERE m.id_bus = p_id_bus
       AND m.statut IN ('PLANIFIEE', 'EN_COURS')
       AND p_date_trajet >= m.date_debut
       AND p_date_trajet <= COALESCE(m.date_fin, '9999-12-31');
    IF v_nb > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50002,
                MESSAGE_TEXT = 'Affectation refusée : le bus est en maintenance à la date du trajet.';
    END IF;

    -- Chauffeur : existe, ACTIF, employé actif, permis valide à la date du trajet
    SET v_statut_chauffeur = (SELECT statut FROM chauffeur WHERE id_chauffeur = p_id_chauffeur);
    SET v_validite_permis  = (SELECT date_validite_permis FROM chauffeur WHERE id_chauffeur = p_id_chauffeur);
    SET v_employe_actif    = (SELECT e.actif
                                FROM chauffeur c
                                JOIN employe e ON e.id_employe = c.id_employe
                               WHERE c.id_chauffeur = p_id_chauffeur);
    IF v_statut_chauffeur IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50000, MESSAGE_TEXT = 'Affectation refusée : chauffeur introuvable.';
    END IF;
    IF v_statut_chauffeur <> 'ACTIF' THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50005, MESSAGE_TEXT = 'Affectation refusée : le chauffeur n''est pas ACTIF.';
    END IF;
    IF v_employe_actif <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50006,
                MESSAGE_TEXT = 'Affectation refusée : l''employé associé au chauffeur est désactivé.';
    END IF;
    IF v_validite_permis < p_date_trajet THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50007,
                MESSAGE_TEXT = 'Affectation refusée : permis de conduire expiré à la date du trajet.';
    END IF;

    -- Chevauchement pour le bus (les affectations ANNULEE sont ignorées).
    -- Deux plages se chevauchent si : début1 < fin2 ET fin1 > début2.
    SELECT COUNT(*) INTO v_nb
      FROM affectation a
     WHERE a.id_bus = p_id_bus
       AND a.id_affectation <> p_id_affectation
       AND a.statut <> 'ANNULEE'
       AND a.date_trajet = p_date_trajet
       AND a.heure_depart  < p_heure_arrivee
       AND a.heure_arrivee > p_heure_depart;
    IF v_nb > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50003,
                MESSAGE_TEXT = 'Affectation refusée : ce bus a déjà une affectation qui chevauche cet horaire.';
    END IF;

    -- Chevauchement pour le chauffeur
    SELECT COUNT(*) INTO v_nb
      FROM affectation a
     WHERE a.id_chauffeur = p_id_chauffeur
       AND a.id_affectation <> p_id_affectation
       AND a.statut <> 'ANNULEE'
       AND a.date_trajet = p_date_trajet
       AND a.heure_depart  < p_heure_arrivee
       AND a.heure_arrivee > p_heure_depart;
    IF v_nb > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50004,
                MESSAGE_TEXT = 'Affectation refusée : ce chauffeur a déjà une affectation qui chevauche cet horaire.';
    END IF;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_controle_inscription` (IN `p_id_inscription` INT UNSIGNED, IN `p_id_employe` INT UNSIGNED, IN `p_id_affectation` INT UNSIGNED)   BEGIN
    DECLARE v_actif       TINYINT      DEFAULT NULL;
    DECLARE v_statut_aff  VARCHAR(20)  DEFAULT NULL;
    DECLARE v_id_trajet   INT UNSIGNED DEFAULT NULL;
    DECLARE v_date_trajet DATE         DEFAULT NULL;
    DECLARE v_capacite    INT          DEFAULT NULL;
    DECLARE v_nb          INT          DEFAULT 0;

    -- Employé : existe et actif ?
    SET v_actif = (SELECT actif FROM employe WHERE id_employe = p_id_employe);
    IF v_actif IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50000, MESSAGE_TEXT = 'Inscription refusée : employé introuvable.';
    END IF;
    IF v_actif <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50008, MESSAGE_TEXT = 'Inscription refusée : l''employé est désactivé.';
    END IF;

    -- Affectation : on la verrouille (FOR UPDATE) pour que deux inscriptions
    -- simultanées ne puissent pas dépasser la capacité en même temps.
    SELECT a.statut, a.id_trajet, a.date_trajet, b.capacite
      INTO v_statut_aff, v_id_trajet, v_date_trajet, v_capacite
      FROM affectation a
      JOIN bus b ON b.id_bus = a.id_bus
     WHERE a.id_affectation = p_id_affectation
       FOR UPDATE;
    IF v_statut_aff IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50000, MESSAGE_TEXT = 'Inscription refusée : affectation introuvable.';
    END IF;
    IF v_statut_aff = 'ANNULEE' THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50011, MESSAGE_TEXT = 'Inscription refusée : cette affectation est annulée.';
    END IF;

    -- Capacité du bus
    SELECT COUNT(*) INTO v_nb
      FROM inscription i
     WHERE i.id_affectation = p_id_affectation
       AND i.statut = 'CONFIRMEE'
       AND i.id_inscription <> p_id_inscription;
    IF v_nb >= v_capacite THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50009, MESSAGE_TEXT = 'Inscription refusée : la capacité du bus est atteinte.';
    END IF;

    -- Pas deux inscriptions CONFIRMEE au même trajet à la même date
    SELECT COUNT(*) INTO v_nb
      FROM inscription i
      JOIN affectation a ON a.id_affectation = i.id_affectation
     WHERE i.id_employe = p_id_employe
       AND i.statut = 'CONFIRMEE'
       AND i.id_inscription <> p_id_inscription
       AND a.id_trajet = v_id_trajet
       AND a.date_trajet = v_date_trajet
       AND a.statut <> 'ANNULEE';
    IF v_nb > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50010,
                MESSAGE_TEXT = 'Inscription refusée : l''employé est déjà inscrit à ce trajet à cette date.';
    END IF;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Structure de la table `affectation`
--

CREATE TABLE `affectation` (
  `id_affectation` int(10) UNSIGNED NOT NULL,
  `id_trajet` int(10) UNSIGNED NOT NULL,
  `id_bus` int(10) UNSIGNED NOT NULL,
  `id_chauffeur` int(10) UNSIGNED NOT NULL,
  `date_trajet` date NOT NULL,
  `heure_depart` time NOT NULL,
  `heure_arrivee` time NOT NULL,
  `statut` enum('PLANIFIEE','EN_COURS','TERMINEE','ANNULEE') NOT NULL DEFAULT 'PLANIFIEE'
) ;

--
-- Déchargement des données de la table `affectation`
--

INSERT INTO `affectation` (`id_affectation`, `id_trajet`, `id_bus`, `id_chauffeur`, `date_trajet`, `heure_depart`, `heure_arrivee`, `statut`) VALUES
(1, 1, 1, 1, '2026-11-02', '07:00:00', '07:45:00', 'TERMINEE'),
(2, 4, 1, 1, '2026-11-02', '17:00:00', '17:45:00', 'TERMINEE'),
(3, 2, 2, 2, '2026-11-02', '07:15:00', '08:00:00', 'TERMINEE'),
(4, 1, 1, 1, '2026-11-03', '07:00:00', '07:45:00', 'TERMINEE'),
(5, 1, 1, 1, '2026-11-09', '07:00:00', '07:45:00', 'PLANIFIEE'),
(6, 4, 1, 1, '2026-11-09', '17:00:00', '17:45:00', 'PLANIFIEE'),
(7, 2, 2, 2, '2026-11-09', '07:15:00', '08:00:00', 'PLANIFIEE'),
(8, 3, 3, 3, '2026-11-09', '07:30:00', '08:15:00', 'PLANIFIEE'),
(9, 1, 1, 1, '2026-11-10', '07:00:00', '07:45:00', 'PLANIFIEE'),
(10, 3, 3, 3, '2026-11-10', '07:30:00', '08:15:00', 'PLANIFIEE'),
(11, 2, 2, 2, '2026-11-10', '07:15:00', '08:00:00', 'ANNULEE'),
(12, 2, 2, 2, '2026-11-10', '07:15:00', '08:00:00', 'PLANIFIEE');

--
-- Déclencheurs `affectation`
--
DELIMITER $$
CREATE TRIGGER `trg_affectation_bi` BEFORE INSERT ON `affectation` FOR EACH ROW BEGIN
    IF NEW.statut <> 'ANNULEE' THEN
        CALL sp_controle_affectation(0, NEW.id_bus, NEW.id_chauffeur,
                                     NEW.date_trajet, NEW.heure_depart, NEW.heure_arrivee);
    END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_affectation_bu` BEFORE UPDATE ON `affectation` FOR EACH ROW BEGIN
    DECLARE v_nb       INT DEFAULT 0;
    DECLARE v_capacite INT DEFAULT NULL;

    IF NEW.statut <> 'ANNULEE' THEN

        -- On refait les contrôles seulement si quelque chose d'important change
        -- (ainsi, passer une affectation passée à TERMINEE n'est jamais bloqué
        --  par l'état ACTUEL du bus).
        IF OLD.statut = 'ANNULEE'
           OR NOT (OLD.id_bus        <=> NEW.id_bus)
           OR NOT (OLD.id_chauffeur  <=> NEW.id_chauffeur)
           OR NOT (OLD.date_trajet   <=> NEW.date_trajet)
           OR NOT (OLD.heure_depart  <=> NEW.heure_depart)
           OR NOT (OLD.heure_arrivee <=> NEW.heure_arrivee) THEN
            CALL sp_controle_affectation(NEW.id_affectation, NEW.id_bus, NEW.id_chauffeur,
                                         NEW.date_trajet, NEW.heure_depart, NEW.heure_arrivee);
        END IF;

        -- Si on change de bus : le nouveau bus doit pouvoir accueillir
        -- les inscriptions déjà confirmées.
        IF NOT (OLD.id_bus <=> NEW.id_bus) THEN
            SET v_nb = (SELECT COUNT(*) FROM inscription
                         WHERE id_affectation = NEW.id_affectation AND statut = 'CONFIRMEE');
            SET v_capacite = (SELECT capacite FROM bus WHERE id_bus = NEW.id_bus);
            IF v_nb > v_capacite THEN
                SIGNAL SQLSTATE '45000'
                    SET MYSQL_ERRNO = 50012,
                        MESSAGE_TEXT = 'Changement de bus refusé : capacité insuffisante pour les inscriptions existantes.';
            END IF;
        END IF;
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Structure de la table `bus`
--

CREATE TABLE `bus` (
  `id_bus` int(10) UNSIGNED NOT NULL,
  `immatriculation` varchar(20) NOT NULL,
  `marque` varchar(50) DEFAULT NULL,
  `modele` varchar(50) DEFAULT NULL,
  `capacite` smallint(5) UNSIGNED NOT NULL,
  `statut` enum('DISPONIBLE','EN_MAINTENANCE','HORS_SERVICE') NOT NULL DEFAULT 'DISPONIBLE'
) ;

--
-- Déchargement des données de la table `bus`
--

INSERT INTO `bus` (`id_bus`, `immatriculation`, `marque`, `modele`, `capacite`, `statut`) VALUES
(1, '48215-A-6', 'Mercedes-Benz', 'Tourismo', 50, 'DISPONIBLE'),
(2, '48216-A-6', 'Iveco', 'Crossway', 45, 'DISPONIBLE'),
(3, '51732-B-6', 'Mercedes-Benz', 'Sprinter Minibus', 20, 'DISPONIBLE'),
(4, '37458-D-6', 'Volvo', '9700', 49, 'EN_MAINTENANCE'),
(5, '29874-E-6', 'Renault', 'Master Minibus', 16, 'HORS_SERVICE');

-- --------------------------------------------------------

--
-- Structure de la table `chauffeur`
--

CREATE TABLE `chauffeur` (
  `id_chauffeur` int(10) UNSIGNED NOT NULL,
  `id_employe` int(10) UNSIGNED NOT NULL,
  `numero_permis` varchar(30) NOT NULL,
  `date_validite_permis` date NOT NULL,
  `statut` enum('ACTIF','EN_CONGE','SUSPENDU','INACTIF') NOT NULL DEFAULT 'ACTIF'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `chauffeur`
--

INSERT INTO `chauffeur` (`id_chauffeur`, `id_employe`, `numero_permis`, `date_validite_permis`, `statut`) VALUES
(1, 1, 'D-123456', '2029-05-31', 'ACTIF'),
(2, 2, 'D-234567', '2028-11-30', 'ACTIF'),
(3, 3, 'D-345678', '2027-08-15', 'ACTIF'),
(4, 4, 'D-456789', '2026-03-15', 'ACTIF');

-- --------------------------------------------------------

--
-- Structure de la table `compte_utilisateur`
--

CREATE TABLE `compte_utilisateur` (
  `id_compte` int(10) UNSIGNED NOT NULL,
  `id_role` int(10) UNSIGNED NOT NULL,
  `id_employe` int(10) UNSIGNED DEFAULT NULL,
  `login` varchar(50) NOT NULL,
  `mot_de_passe_hash` varchar(255) NOT NULL,
  `actif` tinyint(1) NOT NULL DEFAULT 1,
  `derniere_connexion` datetime DEFAULT NULL
) ;

--
-- Déchargement des données de la table `compte_utilisateur`
--

INSERT INTO `compte_utilisateur` (`id_compte`, `id_role`, `id_employe`, `login`, `mot_de_passe_hash`, `actif`, `derniere_connexion`) VALUES
(1, 1, NULL, 'admin', '$2a$10$PLACEHOLDER.A.REMPLACER.PAR.UN.VRAI.HASH.BCRYPT.0000000000', 1, '2026-11-06 08:30:00'),
(2, 2, 5, 's.amrani', '$2a$10$PLACEHOLDER.A.REMPLACER.PAR.UN.VRAI.HASH.BCRYPT.0000000000', 1, '2026-11-06 09:05:00'),
(3, 3, 1, 'y.alaoui', '$2a$10$PLACEHOLDER.A.REMPLACER.PAR.UN.VRAI.HASH.BCRYPT.0000000000', 1, '2026-11-05 06:40:00'),
(4, 3, 2, 'k.benani', '$2a$10$PLACEHOLDER.A.REMPLACER.PAR.UN.VRAI.HASH.BCRYPT.0000000000', 1, '2026-11-05 06:55:00'),
(5, 4, 6, 'n.elfassi', '$2a$10$PLACEHOLDER.A.REMPLACER.PAR.UN.VRAI.HASH.BCRYPT.0000000000', 1, '2026-11-04 18:12:00'),
(6, 4, 7, 'o.bennis', '$2a$10$PLACEHOLDER.A.REMPLACER.PAR.UN.VRAI.HASH.BCRYPT.0000000000', 1, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `employe`
--

CREATE TABLE `employe` (
  `id_employe` int(10) UNSIGNED NOT NULL,
  `matricule` varchar(20) NOT NULL,
  `nom` varchar(80) NOT NULL,
  `prenom` varchar(80) NOT NULL,
  `email` varchar(150) NOT NULL,
  `telephone` varchar(20) DEFAULT NULL,
  `adresse` varchar(255) DEFAULT NULL,
  `departement` varchar(100) DEFAULT NULL,
  `date_embauche` date DEFAULT NULL,
  `actif` tinyint(1) NOT NULL DEFAULT 1
) ;

--
-- Déchargement des données de la table `employe`
--

INSERT INTO `employe` (`id_employe`, `matricule`, `nom`, `prenom`, `email`, `telephone`, `adresse`, `departement`, `date_embauche`, `actif`) VALUES
(1, 'E001', 'Alaoui', 'Youssef', 'youssef.alaoui@exemple.ma', '0661000001', 'Hay Mohammadi, Agadir', 'Transport', '2019-03-04', 1),
(2, 'E002', 'Benani', 'Karim', 'karim.benani@exemple.ma', '0661000002', 'Founty, Agadir', 'Transport', '2020-09-14', 1),
(3, 'E003', 'Chraibi', 'Hamid', 'hamid.chraibi@exemple.ma', '0661000003', 'Anza, Agadir', 'Transport', '2018-01-22', 1),
(4, 'E004', 'Idrissi', 'Mohamed', 'mohamed.idrissi@exemple.ma', '0661000004', 'Inezgane', 'Transport', '2017-06-05', 1),
(5, 'E005', 'Amrani', 'Salma', 'salma.amrani@exemple.ma', '0661000005', 'Centre-ville, Agadir', 'Ressources humaines', '2016-02-01', 1),
(6, 'E006', 'El Fassi', 'Nadia', 'nadia.elfassi@exemple.ma', '0661000006', 'Dcheira El Jihadia', 'Production', '2021-04-12', 1),
(7, 'E007', 'Bennis', 'Omar', 'omar.bennis@exemple.ma', '0661000007', 'Inezgane', 'Production', '2022-01-10', 1),
(8, 'E008', 'Tazi', 'Laila', 'laila.tazi@exemple.ma', '0661000008', 'Talborjt, Agadir', 'Comptabilité', '2020-11-23', 1),
(9, 'E009', 'Lahlou', 'Rachid', 'rachid.lahlou@exemple.ma', '0661000009', 'Ait Melloul', 'Maintenance industrielle', '2019-09-02', 1),
(10, 'E010', 'Berrada', 'Houda', 'houda.berrada@exemple.ma', '0661000010', 'Inezgane', 'Qualité', '2023-02-06', 1),
(11, 'E011', 'Saidi', 'Hicham', 'hicham.saidi@exemple.ma', '0661000011', 'Dcheira El Jihadia', 'Logistique', '2021-10-18', 1),
(12, 'E012', 'Zerouali', 'Amina', 'amina.zerouali@exemple.ma', '0661000012', 'Hay Salam, Agadir', 'Production', '2018-05-14', 0),
(13, '12', 'sara', 'serbout', 'sara@gmail.com', '06633389237', 'taroudant', 'it', '2026-11-10', 1);

-- --------------------------------------------------------

--
-- Structure de la table `incident`
--

CREATE TABLE `incident` (
  `id_incident` int(10) UNSIGNED NOT NULL,
  `id_affectation` int(10) UNSIGNED NOT NULL,
  `id_compte` int(10) UNSIGNED DEFAULT NULL,
  `type_incident` enum('PANNE','ACCIDENT','RETARD','COMPORTEMENT','AUTRE') NOT NULL,
  `gravite` enum('FAIBLE','MOYENNE','ELEVEE','CRITIQUE') NOT NULL,
  `description` text DEFAULT NULL,
  `date_incident` datetime NOT NULL,
  `mode_declaration` enum('APPLICATION','TELEPHONE','EMAIL','SUR_PLACE') NOT NULL DEFAULT 'APPLICATION',
  `statut_traitement` enum('OUVERT','EN_COURS','RESOLU','CLOS') NOT NULL DEFAULT 'OUVERT'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `incident`
--

INSERT INTO `incident` (`id_incident`, `id_affectation`, `id_compte`, `type_incident`, `gravite`, `description`, `date_incident`, `mode_declaration`, `statut_traitement`) VALUES
(1, 3, 4, 'RETARD', 'MOYENNE', 'Embouteillage important vers Inezgane, retard d\'environ 20 minutes.', '2026-11-02 07:40:00', 'APPLICATION', 'RESOLU'),
(2, 2, 3, 'PANNE', 'FAIBLE', 'Climatisation défaillante pendant le trajet retour.', '2026-11-02 17:20:00', 'APPLICATION', 'OUVERT'),
(3, 4, NULL, 'COMPORTEMENT', 'FAIBLE', 'Signalement d\'un passager debout dans l\'allée pendant le trajet.', '2026-11-03 07:20:00', 'TELEPHONE', 'EN_COURS'),
(4, 1, 2, 'ACCIDENT', 'ELEVEE', 'Léger accrochage au rond-point, constat amiable établi.', '2026-11-02 07:25:00', 'SUR_PLACE', 'EN_COURS'),
(5, 4, 2, 'AUTRE', 'FAIBLE', 'Objet oublié dans le bus, signalé par courriel.', '2026-11-03 09:10:00', 'EMAIL', 'CLOS');

-- --------------------------------------------------------

--
-- Structure de la table `inscription`
--

CREATE TABLE `inscription` (
  `id_inscription` int(10) UNSIGNED NOT NULL,
  `id_employe` int(10) UNSIGNED NOT NULL,
  `id_affectation` int(10) UNSIGNED NOT NULL,
  `date_inscription` datetime NOT NULL DEFAULT current_timestamp(),
  `statut` enum('CONFIRMEE','ANNULEE') NOT NULL DEFAULT 'CONFIRMEE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `inscription`
--

INSERT INTO `inscription` (`id_inscription`, `id_employe`, `id_affectation`, `date_inscription`, `statut`) VALUES
(1, 6, 1, '2026-10-30 09:12:00', 'CONFIRMEE'),
(2, 7, 1, '2026-10-30 09:20:00', 'CONFIRMEE'),
(3, 8, 1, '2026-10-30 10:02:00', 'CONFIRMEE'),
(4, 9, 1, '2026-10-30 11:45:00', 'CONFIRMEE'),
(5, 12, 1, '2026-10-30 14:30:00', 'CONFIRMEE'),
(6, 6, 2, '2026-10-30 09:12:00', 'CONFIRMEE'),
(7, 7, 2, '2026-10-30 09:20:00', 'CONFIRMEE'),
(8, 10, 3, '2026-10-30 08:50:00', 'CONFIRMEE'),
(9, 11, 3, '2026-10-30 08:55:00', 'CONFIRMEE'),
(10, 12, 3, '2026-10-30 14:31:00', 'CONFIRMEE'),
(11, 6, 4, '2026-10-31 09:00:00', 'CONFIRMEE'),
(12, 7, 4, '2026-10-31 09:05:00', 'CONFIRMEE'),
(13, 8, 4, '2026-10-31 09:30:00', 'CONFIRMEE'),
(14, 6, 5, '2026-11-05 08:10:00', 'CONFIRMEE'),
(15, 7, 5, '2026-11-05 08:15:00', 'CONFIRMEE'),
(16, 8, 5, '2026-11-05 09:00:00', 'CONFIRMEE'),
(17, 9, 5, '2026-11-05 09:40:00', 'CONFIRMEE'),
(18, 6, 6, '2026-11-05 08:10:00', 'CONFIRMEE'),
(19, 7, 6, '2026-11-05 08:15:00', 'CONFIRMEE'),
(20, 10, 7, '2026-11-05 10:20:00', 'CONFIRMEE'),
(21, 11, 7, '2026-11-05 10:25:00', 'CONFIRMEE'),
(22, 5, 8, '2026-11-06 08:00:00', 'CONFIRMEE'),
(23, 6, 9, '2026-11-06 08:30:00', 'CONFIRMEE'),
(24, 7, 9, '2026-11-06 08:35:00', 'CONFIRMEE'),
(25, 8, 9, '2026-11-06 09:10:00', 'CONFIRMEE'),
(26, 9, 9, '2026-11-06 09:50:00', 'ANNULEE'),
(27, 10, 10, '2026-11-06 10:00:00', 'CONFIRMEE'),
(28, 11, 10, '2026-11-06 10:05:00', 'CONFIRMEE'),
(29, 9, 12, '2026-11-06 11:00:00', 'CONFIRMEE');

--
-- Déclencheurs `inscription`
--
DELIMITER $$
CREATE TRIGGER `trg_inscription_bi` BEFORE INSERT ON `inscription` FOR EACH ROW BEGIN
    IF NEW.statut = 'CONFIRMEE' THEN
        CALL sp_controle_inscription(0, NEW.id_employe, NEW.id_affectation);
    END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_inscription_bu` BEFORE UPDATE ON `inscription` FOR EACH ROW BEGIN
    -- Contrôles refaits quand une inscription devient (ou redevient) CONFIRMEE,
    -- ou quand on déplace une inscription confirmée vers un autre employé/une autre affectation.
    IF NEW.statut = 'CONFIRMEE'
       AND (OLD.statut <> 'CONFIRMEE'
            OR NOT (OLD.id_employe     <=> NEW.id_employe)
            OR NOT (OLD.id_affectation <=> NEW.id_affectation)) THEN
        CALL sp_controle_inscription(NEW.id_inscription, NEW.id_employe, NEW.id_affectation);
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Structure de la table `maintenance`
--

CREATE TABLE `maintenance` (
  `id_maintenance` int(10) UNSIGNED NOT NULL,
  `id_bus` int(10) UNSIGNED NOT NULL,
  `type_maintenance` enum('PREVENTIVE','CORRECTIVE','CONTROLE_TECHNIQUE','AUTRE') NOT NULL,
  `date_debut` date NOT NULL,
  `date_fin` date DEFAULT NULL,
  `statut` enum('PLANIFIEE','EN_COURS','TERMINEE','ANNULEE') NOT NULL DEFAULT 'PLANIFIEE'
) ;

--
-- Déchargement des données de la table `maintenance`
--

INSERT INTO `maintenance` (`id_maintenance`, `id_bus`, `type_maintenance`, `date_debut`, `date_fin`, `statut`) VALUES
(1, 1, 'PREVENTIVE', '2026-09-14', '2026-09-15', 'TERMINEE'),
(2, 2, 'CONTROLE_TECHNIQUE', '2026-10-05', '2026-10-05', 'TERMINEE'),
(3, 5, 'CORRECTIVE', '2026-08-20', '2026-08-28', 'TERMINEE'),
(4, 4, 'CORRECTIVE', '2026-11-03', '2026-11-12', 'EN_COURS'),
(5, 3, 'PREVENTIVE', '2026-11-23', '2026-11-24', 'PLANIFIEE');

--
-- Déclencheurs `maintenance`
--
DELIMITER $$
CREATE TRIGGER `trg_maintenance_ad` AFTER DELETE ON `maintenance` FOR EACH ROW BEGIN
    IF OLD.statut = 'EN_COURS' THEN
        IF NOT EXISTS (SELECT 1 FROM maintenance
                        WHERE id_bus = OLD.id_bus AND statut = 'EN_COURS') THEN
            UPDATE bus SET statut = 'DISPONIBLE'
             WHERE id_bus = OLD.id_bus AND statut = 'EN_MAINTENANCE';
        END IF;
    END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_maintenance_ai` AFTER INSERT ON `maintenance` FOR EACH ROW BEGIN
    IF NEW.statut = 'EN_COURS' THEN
        UPDATE bus SET statut = 'EN_MAINTENANCE' WHERE id_bus = NEW.id_bus;
    END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_maintenance_au` AFTER UPDATE ON `maintenance` FOR EACH ROW BEGIN
    IF NEW.statut = 'EN_COURS' AND OLD.statut <> 'EN_COURS' THEN
        UPDATE bus SET statut = 'EN_MAINTENANCE' WHERE id_bus = NEW.id_bus;
    ELSEIF OLD.statut = 'EN_COURS' AND NEW.statut <> 'EN_COURS' THEN
        IF NOT EXISTS (SELECT 1 FROM maintenance
                        WHERE id_bus = NEW.id_bus
                          AND statut = 'EN_COURS'
                          AND id_maintenance <> NEW.id_maintenance) THEN
            UPDATE bus SET statut = 'DISPONIBLE'
             WHERE id_bus = NEW.id_bus AND statut = 'EN_MAINTENANCE';
        END IF;
    END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_maintenance_bu` BEFORE UPDATE ON `maintenance` FOR EACH ROW BEGIN
    IF NOT (OLD.id_bus <=> NEW.id_bus) THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50013,
                MESSAGE_TEXT = 'Modification refusée : on ne peut pas changer le bus d''une maintenance.';
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Structure de la table `permission`
--

CREATE TABLE `permission` (
  `id_permission` int(10) UNSIGNED NOT NULL,
  `code` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `permission`
--

INSERT INTO `permission` (`id_permission`, `code`, `description`) VALUES
(1, 'EMPLOYE_GERER', 'Créer, modifier et désactiver les employés'),
(2, 'BUS_GERER', 'Gérer les bus et leur maintenance'),
(3, 'CHAUFFEUR_GERER', 'Gérer les chauffeurs'),
(4, 'TRAJET_GERER', 'Gérer les trajets'),
(5, 'AFFECTATION_GERER', 'Planifier les affectations'),
(6, 'INSCRIPTION_CREER', 'S\'inscrire ou se désinscrire d\'un trajet'),
(7, 'INSCRIPTION_CONSULTER', 'Consulter les inscriptions et le planning'),
(8, 'PRESENCE_SAISIR', 'Saisir les présences'),
(9, 'INCIDENT_DECLARER', 'Déclarer un incident'),
(10, 'INCIDENT_TRAITER', 'Traiter et clôturer les incidents'),
(11, 'RAPPORT_CONSULTER', 'Consulter statistiques et rapports'),
(12, 'COMPTE_GERER', 'Gérer les comptes, rôles et autorisations');

-- --------------------------------------------------------

--
-- Structure de la table `presence`
--

CREATE TABLE `presence` (
  `id_presence` int(10) UNSIGNED NOT NULL,
  `id_inscription` int(10) UNSIGNED NOT NULL,
  `statut` enum('PRESENT','ABSENT','RETARD') NOT NULL,
  `heure_montee` time DEFAULT NULL,
  `remarque` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `presence`
--

INSERT INTO `presence` (`id_presence`, `id_inscription`, `statut`, `heure_montee`, `remarque`) VALUES
(1, 1, 'PRESENT', '06:55:00', NULL),
(2, 2, 'PRESENT', '06:57:00', NULL),
(3, 3, 'RETARD', '07:04:00', 'Arrivée après l\'heure de départ, pris au point suivant'),
(4, 4, 'PRESENT', '06:58:00', NULL),
(5, 5, 'ABSENT', NULL, 'Absence justifiée'),
(6, 6, 'PRESENT', '16:55:00', NULL),
(7, 7, 'PRESENT', '16:58:00', NULL),
(8, 8, 'PRESENT', '07:10:00', NULL),
(9, 9, 'PRESENT', '07:12:00', NULL),
(10, 10, 'ABSENT', NULL, NULL),
(11, 11, 'PRESENT', '06:56:00', NULL),
(12, 12, 'PRESENT', '06:59:00', NULL),
(13, 13, 'PRESENT', '06:57:00', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `role`
--

CREATE TABLE `role` (
  `id_role` int(10) UNSIGNED NOT NULL,
  `nom` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `role`
--

INSERT INTO `role` (`id_role`, `nom`, `description`) VALUES
(1, 'ADMIN', 'Administrateur : accès complet à l\'application'),
(2, 'GESTIONNAIRE', 'Gère les bus, trajets, affectations et rapports'),
(3, 'CHAUFFEUR', 'Consulte son planning et saisit les présences'),
(4, 'EMPLOYE', 'S\'inscrit aux trajets et déclare les incidents');

-- --------------------------------------------------------

--
-- Structure de la table `role_permission`
--

CREATE TABLE `role_permission` (
  `id_role` int(10) UNSIGNED NOT NULL,
  `id_permission` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `role_permission`
--

INSERT INTO `role_permission` (`id_role`, `id_permission`) VALUES
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
(2, 1),
(2, 2),
(2, 3),
(2, 4),
(2, 5),
(2, 7),
(2, 8),
(2, 10),
(2, 11),
(3, 7),
(3, 8),
(3, 9),
(4, 6),
(4, 7),
(4, 9);

-- --------------------------------------------------------

--
-- Structure de la table `trajet`
--

CREATE TABLE `trajet` (
  `id_trajet` int(10) UNSIGNED NOT NULL,
  `nom` varchar(100) NOT NULL,
  `lieu_depart` varchar(150) NOT NULL,
  `lieu_arrivee` varchar(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `trajet`
--

INSERT INTO `trajet` (`id_trajet`, `nom`, `lieu_depart`, `lieu_arrivee`) VALUES
(1, 'Ligne A - Agadir Centre / Tassila', 'Agadir Centre', 'Zone industrielle Tassila'),
(2, 'Ligne B - Inezgane / Tassila', 'Inezgane', 'Zone industrielle Tassila'),
(3, 'Ligne C - Dcheira / Ait Melloul', 'Dcheira El Jihadia', 'Zone industrielle Ait Melloul'),
(4, 'Ligne A retour - Tassila / Agadir Centre', 'Zone industrielle Tassila', 'Agadir Centre');

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `affectation`
--
ALTER TABLE `affectation`
  ADD PRIMARY KEY (`id_affectation`),
  ADD KEY `idx_affectation_bus_date` (`id_bus`,`date_trajet`),
  ADD KEY `idx_affectation_chauffeur_date` (`id_chauffeur`,`date_trajet`),
  ADD KEY `idx_affectation_trajet_date` (`id_trajet`,`date_trajet`);

--
-- Index pour la table `bus`
--
ALTER TABLE `bus`
  ADD PRIMARY KEY (`id_bus`),
  ADD UNIQUE KEY `uq_bus_immatriculation` (`immatriculation`);

--
-- Index pour la table `chauffeur`
--
ALTER TABLE `chauffeur`
  ADD PRIMARY KEY (`id_chauffeur`),
  ADD UNIQUE KEY `uq_chauffeur_employe` (`id_employe`),
  ADD UNIQUE KEY `uq_chauffeur_permis` (`numero_permis`);

--
-- Index pour la table `compte_utilisateur`
--
ALTER TABLE `compte_utilisateur`
  ADD PRIMARY KEY (`id_compte`),
  ADD UNIQUE KEY `uq_compte_login` (`login`),
  ADD UNIQUE KEY `uq_compte_employe` (`id_employe`),
  ADD KEY `fk_compte_role` (`id_role`);

--
-- Index pour la table `employe`
--
ALTER TABLE `employe`
  ADD PRIMARY KEY (`id_employe`),
  ADD UNIQUE KEY `uq_employe_matricule` (`matricule`),
  ADD UNIQUE KEY `uq_employe_email` (`email`);

--
-- Index pour la table `incident`
--
ALTER TABLE `incident`
  ADD PRIMARY KEY (`id_incident`),
  ADD KEY `fk_incident_affectation` (`id_affectation`),
  ADD KEY `fk_incident_compte` (`id_compte`);

--
-- Index pour la table `inscription`
--
ALTER TABLE `inscription`
  ADD PRIMARY KEY (`id_inscription`),
  ADD UNIQUE KEY `uq_inscription_employe_affectation` (`id_employe`,`id_affectation`),
  ADD KEY `fk_inscription_affectation` (`id_affectation`);

--
-- Index pour la table `maintenance`
--
ALTER TABLE `maintenance`
  ADD PRIMARY KEY (`id_maintenance`),
  ADD KEY `idx_maintenance_bus_statut` (`id_bus`,`statut`);

--
-- Index pour la table `permission`
--
ALTER TABLE `permission`
  ADD PRIMARY KEY (`id_permission`),
  ADD UNIQUE KEY `uq_permission_code` (`code`);

--
-- Index pour la table `presence`
--
ALTER TABLE `presence`
  ADD PRIMARY KEY (`id_presence`),
  ADD UNIQUE KEY `uq_presence_inscription` (`id_inscription`);

--
-- Index pour la table `role`
--
ALTER TABLE `role`
  ADD PRIMARY KEY (`id_role`),
  ADD UNIQUE KEY `uq_role_nom` (`nom`);

--
-- Index pour la table `role_permission`
--
ALTER TABLE `role_permission`
  ADD PRIMARY KEY (`id_role`,`id_permission`),
  ADD KEY `fk_rp_permission` (`id_permission`);

--
-- Index pour la table `trajet`
--
ALTER TABLE `trajet`
  ADD PRIMARY KEY (`id_trajet`),
  ADD UNIQUE KEY `uq_trajet_nom` (`nom`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `affectation`
--
ALTER TABLE `affectation`
  MODIFY `id_affectation` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `bus`
--
ALTER TABLE `bus`
  MODIFY `id_bus` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `chauffeur`
--
ALTER TABLE `chauffeur`
  MODIFY `id_chauffeur` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `compte_utilisateur`
--
ALTER TABLE `compte_utilisateur`
  MODIFY `id_compte` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `employe`
--
ALTER TABLE `employe`
  MODIFY `id_employe` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `incident`
--
ALTER TABLE `incident`
  MODIFY `id_incident` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT pour la table `inscription`
--
ALTER TABLE `inscription`
  MODIFY `id_inscription` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=30;

--
-- AUTO_INCREMENT pour la table `maintenance`
--
ALTER TABLE `maintenance`
  MODIFY `id_maintenance` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `permission`
--
ALTER TABLE `permission`
  MODIFY `id_permission` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT pour la table `presence`
--
ALTER TABLE `presence`
  MODIFY `id_presence` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT pour la table `role`
--
ALTER TABLE `role`
  MODIFY `id_role` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `trajet`
--
ALTER TABLE `trajet`
  MODIFY `id_trajet` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `affectation`
--
ALTER TABLE `affectation`
  ADD CONSTRAINT `fk_affectation_bus` FOREIGN KEY (`id_bus`) REFERENCES `bus` (`id_bus`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_affectation_chauffeur` FOREIGN KEY (`id_chauffeur`) REFERENCES `chauffeur` (`id_chauffeur`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_affectation_trajet` FOREIGN KEY (`id_trajet`) REFERENCES `trajet` (`id_trajet`) ON UPDATE CASCADE;

--
-- Contraintes pour la table `chauffeur`
--
ALTER TABLE `chauffeur`
  ADD CONSTRAINT `fk_chauffeur_employe` FOREIGN KEY (`id_employe`) REFERENCES `employe` (`id_employe`) ON UPDATE CASCADE;

--
-- Contraintes pour la table `compte_utilisateur`
--
ALTER TABLE `compte_utilisateur`
  ADD CONSTRAINT `fk_compte_employe` FOREIGN KEY (`id_employe`) REFERENCES `employe` (`id_employe`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_compte_role` FOREIGN KEY (`id_role`) REFERENCES `role` (`id_role`) ON UPDATE CASCADE;

--
-- Contraintes pour la table `incident`
--
ALTER TABLE `incident`
  ADD CONSTRAINT `fk_incident_affectation` FOREIGN KEY (`id_affectation`) REFERENCES `affectation` (`id_affectation`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_incident_compte` FOREIGN KEY (`id_compte`) REFERENCES `compte_utilisateur` (`id_compte`) ON UPDATE CASCADE;

--
-- Contraintes pour la table `inscription`
--
ALTER TABLE `inscription`
  ADD CONSTRAINT `fk_inscription_affectation` FOREIGN KEY (`id_affectation`) REFERENCES `affectation` (`id_affectation`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_inscription_employe` FOREIGN KEY (`id_employe`) REFERENCES `employe` (`id_employe`) ON UPDATE CASCADE;

--
-- Contraintes pour la table `maintenance`
--
ALTER TABLE `maintenance`
  ADD CONSTRAINT `fk_maintenance_bus` FOREIGN KEY (`id_bus`) REFERENCES `bus` (`id_bus`) ON UPDATE CASCADE;

--
-- Contraintes pour la table `presence`
--
ALTER TABLE `presence`
  ADD CONSTRAINT `fk_presence_inscription` FOREIGN KEY (`id_inscription`) REFERENCES `inscription` (`id_inscription`) ON UPDATE CASCADE;

--
-- Contraintes pour la table `role_permission`
--
ALTER TABLE `role_permission`
  ADD CONSTRAINT `fk_rp_permission` FOREIGN KEY (`id_permission`) REFERENCES `permission` (`id_permission`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_rp_role` FOREIGN KEY (`id_role`) REFERENCES `role` (`id_role`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
