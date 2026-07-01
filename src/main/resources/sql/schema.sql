-- =============================================================================
-- Disaster Response System Database Schema
--
-- This script creates the disaster_response_system database (if missing) and all 13
-- tables. It is executed programmatically by DatabaseBootstrap at server
-- startup. Statements are idempotent (CREATE IF NOT EXISTS).
--
--  
-- =============================================================================

-- ----------------------------------------------------------------------------
-- Database
-- ----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS disaster_response_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE disaster_response_system;

-- ----------------------------------------------------------------------------
-- Table: users
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_pk         INT AUTO_INCREMENT PRIMARY KEY,
    user_code       VARCHAR(20)  NOT NULL UNIQUE,
    full_name       VARCHAR(120) NOT NULL,
    email           VARCHAR(120) NOT NULL UNIQUE,
    password_hash   VARCHAR(120) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    -- NULL for citizen/coordinator/admin; set for agency_rep so the
    -- inbox can be scoped to the user's own department. team_leader
    -- accounts may also set this if they belong to a single dept.
    -- No DB-level FK constraint here because the departments table
    -- is created later in this script. The application layer
    -- enforces referential intent.
    department_pk   INT          NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME     NOT NULL,
    last_login_at   DATETIME     NULL,
    INDEX idx_users_email (email),
    INDEX idx_users_role  (role),
    INDEX idx_users_dept  (department_pk)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: departments
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS departments (
    department_pk   INT AUTO_INCREMENT PRIMARY KEY,
    department_code VARCHAR(20)  NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    department_type VARCHAR(30)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME     NOT NULL,
    INDEX idx_dept_type (department_type)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: locations
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS locations (
    location_pk   INT AUTO_INCREMENT PRIMARY KEY,
    location_code VARCHAR(20)   NOT NULL UNIQUE,
    postcode      VARCHAR(10)   NOT NULL,
    suburb        VARCHAR(80)   NOT NULL,
    state         VARCHAR(10)   NOT NULL,
    latitude      DECIMAL(10,7) NOT NULL,
    longitude     DECIMAL(10,7) NOT NULL,
    risk_zone     VARCHAR(20)   NULL,
    display_name  VARCHAR(180)  NOT NULL,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    DATETIME      NOT NULL,
    INDEX idx_locations_state (state),
    INDEX idx_locations_active (active)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: response_teams
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS response_teams (
    team_pk        INT AUTO_INCREMENT PRIMARY KEY,
    team_code      VARCHAR(20)   NOT NULL UNIQUE,
    team_name      VARCHAR(120)  NOT NULL,
    department_pk  INT           NOT NULL,
    availability   VARCHAR(20)   NOT NULL,
    latitude       DECIMAL(10,7) NOT NULL,
    longitude      DECIMAL(10,7) NOT NULL,
    leader_user_pk INT           NULL,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     DATETIME      NOT NULL,
    FOREIGN KEY (department_pk)  REFERENCES departments(department_pk),
    FOREIGN KEY (leader_user_pk) REFERENCES users(user_pk),
    INDEX idx_teams_department   (department_pk),
    INDEX idx_teams_availability (availability)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: incidents
--   description_encrypted holds AES-GCM ciphertext (base64) of the
--   citizen's description.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incidents (
    incident_pk            INT AUTO_INCREMENT PRIMARY KEY,
    incident_code          VARCHAR(20)   NOT NULL UNIQUE,
    reported_by_user_pk    INT           NOT NULL,
    disaster_type          VARCHAR(30)   NOT NULL,
    location_pk            INT           NOT NULL,
    incident_lat           DECIMAL(10,7) NOT NULL,
    incident_lon           DECIMAL(10,7) NOT NULL,
    description_encrypted  TEXT          NOT NULL,
    -- Contact phone the citizen provides at report time. The
    -- coordinator uses this for callbacks ("can you describe what
    -- you saw?"). Stored per-incident rather than per-user because
    -- a citizen may report from different phones on different days.
    -- Validated as AU format (mobile or landline) at the service
    -- layer. DEFAULT '' lets pre-existing rows survive a schema
    -- upgrade; new inserts must pass validation.
    contact_phone          VARCHAR(20)   NOT NULL DEFAULT '',
    people_affected        INT           NOT NULL DEFAULT 0,
    property_risk_level    VARCHAR(20)   NULL,
    cap_severity           VARCHAR(20)   NOT NULL DEFAULT 'UNKNOWN',
    cap_urgency            VARCHAR(20)   NOT NULL DEFAULT 'UNKNOWN',
    cap_certainty          VARCHAR(20)   NOT NULL DEFAULT 'UNKNOWN',
    priority_score         INT           NOT NULL DEFAULT 0,
    status                 VARCHAR(20)   NOT NULL DEFAULT 'REPORTED',
    reported_at            DATETIME      NOT NULL,
    assessed_at            DATETIME      NULL,
    resolved_at            DATETIME      NULL,
    closed_at              DATETIME      NULL,
    closed_by              VARCHAR(20)   NULL,
    FOREIGN KEY (reported_by_user_pk) REFERENCES users(user_pk),
    FOREIGN KEY (location_pk)         REFERENCES locations(location_pk),
    INDEX idx_incidents_status      (status),
    INDEX idx_incidents_priority    (priority_score),
    INDEX idx_incidents_reported_by (reported_by_user_pk)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: incident_assignments
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incident_assignments (
    assignment_pk        INT AUTO_INCREMENT PRIMARY KEY,
    assignment_code      VARCHAR(20)  NOT NULL UNIQUE,
    incident_pk          INT          NOT NULL,
    team_pk              INT          NOT NULL,
    role                 VARCHAR(20)  NOT NULL,
    assignment_status    VARCHAR(20)  NOT NULL,
    assigned_by_user_pk  INT          NOT NULL,
    distance_km          DECIMAL(8,3) NOT NULL,
    assigned_at          DATETIME     NOT NULL,
    started_at           DATETIME     NULL,
    completed_at         DATETIME     NULL,
    FOREIGN KEY (incident_pk)         REFERENCES incidents(incident_pk),
    FOREIGN KEY (team_pk)             REFERENCES response_teams(team_pk),
    FOREIGN KEY (assigned_by_user_pk) REFERENCES users(user_pk),
    INDEX idx_assignments_incident (incident_pk),
    INDEX idx_assignments_team     (team_pk)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: incident_updates
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incident_updates (
    update_pk      INT AUTO_INCREMENT PRIMARY KEY,
    update_code    VARCHAR(20)  NOT NULL UNIQUE,
    incident_pk    INT          NOT NULL,
    updated_by_pk  INT          NOT NULL,
    status_before  VARCHAR(20)  NOT NULL,
    status_after   VARCHAR(20)  NOT NULL,
    comment        VARCHAR(500) NULL,
    created_at     DATETIME     NOT NULL,
    FOREIGN KEY (incident_pk)   REFERENCES incidents(incident_pk),
    FOREIGN KEY (updated_by_pk) REFERENCES users(user_pk),
    INDEX idx_updates_incident (incident_pk)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: notifications
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    notification_pk    INT AUTO_INCREMENT PRIMARY KEY,
    notification_code  VARCHAR(20)  NOT NULL UNIQUE,
    incident_pk        INT          NOT NULL,
    recipient_user_pk  INT          NOT NULL,
    message            VARCHAR(500) NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    created_at         DATETIME     NOT NULL,
    acknowledged_at    DATETIME     NULL,
    FOREIGN KEY (incident_pk)       REFERENCES incidents(incident_pk),
    FOREIGN KEY (recipient_user_pk) REFERENCES users(user_pk),
    INDEX idx_notif_recipient_status (recipient_user_pk, status)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: audit_logs
--   Tamper-evident audit trail. Each row stores prev_hash (the previous
--   row's current_hash) and current_hash (SHA-256 of prev_hash + content).
--   details_encrypted holds AES-GCM ciphertext.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    audit_pk           INT AUTO_INCREMENT PRIMARY KEY,
    audit_code         VARCHAR(20) NOT NULL UNIQUE,
    user_pk            INT         NULL,
    action             VARCHAR(60) NOT NULL,
    entity_type        VARCHAR(40) NULL,
    entity_code        VARCHAR(20) NULL,
    details_encrypted  TEXT        NULL,
    client_ip          VARCHAR(45) NULL,
    success            BOOLEAN     NOT NULL,
    prev_hash          CHAR(64)    NULL,
    current_hash       CHAR(64)    NOT NULL,
    created_at         DATETIME    NOT NULL,
    FOREIGN KEY (user_pk) REFERENCES users(user_pk),
    INDEX idx_audit_created_at (created_at),
    INDEX idx_audit_user       (user_pk)
) ENGINE=InnoDB;

-- ============================================================================
-- NEW FOR A3
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table: resources (Feature 1)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS resources (
    resource_pk        INT AUTO_INCREMENT PRIMARY KEY,
    resource_code      VARCHAR(20)  NOT NULL UNIQUE,
    resource_name      VARCHAR(120) NOT NULL,
    resource_type      VARCHAR(30)  NOT NULL,
    quantity_total     INT          NOT NULL,
    quantity_available INT          NOT NULL,
    home_location_pk   INT          NULL,
    status             VARCHAR(20)  NOT NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,
    FOREIGN KEY (home_location_pk) REFERENCES locations(location_pk),
    INDEX idx_resources_type   (resource_type),
    INDEX idx_resources_status (status),
    CONSTRAINT chk_resource_quantity
        CHECK (quantity_available >= 0
               AND quantity_available <= quantity_total)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: resource_allocations (Feature 1)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS resource_allocations (
    allocation_pk         INT AUTO_INCREMENT PRIMARY KEY,
    allocation_code       VARCHAR(20)  NOT NULL UNIQUE,
    resource_pk           INT          NOT NULL,
    incident_pk           INT          NOT NULL,
    quantity_allocated    INT          NOT NULL,
    allocated_by_user_pk  INT          NOT NULL,
    allocated_at          DATETIME     NOT NULL,
    returned_at           DATETIME     NULL,
    notes                 VARCHAR(500) NULL,
    FOREIGN KEY (resource_pk)          REFERENCES resources(resource_pk),
    FOREIGN KEY (incident_pk)          REFERENCES incidents(incident_pk),
    FOREIGN KEY (allocated_by_user_pk) REFERENCES users(user_pk),
    INDEX idx_alloc_resource (resource_pk),
    INDEX idx_alloc_incident (incident_pk)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: damage_assessments (Feature 2)
--   notes_encrypted holds AES-GCM ciphertext.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS damage_assessments (
    assessment_pk          INT AUTO_INCREMENT PRIMARY KEY,
    assessment_code        VARCHAR(20) NOT NULL UNIQUE,
    incident_pk            INT         NOT NULL,
    assessed_by_user_pk    INT         NOT NULL,
    building_damage_level  VARCHAR(20) NOT NULL,
    road_status            VARCHAR(20) NOT NULL,
    power_status           VARCHAR(20) NOT NULL,
    water_status           VARCHAR(20) NOT NULL,
    casualty_estimate      INT         NOT NULL DEFAULT 0,
    notes_encrypted        TEXT        NULL,
    assessed_at            DATETIME    NOT NULL,
    FOREIGN KEY (incident_pk)         REFERENCES incidents(incident_pk),
    FOREIGN KEY (assessed_by_user_pk) REFERENCES users(user_pk),
    INDEX idx_assess_incident (incident_pk)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table: recovery_tasks (Feature 2)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recovery_tasks (
    task_pk              INT AUTO_INCREMENT PRIMARY KEY,
    task_code            VARCHAR(20)  NOT NULL UNIQUE,
    incident_pk          INT          NOT NULL,
    department_pk        INT          NOT NULL,
    task_type            VARCHAR(30)  NOT NULL,
    description          VARCHAR(500) NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    assigned_to_user_pk  INT          NULL,
    assigned_at          DATETIME     NULL,
    started_at           DATETIME     NULL,
    completed_at         DATETIME     NULL,
    blocked_reason       VARCHAR(300) NULL,
    created_at           DATETIME     NOT NULL,
    FOREIGN KEY (incident_pk)         REFERENCES incidents(incident_pk),
    FOREIGN KEY (department_pk)       REFERENCES departments(department_pk),
    FOREIGN KEY (assigned_to_user_pk) REFERENCES users(user_pk),
    INDEX idx_tasks_incident (incident_pk),
    INDEX idx_tasks_status   (status),
    INDEX idx_tasks_assigned (assigned_to_user_pk)
) ENGINE=InnoDB;
