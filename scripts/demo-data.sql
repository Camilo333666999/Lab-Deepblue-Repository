-- Datos de demostración para probar el frontend de DeepBlue Rescue.
-- IMPORTANTE: este archivo NO es una migración Flyway y NO se ejecuta
-- automáticamente. Ejecutarlo solo en una base local o de laboratorio.
--
-- Regla de tratamiento: solo UNDER_EVALUATION e IN_REHABILITATION son
-- elegibles. AN-2026-001 (ADMITTED) devuelve eligible:false y POST 409.
--
-- Ejemplo:
--   psql -U postgres -h localhost -p 5432 -d deepblue -f scripts/demo-data.sql
--
-- Códigos disponibles después de ejecutarlo:
--   Centro:      DB-CAR
--   Casos:       RES-2026-001 (ADMITTED)
--                RES-2026-002 (IN_REHABILITATION)
--                RES-2026-003 (RELEASED)
--                RES-2026-004 (ADMITTED, para probar transición inválida)
--                RES-2026-005 (UNDER_EVALUATION)
--   Animales:    AN-2026-001 ... AN-2026-005
--   Especialista: SPEC-001 (activo), SPEC-002 (inactivo)

BEGIN;

INSERT INTO rescue_centers (code, name, city)
VALUES ('DB-CAR', 'DeepBlue Caribbean', 'Santa Marta')
ON CONFLICT (code) DO NOTHING;

INSERT INTO rescue_cases (case_code, rescue_date, rescue_location, status, rescue_center_id)
VALUES
    ('RES-2026-001', '2026-10-20', 'Bahia Concha', 'ADMITTED',
        (SELECT id FROM rescue_centers WHERE code = 'DB-CAR')),
    ('RES-2026-002', '2026-10-20', 'Taganga', 'IN_REHABILITATION',
        (SELECT id FROM rescue_centers WHERE code = 'DB-CAR')),
    ('RES-2026-003', '2026-10-20', 'Playa Blanca', 'RELEASED',
        (SELECT id FROM rescue_centers WHERE code = 'DB-CAR')),
    ('RES-2026-004', '2026-10-20', 'Santa Marta', 'ADMITTED',
        (SELECT id FROM rescue_centers WHERE code = 'DB-CAR')),
    ('RES-2026-005', '2026-10-20', 'Tayrona', 'UNDER_EVALUATION',
        (SELECT id FROM rescue_centers WHERE code = 'DB-CAR'))
ON CONFLICT (case_code) DO NOTHING;

INSERT INTO animals (animal_code, common_name, scientific_name, sex, rescue_case_id, tracking_device_code)
VALUES
    ('AN-2026-001', 'Green Sea Turtle', 'Chelonia mydas', 'FEMALE',
        (SELECT id FROM rescue_cases WHERE case_code = 'RES-2026-001'), NULL),
    ('AN-2026-002', 'Hawksbill Turtle', 'Eretmochelys imbricata', 'FEMALE',
        (SELECT id FROM rescue_cases WHERE case_code = 'RES-2026-002'), NULL),
    ('AN-2026-003', 'Sea Lion', 'Zalophus californianus', 'MALE',
        (SELECT id FROM rescue_cases WHERE case_code = 'RES-2026-003'), NULL),
    ('AN-2026-004', 'Dolphin', 'Tursiops truncatus', 'UNKNOWN',
        (SELECT id FROM rescue_cases WHERE case_code = 'RES-2026-004'), NULL),
    ('AN-2026-005', 'Green Sea Turtle', 'Chelonia mydas', 'UNKNOWN',
        (SELECT id FROM rescue_cases WHERE case_code = 'RES-2026-005'), NULL)
ON CONFLICT (animal_code) DO NOTHING;

INSERT INTO medical_records (animal_id, initial_weight, initial_condition, injuries, observations)
SELECT a.id, 27.80, 'STABLE', 'Fishing net injury', 'Possible plastic ingestion'
FROM animals a
WHERE a.animal_code = 'AN-2026-001'
  AND NOT EXISTS (
      SELECT 1 FROM medical_records m WHERE m.animal_id = a.id
  );

INSERT INTO specialists (professional_code, first_name, last_name, email, active)
VALUES
    ('SPEC-001', 'Elena', 'Vargas', 'elena.demo@deepblue.org', true),
    ('SPEC-002', 'Inactive', 'Specialist', 'inactive.demo@deepblue.org', false)
ON CONFLICT (professional_code) DO NOTHING;

INSERT INTO specialist_expertise (specialist_id, expertise_id)
SELECT s.id, e.id
FROM specialists s, expertise e
WHERE s.professional_code = 'SPEC-001'
  AND e.name IN ('Marine Reptiles', 'Rehabilitation')
ON CONFLICT DO NOTHING;

INSERT INTO treatments (animal_id, specialist_id, performed_at, type, description)
SELECT a.id, s.id, '2026-10-21 09:00:00', 'WOUND_CARE',
       'Clean wound and apply antibiotic treatment'
FROM animals a, specialists s
WHERE a.animal_code = 'AN-2026-002'
  AND s.professional_code = 'SPEC-001'
  AND NOT EXISTS (
      SELECT 1
      FROM treatments t
      WHERE t.animal_id = a.id
        AND t.performed_at = '2026-10-21 09:00:00'
  );

COMMIT;
