-- =============================================================================
-- FuelMatch :: Nutrition Service — V2
-- Configuração de threshold fuzzy + seed de amostra TACO
-- =============================================================================

-- ── Configuração global do pg_trgm para este banco ────────────────────────────
-- Threshold padrão é 0.3; baixamos para 0.2 para capturar erros maiores de digitação
-- (ex: "fragnho" → "frango")
SET pg_trgm.similarity_threshold = 0.2;

-- Persiste config na sessão do flyway; em produção, configure também no postgresql.conf:
-- pg_trgm.similarity_threshold = 0.2

-- ── Seed: Amostra da Tabela TACO ──────────────────────────────────────────────
-- Fonte: TACO 4ª edição — valores por 100g
-- Em produção, substituir por importação completa via TacoImportService

INSERT INTO foods (
    id, name, source, external_id, category,
    energy_kcal, energy_kj, carbohydrates_g, of_which_fiber_g, of_which_sugars_g,
    proteins_g, fat_total_g, of_which_saturated_g, sodium_mg,
    is_active, is_verified
) VALUES

-- Cereais e Grãos
('00000001-0000-0000-0000-000000000001', 'Arroz, branco, cozido',               'TACO', 'TACO-001', 'CEREAIS_GRAOS',    128, 537,  28.1, 1.6, NULL,  2.5, 0.2, 0.1,   1,  true, true),
('00000001-0000-0000-0000-000000000002', 'Arroz, integral, cozido',             'TACO', 'TACO-002', 'CEREAIS_GRAOS',    124, 519,  25.8, 2.7, NULL,  2.6, 1.0, 0.2,   1,  true, true),
('00000001-0000-0000-0000-000000000003', 'Macarrão, cozido',                    'TACO', 'TACO-003', 'CEREAIS_GRAOS',    125, 522,  25.1, 1.8, NULL,  3.8, 0.5, 0.1,   1,  true, true),
('00000001-0000-0000-0000-000000000004', 'Aveia, flocos',                       'TACO', 'TACO-004', 'CEREAIS_GRAOS',    394, 1649, 67.0, 9.1, NULL, 13.9, 7.0, 1.2,   3,  true, true),
('00000001-0000-0000-0000-000000000005', 'Pão, francês',                        'TACO', 'TACO-005', 'CEREAIS_GRAOS',    300, 1254, 58.6, 2.3, NULL,  8.0, 3.1, 0.7, 590,  true, true),
('00000001-0000-0000-0000-000000000006', 'Quinoa, cozida',                      'TACO', 'TACO-006', 'CEREAIS_GRAOS',    120, 502,  21.3, 2.8, NULL,  4.4, 1.9, 0.2,   7,  true, true),

-- Leguminosas
('00000002-0000-0000-0000-000000000001', 'Feijão, preto, cozido',               'TACO', 'TACO-101', 'LEGUMINOSAS',       77, 323,  13.6, 8.4, NULL,  4.5, 0.5, 0.1,   2,  true, true),
('00000002-0000-0000-0000-000000000002', 'Feijão, carioca, cozido',             'TACO', 'TACO-102', 'LEGUMINOSAS',       76, 317,  13.5, 7.8, NULL,  4.8, 0.5, 0.1,   2,  true, true),
('00000002-0000-0000-0000-000000000003', 'Lentilha, cozida',                    'TACO', 'TACO-103', 'LEGUMINOSAS',       93, 390,  16.3, 7.9, NULL,  6.3, 0.5, 0.1,   2,  true, true),
('00000002-0000-0000-0000-000000000004', 'Grão-de-bico, cozido',                'TACO', 'TACO-104', 'LEGUMINOSAS',      164, 687,  27.4, 6.0, NULL,  8.9, 2.6, 0.3,   7,  true, true),

-- Carnes e Aves
('00000003-0000-0000-0000-000000000001', 'Frango, peito, grelhado',             'TACO', 'TACO-201', 'CARNES_AVES',      159, 665,   0.0, 0.0, NULL, 32.0, 3.2, 0.9,  74,  true, true),
('00000003-0000-0000-0000-000000000002', 'Frango, coxa, cozida',                'TACO', 'TACO-202', 'CARNES_AVES',      175, 733,   0.0, 0.0, NULL, 25.9, 7.9, 2.2,  89,  true, true),
('00000003-0000-0000-0000-000000000003', 'Bife, contrafilé, grelhado',          'TACO', 'TACO-301', 'CARNES_BOVINAS',   219, 917,   0.0, 0.0, NULL, 29.3,10.8, 4.3,  52,  true, true),
('00000003-0000-0000-0000-000000000004', 'Patinho, moído, cozido',              'TACO', 'TACO-302', 'CARNES_BOVINAS',   199, 833,   0.0, 0.0, NULL, 28.5, 9.5, 3.6,  66,  true, true),

-- Pescados
('00000004-0000-0000-0000-000000000001', 'Atum, em lata, ao natural',           'TACO', 'TACO-401', 'PESCADOS',         119, 499,   0.0, 0.0, NULL, 26.0, 1.5, 0.5, 396,  true, true),
('00000004-0000-0000-0000-000000000002', 'Salmão, grelhado',                    'TACO', 'TACO-402', 'PESCADOS',         183, 767,   0.0, 0.0, NULL, 23.9, 9.6, 1.9,  50,  true, true),
('00000004-0000-0000-0000-000000000003', 'Tilápia, assada',                     'TACO', 'TACO-403', 'PESCADOS',         128, 537,   0.0, 0.0, NULL, 26.2, 2.7, 0.9,  52,  true, true),

-- Ovos e Laticínios
('00000005-0000-0000-0000-000000000001', 'Ovo de galinha, inteiro, cozido',     'TACO', 'TACO-501', 'OVOS_LATICINIOS',  146, 611,   0.6, 0.0, NULL, 12.6,10.0, 3.1, 139,  true, true),
('00000005-0000-0000-0000-000000000002', 'Clara de ovo, cozida',                'TACO', 'TACO-502', 'OVOS_LATICINIOS',   48, 203,   0.8, 0.0, NULL, 10.7, 0.0, 0.0, 175,  true, true),
('00000005-0000-0000-0000-000000000003', 'Queijo minas, frescal',               'TACO', 'TACO-503', 'OVOS_LATICINIOS',  264,1103,   3.0, 0.0, NULL, 17.4,20.8,13.3, 430,  true, true),
('00000005-0000-0000-0000-000000000004', 'Iogurte, desnatado, natural',         'TACO', 'TACO-504', 'OVOS_LATICINIOS',   43, 179,   5.6, 0.0, NULL,  4.1, 0.4, 0.2,  62,  true, true),
('00000005-0000-0000-0000-000000000005', 'Leite, integral',                     'TACO', 'TACO-505', 'OVOS_LATICINIOS',   61, 253,   4.7, 0.0, NULL,  3.2, 3.2, 2.0,  44,  true, true),

-- Hortalicas
('00000006-0000-0000-0000-000000000001', 'Brócolis, cozido',                    'TACO', 'TACO-601', 'HORTALICAS',        25, 104,   3.1, 2.5, NULL,  2.9, 0.4, 0.1,  26,  true, true),
('00000006-0000-0000-0000-000000000002', 'Espinafre, cru',                      'TACO', 'TACO-602', 'HORTALICAS',        18,  77,   2.2, 2.2, NULL,  2.2, 0.3, 0.0,  79,  true, true),
('00000006-0000-0000-0000-000000000003', 'Batata-doce, cozida',                 'TACO', 'TACO-603', 'HORTALICAS',        77, 323,  18.0, 2.2, NULL,  1.4, 0.1, 0.0,  36,  true, true),
('00000006-0000-0000-0000-000000000004', 'Batata, inglesa, cozida',             'TACO', 'TACO-604', 'HORTALICAS',        52, 218,  11.9, 1.3, NULL,  1.2, 0.1, 0.0,   5,  true, true),

-- Frutas
('00000007-0000-0000-0000-000000000001', 'Banana, prata',                       'TACO', 'TACO-701', 'FRUTAS',            98, 411,  26.0, 2.0, NULL,  1.3, 0.1, 0.0,   2,  true, true),
('00000007-0000-0000-0000-000000000002', 'Maçã, fuji',                          'TACO', 'TACO-702', 'FRUTAS',            56, 235,  15.2, 1.3, NULL,  0.3, 0.1, 0.0,   1,  true, true),
('00000007-0000-0000-0000-000000000003', 'Morango',                             'TACO', 'TACO-703', 'FRUTAS',            30, 127,   6.6, 1.7, NULL,  0.8, 0.4, 0.0,   1,  true, true),

-- Óleos e Gorduras
('00000008-0000-0000-0000-000000000001', 'Azeite de oliva',                     'TACO', 'TACO-801', 'OLEOS_GORDURAS',   884,3701,   0.0, 0.0, NULL,  0.0,100.0,14.0,   0,  true, true),
('00000008-0000-0000-0000-000000000002', 'Óleo de coco',                        'TACO', 'TACO-802', 'OLEOS_GORDURAS',   862,3607,   0.0, 0.0, NULL,  0.0, 99.9,86.5,   0,  true, true)

ON CONFLICT (source, external_id) DO NOTHING;

-- ── Medidas caseiras para os alimentos TACO mais comuns ───────────────────────

-- Arroz branco cozido
INSERT INTO household_measures (food_id, name, quantity, weight_g, is_default) VALUES
('00000001-0000-0000-0000-000000000001', '1 colher de sopa', 1, 20, false),
('00000001-0000-0000-0000-000000000001', '1 escumadeira',    1, 80, false),
('00000001-0000-0000-0000-000000000001', '1 xícara de chá',  1,160, true)
ON CONFLICT (food_id, name) DO NOTHING;

-- Feijão cozido
INSERT INTO household_measures (food_id, name, quantity, weight_g, is_default) VALUES
('00000002-0000-0000-0000-000000000001', '1 concha',         1, 80, true),
('00000002-0000-0000-0000-000000000001', '1 xícara de chá',  1,180, false)
ON CONFLICT (food_id, name) DO NOTHING;

-- Frango peito grelhado
INSERT INTO household_measures (food_id, name, quantity, weight_g, is_default) VALUES
('00000003-0000-0000-0000-000000000001', '1 filé pequeno',   1, 80, false),
('00000003-0000-0000-0000-000000000001', '1 filé médio',     1,120, true),
('00000003-0000-0000-0000-000000000001', '1 escumadeira',    1, 85, false)
ON CONFLICT (food_id, name) DO NOTHING;

-- Ovo inteiro cozido
INSERT INTO household_measures (food_id, name, quantity, weight_g, is_default) VALUES
('00000005-0000-0000-0000-000000000001', '1 unidade pequena', 1, 40, false),
('00000005-0000-0000-0000-000000000001', '1 unidade média',   1, 50, true),
('00000005-0000-0000-0000-000000000001', '1 unidade grande',  1, 60, false)
ON CONFLICT (food_id, name) DO NOTHING;

-- Aveia flocos
INSERT INTO household_measures (food_id, name, quantity, weight_g, is_default) VALUES
('00000001-0000-0000-0000-000000000004', '1 colher de sopa',  1, 10, false),
('00000001-0000-0000-0000-000000000004', '1 xícara de chá',   1, 80, true)
ON CONFLICT (food_id, name) DO NOTHING;

-- Azeite de oliva
INSERT INTO household_measures (food_id, name, quantity, weight_g, is_default) VALUES
('00000008-0000-0000-0000-000000000001', '1 colher de chá',   1,  5, false),
('00000008-0000-0000-0000-000000000001', '1 colher de sopa',  1, 13, true)
ON CONFLICT (food_id, name) DO NOTHING;

-- Banana prata
INSERT INTO household_measures (food_id, name, quantity, weight_g, is_default) VALUES
('00000007-0000-0000-0000-000000000001', '1 unidade pequena', 1, 80, false),
('00000007-0000-0000-0000-000000000001', '1 unidade média',   1,100, true),
('00000007-0000-0000-0000-000000000001', '1 unidade grande',  1,130, false)
ON CONFLICT (food_id, name) DO NOTHING;

-- ── Sinônimos para melhorar fuzzy search ─────────────────────────────────────

INSERT INTO food_synonyms (food_id, synonym) VALUES
-- Arroz
('00000001-0000-0000-0000-000000000001', 'arroz cozido'),
('00000001-0000-0000-0000-000000000001', 'arroz branco'),
-- Feijão
('00000002-0000-0000-0000-000000000001', 'feijão preto'),
('00000002-0000-0000-0000-000000000002', 'feijão carioca'),
('00000002-0000-0000-0000-000000000002', 'feijão mulatinho'),
-- Frango
('00000003-0000-0000-0000-000000000001', 'peito de frango'),
('00000003-0000-0000-0000-000000000001', 'frango grelhado'),
('00000003-0000-0000-0000-000000000001', 'frango cozido'),
-- Ovo
('00000005-0000-0000-0000-000000000001', 'ovo cozido'),
('00000005-0000-0000-0000-000000000001', 'ovo mexido'),
('00000005-0000-0000-0000-000000000002', 'clara de ovo'),
-- Salmão
('00000004-0000-0000-0000-000000000002', 'salmão assado'),
('00000004-0000-0000-0000-000000000002', 'filé de salmão'),
-- Batata-doce
('00000006-0000-0000-0000-000000000003', 'batata doce'),
('00000006-0000-0000-0000-000000000003', 'batata doce cozida')
ON CONFLICT DO NOTHING;
