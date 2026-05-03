-- =============================================================================
-- FuelMatch :: Nutrition Service — Schema V1
-- Engine: PostgreSQL 16+
-- Features: pg_trgm (fuzzy/trigram search), JSONB (micronutrients), partitioning
-- =============================================================================

-- ── Extensions ───────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";       -- fuzzy search / similarity
CREATE EXTENSION IF NOT EXISTS "unaccent";      -- remove diacritics on search

-- Wrapper IMMUTABLE para unaccent (necessário para GENERATED ALWAYS AS e índices funcionais)
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
RETURNS text AS $$
    SELECT unaccent($1);
$$ LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE;

-- ── Enum Types ────────────────────────────────────────────────────────────────
CREATE TYPE food_source AS ENUM (
    'TACO',           -- Tabela Brasileira de Composição de Alimentos
    'OPEN_FOOD_FACTS', -- importado via API + cached
    'CUSTOM'          -- cadastrado por nutricionista
);

CREATE TYPE food_category AS ENUM (
    'CEREAIS_GRAOS',
    'LEGUMINOSAS',
    'HORTALICAS',
    'FRUTAS',
    'CARNES_AVES',
    'CARNES_BOVINAS',
    'PESCADOS',
    'OVOS_LATICINIOS',
    'ACUCARES_DOCES',
    'OLEOS_GORDURAS',
    'BEBIDAS',
    'ALIMENTOS_PREPARADOS',
    'INDUSTRIALIZADOS',
    'SUPLEMENTOS',
    'OUTROS'
);

-- ── Core Table: foods ─────────────────────────────────────────────────────────
-- Unificada — alimentos TACO, Open Food Facts e custom vivem aqui.
-- Macros sempre normalizados para 100g/100ml (base de cálculo).
CREATE TABLE foods (
    id                      UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Identificação
    name                    VARCHAR(300)    NOT NULL,
    name_unaccented         VARCHAR(300),   -- SEM coluna gerada — será populada via trigger
    brand                   VARCHAR(200),
    barcode                 VARCHAR(50),                     -- EAN-13 / EAN-8
    source                  food_source     NOT NULL,
    external_id             VARCHAR(100),                    -- id na TACO ou OFF
    category                food_category,

    -- ── Macronutrientes por 100g ────────────────────────────────────────────
    -- REGRA: TODOS os valores nutricionais são armazenados por 100g ou 100ml.
    -- O motor de cálculo faz a proporção com base no peso/volume informado.
    energy_kcal             NUMERIC(8, 2),
    energy_kj               NUMERIC(8, 2),
    carbohydrates_g         NUMERIC(8, 2),
    of_which_sugars_g       NUMERIC(8, 2),
    of_which_fiber_g        NUMERIC(8, 2),
    proteins_g              NUMERIC(8, 2),
    fat_total_g             NUMERIC(8, 2),
    of_which_saturated_g    NUMERIC(8, 2),
    of_which_trans_g        NUMERIC(8, 2),
    sodium_mg               NUMERIC(8, 2),

    -- ── Micronutrientes (flexível via JSONB) ───────────────────────────────
    -- Exemplo: {"calcium_mg": 120, "iron_mg": 2.5, "vitamin_c_mg": 30}
    micronutrients          JSONB           DEFAULT '{}'::jsonb,

    -- ── Metadados Comerciais ───────────────────────────────────────────────
    ingredients_text        TEXT,
    allergens               TEXT[],
    serving_size_g          NUMERIC(8, 2),   -- porção do rótulo em gramas
    serving_description     VARCHAR(100),    -- ex: "1 fatia", "1 unidade"
    image_url               VARCHAR(500),

    -- ── Controle ───────────────────────────────────────────────────────────
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    is_verified             BOOLEAN         NOT NULL DEFAULT FALSE,
    tenant_id               UUID,                            -- NULL = global
    created_by              UUID,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ────────────────────────────────────────────────────────
    CONSTRAINT uq_foods_external        UNIQUE (source, external_id),
    CONSTRAINT uq_foods_barcode         UNIQUE (barcode),
    CONSTRAINT chk_energy_positive      CHECK (energy_kcal IS NULL OR energy_kcal >= 0),
    CONSTRAINT chk_protein_positive     CHECK (proteins_g IS NULL OR proteins_g >= 0),
    CONSTRAINT chk_carb_positive        CHECK (carbohydrates_g IS NULL OR carbohydrates_g >= 0),
    CONSTRAINT chk_fat_positive         CHECK (fat_total_g IS NULL OR fat_total_g >= 0)
);

-- ── Household Measures: medidas_caseiras ──────────────────────────────────────
-- Cada linha é uma medida caseira para um alimento.
-- weight_g: quantos gramas essa medida representa.
CREATE TABLE household_measures (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    food_id         UUID            NOT NULL REFERENCES foods(id) ON DELETE CASCADE,
    name            VARCHAR(100)    NOT NULL,   -- "1 colher de sopa", "1 xícara"
    quantity        NUMERIC(6, 2)   NOT NULL DEFAULT 1,  -- multiplicador da medida
    weight_g        NUMERIC(8, 2)   NOT NULL,   -- peso em gramas desta medida * quantity
    is_default      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_measure_food_name UNIQUE (food_id, name),
    CONSTRAINT chk_weight_positive  CHECK (weight_g > 0),
    CONSTRAINT chk_qty_positive     CHECK (quantity > 0)
);

-- ── Food Synonyms: para melhorar o fuzzy search ───────────────────────────────
CREATE TABLE food_synonyms (
    id                  UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    food_id             UUID         NOT NULL REFERENCES foods(id) ON DELETE CASCADE,
    synonym             VARCHAR(300) NOT NULL,
    synonym_unaccented  VARCHAR(300)
);

-- ── Audit Log: food_import_log ────────────────────────────────────────────────
CREATE TABLE food_import_log (
    id              BIGSERIAL       PRIMARY KEY,
    food_id         UUID            REFERENCES foods(id),
    source          food_source     NOT NULL,
    external_id     VARCHAR(100),
    status          VARCHAR(20)     NOT NULL,   -- SUCCESS, FAILED, DUPLICATE
    error_message   TEXT,
    imported_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- ÍNDICES DE PERFORMANCE
-- =============================================================================

-- Trigram GIN para fuzzy search em nome (core do serviço)
CREATE INDEX idx_foods_name_trgm
    ON foods USING GIN (name_unaccented gin_trgm_ops);

-- Trigram nos sinônimos
CREATE INDEX idx_synonyms_trgm
    ON food_synonyms USING GIN (synonym_unaccented gin_trgm_ops);

-- Busca por barcode (frequente em comerciais)
CREATE INDEX idx_foods_barcode
    ON foods (barcode) WHERE barcode IS NOT NULL;

-- Busca por source (filtros TACO vs OFF vs CUSTOM)
CREATE INDEX idx_foods_source
    ON foods (source, is_active);

-- Busca por tenant (itens customizados)
CREATE INDEX idx_foods_tenant
    ON foods (tenant_id, is_active) WHERE tenant_id IS NOT NULL;

-- JSONB para consultas de micronutrientes específicos
CREATE INDEX idx_foods_micronutrients
    ON foods USING GIN (micronutrients);

-- Household measures lookup
CREATE INDEX idx_measures_food_id
    ON household_measures (food_id, is_default);

-- =============================================================================
-- TRIGGERS
-- =============================================================================

-- Auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_foods_updated_at
    BEFORE UPDATE ON foods
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Garante apenas uma medida default por alimento
CREATE OR REPLACE FUNCTION ensure_single_default_measure()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default THEN
        UPDATE household_measures
        SET is_default = FALSE
        WHERE food_id = NEW.food_id AND id != NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_single_default_measure
    BEFORE INSERT OR UPDATE ON household_measures
    FOR EACH ROW WHEN (NEW.is_default = TRUE)
    EXECUTE FUNCTION ensure_single_default_measure();

-- Trigger para manter name_unaccented sincronizado
CREATE OR REPLACE FUNCTION sync_food_name_unaccented()
RETURNS TRIGGER AS $$
BEGIN
    NEW.name_unaccented := immutable_unaccent(lower(NEW.name));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_foods_name_unaccented
    BEFORE INSERT OR UPDATE OF name ON foods
    FOR EACH ROW EXECUTE FUNCTION sync_food_name_unaccented();

-- Trigger para manter synonym_unaccented sincronizado
CREATE OR REPLACE FUNCTION sync_synonym_unaccented()
RETURNS TRIGGER AS $$
BEGIN
    NEW.synonym_unaccented := immutable_unaccent(lower(NEW.synonym));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_synonym_unaccented
    BEFORE INSERT OR UPDATE OF synonym ON food_synonyms
    FOR EACH ROW EXECUTE FUNCTION sync_synonym_unaccented();

-- =============================================================================
-- FUNÇÃO DE BUSCA FUZZY (usada como fallback se o app não suportar pg_trgm)
-- =============================================================================
CREATE OR REPLACE FUNCTION search_foods(
    p_query         TEXT,
    p_limit         INT     DEFAULT 20,
    p_offset        INT     DEFAULT 0,
    p_source        food_source DEFAULT NULL,
    p_tenant_id     UUID    DEFAULT NULL,
    p_min_similarity FLOAT  DEFAULT 0.2
)
RETURNS TABLE (
    id              UUID,
    name            VARCHAR,
    brand           VARCHAR,
    source          food_source,
    energy_kcal     NUMERIC,
    proteins_g      NUMERIC,
    carbohydrates_g NUMERIC,
    fat_total_g     NUMERIC,
    similarity_score FLOAT
) AS $$
DECLARE
    v_query TEXT := immutable_unaccent(lower(trim(p_query)));
BEGIN
    RETURN QUERY
    SELECT DISTINCT ON (f.id)
        f.id,
        f.name,
        f.brand,
        f.source,
        f.energy_kcal,
        f.proteins_g,
        f.carbohydrates_g,
        f.fat_total_g,
        GREATEST(
            similarity(f.name_unaccented, v_query),
            COALESCE((
                SELECT MAX(similarity(fs.synonym_unaccented, v_query))
                FROM food_synonyms fs WHERE fs.food_id = f.id
            ), 0)
        ) AS similarity_score
    FROM foods f
    WHERE f.is_active = TRUE
      AND (p_source IS NULL OR f.source = p_source)
      AND (p_tenant_id IS NULL OR f.tenant_id IS NULL OR f.tenant_id = p_tenant_id)
      AND (
            f.name_unaccented % v_query
            OR EXISTS (
                SELECT 1 FROM food_synonyms fs
                WHERE fs.food_id = f.id AND fs.synonym_unaccented % v_query
            )
      )
    ORDER BY f.id, similarity_score DESC
    LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql STABLE PARALLEL SAFE;

COMMENT ON TABLE foods IS 'Tabela unificada de alimentos: TACO (in natura), Open Food Facts (industrializados) e CUSTOM (nutricionistas). Macros sempre em base 100g.';
COMMENT ON TABLE household_measures IS 'Medidas caseiras por alimento (colher, xícara, unidade). weight_g é o peso em gramas da medida informada.';
COMMENT ON COLUMN foods.micronutrients IS 'JSONB flexível para micronutrientes: calcium_mg, iron_mg, vitamin_c_mg, etc. Evita proliferação de colunas nullable.';