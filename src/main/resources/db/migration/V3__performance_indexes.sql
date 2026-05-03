-- =============================================================================
-- FuelMatch :: Nutrition Service — V3
-- Índices adicionais para alta performance de leitura e queries de produção
-- =============================================================================

-- ── Índice composto para busca por categoria + ativo ─────────────────────────
-- Usado em: "listar todos os CEREAIS_GRAOS ativos da TACO"
CREATE INDEX IF NOT EXISTS idx_foods_category_source_active
    ON foods (category, source, is_active)
    WHERE is_active = TRUE;

-- ── Índice parcial para alimentos verificados ─────────────────────────────────
-- Usado em: filtro "mostrar apenas alimentos verificados"
CREATE INDEX IF NOT EXISTS idx_foods_verified
    ON foods (is_verified, source)
    WHERE is_verified = TRUE AND is_active = TRUE;

-- ── Índice para updated_at (cache invalidation queries) ──────────────────────
CREATE INDEX IF NOT EXISTS idx_foods_updated_at
    ON foods (updated_at DESC)
    WHERE is_active = TRUE;

-- ── Índice para import log (monitoramento) ────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_import_log_source_status
    ON food_import_log (source, status, imported_at DESC);

CREATE INDEX IF NOT EXISTS idx_import_log_food_id
    ON food_import_log (food_id)
    WHERE food_id IS NOT NULL;

-- ── Estatísticas do pg_trgm ───────────────────────────────────────────────────
-- Força o planner a atualizar stats após seed do V2
ANALYZE foods;
ANALYZE food_synonyms;
ANALYZE household_measures;

-- ── View materializada para estatísticas de macros por categoria ──────────────
-- Útil para dashboard de nutricionistas
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_category_macro_stats AS
SELECT
    source,
    category,
    COUNT(*)                                    AS food_count,
    ROUND(AVG(energy_kcal)::NUMERIC, 1)         AS avg_kcal,
    ROUND(AVG(proteins_g)::NUMERIC, 1)          AS avg_protein_g,
    ROUND(AVG(carbohydrates_g)::NUMERIC, 1)     AS avg_carbs_g,
    ROUND(AVG(fat_total_g)::NUMERIC, 1)         AS avg_fat_g
FROM foods
WHERE is_active = TRUE
  AND energy_kcal IS NOT NULL
GROUP BY source, category
ORDER BY source, category;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_category_stats
    ON mv_category_macro_stats (source, category);

COMMENT ON MATERIALIZED VIEW mv_category_macro_stats IS
    'Stats agregados de macros por fonte e categoria. Refresh com: REFRESH MATERIALIZED VIEW CONCURRENTLY mv_category_macro_stats';
