-- =============================================================================
-- Disaster Response System — Demo Seed Data
--
-- Demo users, departments, locations, teams, resources, and three fresh
-- incident reports. Incidents are deliberately left in REPORTED status
-- so the coordinator can assess, assign, and progress them during a demo.
--
-- Run AFTER schema.sql.
--
-- All demo accounts use password: Demo@123
-- BCrypt hash ($2a$10$...) is verified to authenticate against "Demo@123"
-- do NOT change this string.
--
-- Encrypted columns such as description_encrypted are seeded as PLAINTEXT.
-- The repositories detect a decrypt failure and fall back to the raw value,
-- so seed data displays correctly. Records inserted at runtime by the
-- application use real AES-GCM ciphertext.
--
-- audit_logs is intentionally NOT seeded with human actions. Real user
-- actions populate it once the system is running. This preserves the
-- non-repudiation property of the hash chain.
-- =============================================================================

USE disaster_response_system;

-- ----------------------------------------------------------------------------
-- Users (password for all: Demo@123)
-- The 5 primary demo accounts cover all five roles.
-- Additional citizens provide realistic incident reporters.
-- Staff linkage to departments is set NULL here; team_leader and
-- agency_rep accounts that need department_pk are set in a second
-- INSERT after the departments block.
-- ----------------------------------------------------------------------------
INSERT INTO users
    (user_code, full_name, email, password_hash, role,
     department_pk, active, created_at)
VALUES
    ('USR-2026-0001', 'Mukesh Varman',      'mukesh@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'COORDINATOR', NULL, TRUE, NOW()),
    ('USR-2026-0002', 'Alex Carter',        'alex.carter@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'CITIZEN', NULL, TRUE, NOW()),
    ('USR-2026-0003', 'Jordan Blake',       'jordan.blake@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'TEAM_LEADER', NULL, TRUE, NOW()),
    ('USR-2026-0004', 'Taylor Morgan',      'taylor.morgan@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'AGENCY_REP', NULL, TRUE, NOW()),
    ('USR-2026-0005', 'System Admin',       'admin@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'ADMIN', NULL, TRUE, NOW()),
    ('USR-2026-0006', 'Sam Rivera',         'sam.rivera@example.com',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'CITIZEN', NULL, TRUE, NOW()),
    ('USR-2026-0007', 'Casey Kim',          'casey.kim@example.com',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'CITIZEN', NULL, TRUE, NOW()),
    ('USR-2026-0008', 'Riley Thompson',     'riley.thompson@example.com',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'CITIZEN', NULL, TRUE, NOW()),
    ('USR-2026-0009', 'Morgan Davies',      'morgan.davies@example.com',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'CITIZEN', NULL, TRUE, NOW());

-- ----------------------------------------------------------------------------
-- Departments
-- ----------------------------------------------------------------------------
INSERT INTO departments
    (department_code, name, department_type, active, created_at)
VALUES
    ('DEP-FIRE',  'Fire & Rescue NSW',             'FIRE',       TRUE, NOW()),
    ('DEP-HOSP',  'NSW Health',                    'HOSPITAL',   TRUE, NOW()),
    ('DEP-POL',   'NSW Police',                    'POLICE',     TRUE, NOW()),
    ('DEP-UTIL',  'Endeavour Energy',              'UTILITY',    TRUE, NOW()),
    ('DEP-TRAN',  'Transport for NSW',             'TRANSPORT',  TRUE, NOW()),
    ('DEP-WAST',  'Cleanaway',                     'WASTE_MGMT', TRUE, NOW()),
    ('DEP-WATR',  'Sydney Water',                  'WATER',      TRUE, NOW()),
    ('DEP-SCHL',  'NSW Department of Education',   'SCHOOL',     TRUE, NOW());

-- ----------------------------------------------------------------------------
-- Locations
-- ----------------------------------------------------------------------------
INSERT INTO locations
    (location_code, postcode, suburb, state, latitude, longitude,
     risk_zone, display_name, active, created_at)
VALUES
    ('LOC-2026-0001', '2000', 'Sydney CBD',     'NSW', -33.8688, 151.2093, 'Urban',    'Sydney CBD NSW 2000',     TRUE, NOW()),
    ('LOC-2026-0002', '2150', 'Parramatta',     'NSW', -33.8150, 151.0000, 'Urban',    'Parramatta NSW 2150',     TRUE, NOW()),
    ('LOC-2026-0003', '2750', 'Penrith',        'NSW', -33.7510, 150.6940, 'Flood',    'Penrith NSW 2750',        TRUE, NOW()),
    ('LOC-2026-0004', '2026', 'Bondi',          'NSW', -33.8908, 151.2743, 'Coastal',  'Bondi NSW 2026',          TRUE, NOW()),
    ('LOC-2026-0005', '2065', 'St Leonards',    'NSW', -33.8230, 151.1955, 'Urban',    'St Leonards NSW 2065',    TRUE, NOW()),
    ('LOC-2026-0006', '2220', 'Hurstville',     'NSW', -33.9670, 151.1023, 'Urban',    'Hurstville NSW 2220',     TRUE, NOW()),
    ('LOC-2026-0007', '2170', 'Liverpool',      'NSW', -33.9200, 150.9230, 'Urban',    'Liverpool NSW 2170',      TRUE, NOW()),
    ('LOC-2026-0008', '2076', 'Hornsby',        'NSW', -33.7036, 151.0987, 'Bushfire', 'Hornsby NSW 2076',        TRUE, NOW()),
    ('LOC-2026-0009', '2228', 'Cronulla',       'NSW', -34.0581, 151.1521, 'Coastal',  'Cronulla NSW 2228',       TRUE, NOW()),
    ('LOC-2026-0010', '2145', 'Westmead',       'NSW', -33.8067, 150.9876, 'Urban',    'Westmead NSW 2145',       TRUE, NOW()),
    ('LOC-2026-0011', '2570', 'Camden',         'NSW', -34.0531, 150.6960, 'Bushfire', 'Camden NSW 2570',         TRUE, NOW()),
    ('LOC-2026-0012', '2444', 'Port Macquarie', 'NSW', -31.4313, 152.9087, 'Coastal',  'Port Macquarie NSW 2444', TRUE, NOW()),
    ('LOC-2026-0013', '2300', 'Newcastle',      'NSW', -32.9283, 151.7817, 'Urban',    'Newcastle NSW 2300',      TRUE, NOW()),
    ('LOC-2026-0014', '2500', 'Wollongong',     'NSW', -34.4278, 150.8931, 'Urban',    'Wollongong NSW 2500',     TRUE, NOW()),
    ('LOC-2026-0015', '2640', 'Albury',         'NSW', -36.0737, 146.9135, 'Flood',    'Albury NSW 2640',         TRUE, NOW());

-- ----------------------------------------------------------------------------
-- Additional staff users — 8 team leaders + 8 agency reps.
-- All passwords: Demo@123. Email addresses use generic role-based format.
-- ----------------------------------------------------------------------------
INSERT INTO users
    (user_code, full_name, email, password_hash, role,
     department_pk, active, created_at)
VALUES
    ('USR-2026-0010', 'Emma Walsh',
     'tl1@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'TEAM_LEADER',
     (SELECT department_pk FROM departments WHERE department_code='DEP-FIRE'),
     TRUE, NOW()),
    ('USR-2026-0011', 'Liam Foster',
     'tl2@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'TEAM_LEADER',
     (SELECT department_pk FROM departments WHERE department_code='DEP-FIRE'),
     TRUE, NOW()),
    ('USR-2026-0012', 'Dr. Nina Patel',
     'tl3@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'TEAM_LEADER',
     (SELECT department_pk FROM departments WHERE department_code='DEP-HOSP'),
     TRUE, NOW()),
    ('USR-2026-0013', 'Dr. Owen Griffiths',
     'tl4@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'TEAM_LEADER',
     (SELECT department_pk FROM departments WHERE department_code='DEP-HOSP'),
     TRUE, NOW()),
    ('USR-2026-0014', 'Sergeant Dana Hooper',
     'tl5@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'TEAM_LEADER',
     (SELECT department_pk FROM departments WHERE department_code='DEP-POL'),
     TRUE, NOW()),
    ('USR-2026-0015', 'Chris Navarro',
     'tl6@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'TEAM_LEADER',
     (SELECT department_pk FROM departments WHERE department_code='DEP-UTIL'),
     TRUE, NOW()),
    ('USR-2026-0016', 'Avery Stone',
     'tl7@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'TEAM_LEADER',
     (SELECT department_pk FROM departments WHERE department_code='DEP-TRAN'),
     TRUE, NOW()),
    ('USR-2026-0017', 'Drew Sinclair',
     'tl8@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'TEAM_LEADER',
     (SELECT department_pk FROM departments WHERE department_code='DEP-WAST'),
     TRUE, NOW()),
    ('USR-2026-0018', 'Captain Robin Hale',
     'ar-fire@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'AGENCY_REP',
     (SELECT department_pk FROM departments WHERE department_code='DEP-FIRE'),
     TRUE, NOW()),
    ('USR-2026-0019', 'Dr. Frances Quinn',
     'ar-hosp@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'AGENCY_REP',
     (SELECT department_pk FROM departments WHERE department_code='DEP-HOSP'),
     TRUE, NOW()),
    ('USR-2026-0020', 'Inspector Lee Grant',
     'ar-pol@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'AGENCY_REP',
     (SELECT department_pk FROM departments WHERE department_code='DEP-POL'),
     TRUE, NOW()),
    ('USR-2026-0021', 'Pat Hendricks',
     'ar-util@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'AGENCY_REP',
     (SELECT department_pk FROM departments WHERE department_code='DEP-UTIL'),
     TRUE, NOW()),
    ('USR-2026-0022', 'Reese Calloway',
     'ar-tran@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'AGENCY_REP',
     (SELECT department_pk FROM departments WHERE department_code='DEP-TRAN'),
     TRUE, NOW()),
    ('USR-2026-0023', 'Quinn Delgado',
     'ar-wast@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'AGENCY_REP',
     (SELECT department_pk FROM departments WHERE department_code='DEP-WAST'),
     TRUE, NOW()),
    ('USR-2026-0024', 'Blair Sutton',
     'ar-watr@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'AGENCY_REP',
     (SELECT department_pk FROM departments WHERE department_code='DEP-WATR'),
     TRUE, NOW()),
    ('USR-2026-0025', 'Principal Jamie Ellis',
     'ar-schl@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'AGENCY_REP',
     (SELECT department_pk FROM departments WHERE department_code='DEP-SCHL'),
     TRUE, NOW()),
    ('USR-2026-0026', 'Harper Lane',          'harper.lane@drs.local',
     '$2a$10$dHu2irqiEf/TIqyHnWkQ.OXhBbL0Gw.60GAArOWfBxNBYLlCrUM8K',
     'CITIZEN', NULL, TRUE, NOW());

-- Wire Taylor Morgan (agency rep) to Fire department.
UPDATE users SET department_pk =
    (SELECT department_pk FROM departments WHERE department_code='DEP-FIRE')
WHERE user_code = 'USR-2026-0004';

-- ----------------------------------------------------------------------------
-- Response teams
-- ----------------------------------------------------------------------------
INSERT INTO response_teams
    (team_code, team_name, department_pk, availability, latitude, longitude,
     leader_user_pk, active, created_at)
VALUES
    ('TM-2026-0001', 'Fire Rescue Sydney CBD',
        (SELECT department_pk FROM departments WHERE department_code='DEP-FIRE'),
        'AVAILABLE', -33.8688, 151.2093,
        (SELECT user_pk FROM users WHERE user_code='USR-2026-0003'), TRUE, NOW()),
    ('TM-2026-0002', 'Fire Rescue Parramatta',
        (SELECT department_pk FROM departments WHERE department_code='DEP-FIRE'),
        'AVAILABLE', -33.8150, 151.0000, NULL, TRUE, NOW()),
    ('TM-2026-0003', 'Hospital Westmead',
        (SELECT department_pk FROM departments WHERE department_code='DEP-HOSP'),
        'AVAILABLE', -33.8067, 150.9876, NULL, TRUE, NOW()),
    ('TM-2026-0004', 'Hospital RPA',
        (SELECT department_pk FROM departments WHERE department_code='DEP-HOSP'),
        'AVAILABLE', -33.8898, 151.1830, NULL, TRUE, NOW()),
    ('TM-2026-0005', 'Police Surry Hills',
        (SELECT department_pk FROM departments WHERE department_code='DEP-POL'),
        'AVAILABLE', -33.8836, 151.2127, NULL, TRUE, NOW()),
    ('TM-2026-0006', 'Utility Crew North',
        (SELECT department_pk FROM departments WHERE department_code='DEP-UTIL'),
        'AVAILABLE', -33.8230, 151.1955, NULL, TRUE, NOW()),
    ('TM-2026-0007', 'Transport Maintenance',
        (SELECT department_pk FROM departments WHERE department_code='DEP-TRAN'),
        'AVAILABLE', -33.8688, 151.2093, NULL, TRUE, NOW()),
    ('TM-2026-0008', 'Waste Cleanup Crew',
        (SELECT department_pk FROM departments WHERE department_code='DEP-WAST'),
        'AVAILABLE', -33.7510, 150.6940, NULL, TRUE, NOW());

-- Wire team leaders to their teams.
UPDATE response_teams SET leader_user_pk =
    (SELECT user_pk FROM users WHERE user_code='USR-2026-0011')
WHERE team_code='TM-2026-0002';

UPDATE response_teams SET leader_user_pk =
    (SELECT user_pk FROM users WHERE user_code='USR-2026-0012')
WHERE team_code='TM-2026-0003';

UPDATE response_teams SET leader_user_pk =
    (SELECT user_pk FROM users WHERE user_code='USR-2026-0013')
WHERE team_code='TM-2026-0004';

UPDATE response_teams SET leader_user_pk =
    (SELECT user_pk FROM users WHERE user_code='USR-2026-0014')
WHERE team_code='TM-2026-0005';

UPDATE response_teams SET leader_user_pk =
    (SELECT user_pk FROM users WHERE user_code='USR-2026-0015')
WHERE team_code='TM-2026-0006';

UPDATE response_teams SET leader_user_pk =
    (SELECT user_pk FROM users WHERE user_code='USR-2026-0016')
WHERE team_code='TM-2026-0007';

UPDATE response_teams SET leader_user_pk =
    (SELECT user_pk FROM users WHERE user_code='USR-2026-0017')
WHERE team_code='TM-2026-0008';

-- ----------------------------------------------------------------------------
-- Resources — initial inventory, no allocations seeded.
-- ----------------------------------------------------------------------------
INSERT INTO resources
    (resource_code, resource_name, resource_type, quantity_total,
     quantity_available, home_location_pk, status, created_at, updated_at)
VALUES
    ('RES-2026-0001', 'Fire Truck Alpha',      'VEHICLE',        3,   3,
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0001'),
        'AVAILABLE', NOW(), NOW()),
    ('RES-2026-0002', 'Ambulance Unit',        'VEHICLE',        5,   5,
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0010'),
        'AVAILABLE', NOW(), NOW()),
    ('RES-2026-0003', 'Medical Kit Standard',  'MEDICAL_SUPPLY', 50,  50,
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0010'),
        'AVAILABLE', NOW(), NOW()),
    ('RES-2026-0004', 'Generator 5kW',         'EQUIPMENT',      10,  10,
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0001'),
        'AVAILABLE', NOW(), NOW()),
    ('RES-2026-0005', 'Sandbag Pallet',        'EQUIPMENT',      100, 100,
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0003'),
        'AVAILABLE', NOW(), NOW()),
    ('RES-2026-0006', 'Emergency Food Pack',   'FOOD_WATER',     200, 200,
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0010'),
        'AVAILABLE', NOW(), NOW()),
    ('RES-2026-0007', 'Bottled Water 1L',      'FOOD_WATER',     500, 500,
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0010'),
        'AVAILABLE', NOW(), NOW()),
    ('RES-2026-0008', 'Temporary Shelter Tent','SHELTER',        20,  20,
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0007'),
        'AVAILABLE', NOW(), NOW()),
    ('RES-2026-0009', 'Two-way Radio Set',     'COMMUNICATION',  30,  30,
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0001'),
        'AVAILABLE', NOW(), NOW()),
    ('RES-2026-0010', 'Chainsaw Heavy Duty',   'EQUIPMENT',      8,   8,
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0008'),
        'AVAILABLE', NOW(), NOW()),
    ('RES-2026-0011', 'Stretcher',             'MEDICAL_SUPPLY', 25,  25,
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0010'),
        'AVAILABLE', NOW(), NOW()),
    ('RES-2026-0012', 'Mobile Command Vehicle','VEHICLE',        2,   2,
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0001'),
        'AVAILABLE', NOW(), NOW());

-- ----------------------------------------------------------------------------
-- Incidents — three fresh reports in REPORTED status.
-- Coordinator assesses, assigns, and progresses these during a demo run.
-- ----------------------------------------------------------------------------
INSERT INTO incidents
    (incident_code, reported_by_user_pk, disaster_type, location_pk,
     incident_lat, incident_lon, description_encrypted, contact_phone,
     people_affected, property_risk_level,
     cap_severity, cap_urgency, cap_certainty, priority_score, status,
     reported_at, assessed_at, resolved_at, closed_at, closed_by)
VALUES
    ('INC-2026-0001',
        (SELECT user_pk FROM users WHERE user_code='USR-2026-0002'),
        'FLOOD',
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0003'),
        -33.7510, 150.6940,
        'Heavy rainfall has caused street flooding near Penrith. Water is rising around homes and residents need assistance.',
        '0412 567 890',
        45, 'High',
        'UNKNOWN', 'UNKNOWN', 'UNKNOWN', 0, 'REPORTED',
        DATE_SUB(NOW(), INTERVAL 30 MINUTE),
        NULL, NULL, NULL, NULL),

    ('INC-2026-0002',
        (SELECT user_pk FROM users WHERE user_code='USR-2026-0026'),
        'FIRE',
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0001'),
        -33.8688, 151.2093,
        'Smoke is visible from an office building in Sydney CBD. People are leaving the area and emergency support is needed.',
        '0456 781 209',
        80, 'High',
        'UNKNOWN', 'UNKNOWN', 'UNKNOWN', 0, 'REPORTED',
        DATE_SUB(NOW(), INTERVAL 20 MINUTE),
        NULL, NULL, NULL, NULL),

    ('INC-2026-0003',
        (SELECT user_pk FROM users WHERE user_code='USR-2026-0006'),
        'HAZMAT',
        (SELECT location_pk FROM locations WHERE location_code='LOC-2026-0007'),
        -33.9200, 150.9230,
        'A tanker truck has rolled near Liverpool and liquid is leaking onto the road. There is a strong chemical smell nearby.',
        '0438 110 224',
        120, 'High',
        'UNKNOWN', 'UNKNOWN', 'UNKNOWN', 0, 'REPORTED',
        DATE_SUB(NOW(), INTERVAL 10 MINUTE),
        NULL, NULL, NULL, NULL);

-- ----------------------------------------------------------------------------
-- The following tables are intentionally not seeded.
-- Runtime application actions create these records during normal use.
--
-- incident_assignments  — coordinator assigns teams after assessing incidents
-- incident_updates      — status transitions recorded as incidents progress
-- notifications         — generated when assignments and updates are created
-- resource_allocations  — resource manager allocates during active incidents
-- damage_assessments    — team leaders submit after on-site assessment
-- recovery_tasks        — coordinators create tasks during recovery phase
-- audit_logs            — every user action writes a hash-chained audit entry
-- ----------------------------------------------------------------------------
