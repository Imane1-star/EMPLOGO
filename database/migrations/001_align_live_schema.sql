-- =====================================================================
-- Migration 001 : aligne une base « emplogo » ancienne version sur le schéma
-- de référence database/emplogo.sql (celui utilisé par le code Java).
--   * chauffeur.validite_permis  -> chauffeur.date_validite_permis
--   * chauffeur.statut : ajout de la valeur INACTIF
--   * remplace les procédures sp_verifier_* et les déclencheurs par ceux du schéma
-- Les données sont conservées. À exécuter une seule fois :
--   mysql -u root emplogo < database/migrations/001_align_live_schema.sql
-- =====================================================================

ALTER TABLE chauffeur
  CHANGE validite_permis date_validite_permis DATE NOT NULL,
  MODIFY statut ENUM('ACTIF','EN_CONGE','SUSPENDU','INACTIF') NOT NULL DEFAULT 'ACTIF';

DROP TRIGGER IF EXISTS trg_affectation_bi;
DROP TRIGGER IF EXISTS trg_affectation_bu;
DROP TRIGGER IF EXISTS trg_inscription_bi;
DROP TRIGGER IF EXISTS trg_inscription_bu;
DROP TRIGGER IF EXISTS trg_maintenance_ai;
DROP TRIGGER IF EXISTS trg_maintenance_au;
DROP TRIGGER IF EXISTS trg_maintenance_ad;
DROP TRIGGER IF EXISTS trg_maintenance_bu;
DROP PROCEDURE IF EXISTS sp_verifier_affectation;
DROP PROCEDURE IF EXISTS sp_verifier_inscription;
DROP PROCEDURE IF EXISTS sp_controle_affectation;
DROP PROCEDURE IF EXISTS sp_controle_inscription;

DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_controle_affectation` (IN `p_id_affectation` INT UNSIGNED, IN `p_id_bus` INT UNSIGNED, IN `p_id_chauffeur` INT UNSIGNED, IN `p_date_trajet` DATE, IN `p_heure_depart` TIME, IN `p_heure_arrivee` TIME)   BEGIN
    DECLARE v_statut_bus       VARCHAR(20) DEFAULT NULL;
    DECLARE v_statut_chauffeur VARCHAR(20) DEFAULT NULL;
    DECLARE v_validite_permis  DATE        DEFAULT NULL;
    DECLARE v_employe_actif    TINYINT     DEFAULT NULL;
    DECLARE v_nb               INT         DEFAULT 0;

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

        SET v_actif = (SELECT actif FROM employe WHERE id_employe = p_id_employe);
    IF v_actif IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50000, MESSAGE_TEXT = 'Inscription refusée : employé introuvable.';
    END IF;
    IF v_actif <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50008, MESSAGE_TEXT = 'Inscription refusée : l''employé est désactivé.';
    END IF;

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

        SELECT COUNT(*) INTO v_nb
      FROM inscription i
     WHERE i.id_affectation = p_id_affectation
       AND i.statut = 'CONFIRMEE'
       AND i.id_inscription <> p_id_inscription;
    IF v_nb >= v_capacite THEN
        SIGNAL SQLSTATE '45000'
            SET MYSQL_ERRNO = 50009, MESSAGE_TEXT = 'Inscription refusée : la capacité du bus est atteinte.';
    END IF;

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

                                IF OLD.statut = 'ANNULEE'
           OR NOT (OLD.id_bus        <=> NEW.id_bus)
           OR NOT (OLD.id_chauffeur  <=> NEW.id_chauffeur)
           OR NOT (OLD.date_trajet   <=> NEW.date_trajet)
           OR NOT (OLD.heure_depart  <=> NEW.heure_depart)
           OR NOT (OLD.heure_arrivee <=> NEW.heure_arrivee) THEN
            CALL sp_controle_affectation(NEW.id_affectation, NEW.id_bus, NEW.id_chauffeur,
                                         NEW.date_trajet, NEW.heure_depart, NEW.heure_arrivee);
        END IF;

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
            IF NEW.statut = 'CONFIRMEE'
       AND (OLD.statut <> 'CONFIRMEE'
            OR NOT (OLD.id_employe     <=> NEW.id_employe)
            OR NOT (OLD.id_affectation <=> NEW.id_affectation)) THEN
        CALL sp_controle_inscription(NEW.id_inscription, NEW.id_employe, NEW.id_affectation);
    END IF;
END
$$
DELIMITER ;

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
