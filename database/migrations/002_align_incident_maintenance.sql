-- =====================================================================
-- Migration 002 : aligne les tables « incident » et « maintenance » d'une base
-- ancienne version sur le schéma de référence database/emplogo.sql.
-- À exécuter une seule fois, et seulement si la table incident a encore une
-- colonne `type` (ancienne version) :
--   mysql -u root emplogo < database/migrations/002_align_incident_maintenance.sql
-- Les données sont conservées (les valeurs équivalentes sont renommées).
-- =====================================================================

-- incident.type -> incident.type_incident
ALTER TABLE incident
  CHANGE `type` type_incident ENUM('PANNE','ACCIDENT','RETARD','COMPORTEMENT','AUTRE') NOT NULL;

-- gravité : MINEURE/MODEREE/GRAVE -> FAIBLE/MOYENNE/ELEVEE
ALTER TABLE incident
  MODIFY gravite ENUM('MINEURE','MODEREE','GRAVE','CRITIQUE','FAIBLE','MOYENNE','ELEVEE') NOT NULL;
UPDATE incident SET gravite = CASE gravite
    WHEN 'MINEURE' THEN 'FAIBLE'
    WHEN 'MODEREE' THEN 'MOYENNE'
    WHEN 'GRAVE'   THEN 'ELEVEE'
    ELSE gravite END;
ALTER TABLE incident
  MODIFY gravite ENUM('FAIBLE','MOYENNE','ELEVEE','CRITIQUE') NOT NULL;

-- incident.statut -> incident.statut_traitement, CLOTURE -> CLOS
ALTER TABLE incident
  CHANGE statut statut_traitement ENUM('OUVERT','EN_COURS','RESOLU','CLOTURE','CLOS') NOT NULL DEFAULT 'OUVERT';
UPDATE incident SET statut_traitement = 'CLOS' WHERE statut_traitement = 'CLOTURE';
ALTER TABLE incident
  MODIFY statut_traitement ENUM('OUVERT','EN_COURS','RESOLU','CLOS') NOT NULL DEFAULT 'OUVERT';

-- mode de déclaration : union des deux versions (aucune valeur perdue)
ALTER TABLE incident
  MODIFY mode_declaration ENUM('APPLICATION','TELEPHONE','EMAIL','SUR_PLACE',
                               'RAPPORT_PAPIER','SIGNALEMENT_CHAUFFEUR') NOT NULL DEFAULT 'APPLICATION';

-- maintenance.type -> maintenance.type_maintenance
ALTER TABLE maintenance
  CHANGE `type` type_maintenance ENUM('PREVENTIVE','CORRECTIVE','CONTROLE_TECHNIQUE','AUTRE') NOT NULL;
