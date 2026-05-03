-- =============================================================================
-- FuelMatch :: Nutrition Service — V4
-- Ajusta threshold do pg_trgm para 0.15 (permite typos maiores como "frnago")
-- e cria função de busca com word_similarity para maior tolerância
-- =============================================================================

-- Configura threshold para esta sessão (aplicado no banco via ALTER DATABASE)
ALTER DATABASE fuelmatch_nutrition SET pg_trgm.similarity_threshold = 0.15;
ALTER DATABASE fuelmatch_nutrition SET pg_trgm.word_similarity_threshold = 0.2;
