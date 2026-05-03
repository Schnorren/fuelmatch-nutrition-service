# FuelMatch · Nutrition Service

> Motor de busca e cálculo de macronutrientes para a plataforma FuelMatch — SaaS de nutrição esportiva com periodização dinâmica de macros.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green?logo=spring)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)

---

## Sumário

- [O que é](#o-que-é)
- [Demo rápida](#demo-rápida)
- [Arquitetura](#arquitetura)
- [Banco de Dados](#banco-de-dados)
- [Como Funciona](#como-funciona)
- [API Reference](#api-reference)
- [Stack](#stack)
- [Como Rodar Localmente](#como-rodar-localmente)
- [Deploy em Produção](#deploy-em-produção)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Dados da TACO](#dados-da-taco)

---

## O que é

O **Nutrition Service** é um microsserviço independente responsável por:

- **Banco de dados nutricional unificado** — une a Tabela TACO (597 alimentos in natura brasileiros) com a Open Food Facts (industrializados) e itens customizados por nutricionistas
- **Fuzzy search** — encontra "frango grelhado" mesmo que o usuário digite "frnago" ou "feijao" sem acento, usando trigram similarity nativa do PostgreSQL
- **Motor de cálculo de macros** — recebe qualquer input ("250g", "2 colheres de sopa", "1 filé médio") e retorna macronutrientes + micronutrientes proporcionalmente calculados
- **Cálculo em batch** — calcula macros de uma refeição inteira em uma requisição, retornando totais agregados
- **Cache inteligente** — alimentos da Open Food Facts são salvos localmente em background; próximas buscas são instantâneas

---

## Demo rápida

```bash
# Busca com tolerância a erros de digitação
curl "https://seu-servico.onrender.com/api/v1/foods/search?q=frnago&source=TACO"

# Calcular macros de 250g de frango grelhado
curl -X POST https://seu-servico.onrender.com/api/v1/foods/calculate \
  -H "Content-Type: application/json" \
  -d '{"foodId":"<uuid>","portionInput":"250g"}'

# Calcular uma refeição completa
curl -X POST https://seu-servico.onrender.com/api/v1/foods/calculate/batch \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"foodId":"<uuid-frango>","portionInput":"150g","correlationId":"proteina"},
      {"foodId":"<uuid-arroz>","portionInput":"1 xícara de chá","correlationId":"carboidrato"},
      {"foodId":"<uuid-brocolis>","portionInput":"100g","correlationId":"vegetal"}
    ]
  }'
```

---

## Arquitetura

O serviço segue **Hexagonal Architecture (Ports & Adapters)** — a lógica de negócio é completamente independente de frameworks, banco de dados e integrações externas.

```
fuelmatch-nutrition-service/
│
├── api/                          # Camada de entrada (HTTP)
│   ├── controller/               # FoodController, FoodAdminController
│   ├── advice/                   # GlobalExceptionHandler (RFC 7807)
│   └── dto/
│       ├── request/              # CreateFoodRequest, CalculateMacrosRequest, BatchCalculateRequest
│       └── response/             # FoodResponse, MacroResultResponse, BatchCalculateResponse
│
├── application/                  # Lógica de negócio (zero dependências de framework)
│   ├── service/
│   │   ├── FoodSearchService     # Busca local + fallback OFF + deduplicação
│   │   ├── FoodPersistenceService# Cache assíncrono (fire-and-forget)
│   │   ├── MacroCalculatorService# Motor de cálculo proporcional
│   │   └── MacroAggregatorService# Orquestração de batch + totais
│   ├── port/
│   │   └── FoodRepository        # Interface (Output Port) — sem JPA aqui
│   └── mapper/
│       └── FoodMapper            # MapStruct: domain <-> entity <-> dto
│
├── domain/                       # Modelos de domínio puros (sem anotações de framework)
│   └── model/
│       ├── Food                  # @Value imutável
│       ├── HouseholdMeasure      # Medidas caseiras
│       └── MacroResult           # Resultado de cálculo
│
└── infrastructure/               # Implementações concretas
    ├── persistence/
    │   ├── entity/               # FoodEntity, HouseholdMeasureEntity (JPA)
    │   ├── repository/           # FoodJpaRepository (Spring Data)
    │   └── adapter/              # FoodRepositoryAdapter (implementa o port)
    ├── integration/
    │   ├── openfoodfacts/        # OpenFoodFactsClient (WebClient reativo)
    │   └── taco/                 # TacoImportService (importação batch CSV)
    └── config/                   # AsyncConfig, CacheConfig, WebClientConfig
```

**Por que Hexagonal?**
- A camada `application` nunca importa JPA, Redis ou HTTP — só interfaces
- Trocar PostgreSQL por outro banco não toca em nenhuma regra de negócio
- Testes unitários dos services são puros, sem necessidade de banco real

---

## Banco de Dados

### Modelo de dados

```
foods (tabela unificada)
├── id, name, brand, barcode
├── source: TACO | OPEN_FOOD_FACTS | CUSTOM
├── category: CEREAIS_GRAOS | HORTALICAS | FRUTAS | CARNES_BOVINAS | ...
│
├── Macros por 100g (colunas dedicadas, alta performance)
│   ├── energy_kcal, energy_kj
│   ├── proteins_g, carbohydrates_g, fat_total_g
│   ├── of_which_fiber_g, of_which_sugars_g, of_which_saturated_g
│   └── sodium_mg
│
├── micronutrients: JSONB  ← cálcio, ferro, zinco, potássio, vitaminas...
│                             flexível, sem proliferação de colunas nullable
│
└── tenant_id  ← NULL = alimento global | UUID = alimento do nutricionista

household_measures
└── food_id -> name -> weight_g
    ex: "1 colher de sopa" = 15g | "1 filé médio" = 120g

food_synonyms
└── food_id -> synonym
    ex: "frango" -> "peito de frango", "frango grelhado"
```

### Decisões de Modelagem

| Decisão | Motivo |
|---|---|
| Tabela única para TACO + OFF + CUSTOM | Queries de busca simples, sem UNION |
| Macros sempre em base **100g** | Cálculo proporcional uniforme para qualquer porção |
| `micronutrients` como **JSONB** | 18 nutrientes opcionais sem proliferar colunas nullable |
| `pg_trgm` para fuzzy search | Sem Elasticsearch — tudo no PostgreSQL |
| `unaccent` + `immutable_unaccent()` | "feijao" encontra "feijão" |
| `word_similarity()` no WHERE | Tolerância a typos maiores que o operador `%` padrão |

### Extensões PostgreSQL utilizadas

```sql
CREATE EXTENSION pg_trgm;    -- trigram similarity (fuzzy search)
CREATE EXTENSION unaccent;   -- remoção de acentos
CREATE EXTENSION uuid-ossp;  -- geração de UUIDs
```

---

## Como Funciona

### Motor de Busca — Fallback Chain

```
GET /api/v1/foods/search?q=whey+protein
         │
         ▼
 ┌───────────────┐
 │  Redis Cache  │──── HIT ──────────────────────────-> Resposta (< 1ms)
 └───────────────┘
         │ MISS
         ▼
 ┌───────────────────────────────┐
 │  PostgreSQL — Fuzzy Search    │
 │  word_similarity + pg_trgm    │──── >= 3 resultados ──-> Resposta (< 50ms)
 │  (tolera typos e sem acentos) │
 └───────────────────────────────┘
         │ < 3 resultados
         ▼
 ┌───────────────────────────────┐
 │  Open Food Facts API          │──── não encontrado ──-> Retorna local
 │  timeout: 5s | retry: 2x     │
 └───────────────────────────────┘
         │ encontrado
         ├──-> Retorna resultado (síncrono)
         └──-> Background Job: salva no banco local (assíncrono)
```

O background job usa um pool dedicado (`offPersistenceExecutor`) e **não bloqueia a resposta ao usuário**.

---

### Motor de Cálculo de Macros

Todos os valores nutricionais são armazenados em base **100g**. O cálculo é uma regra de três simples:

```
macro_resultado = (macro_por_100g × peso_g) / 100
```

O serviço resolve automaticamente qualquer formato de porção para gramas:

| Input do usuário | Como é resolvido | Exemplo |
|---|---|---|
| `"250g"` / `"250 gramas"` | Gramas explícitos | 250g |
| `"250"` | Número puro → assume gramas | 250g |
| `"2 colheres de sopa"` | Lookup em `household_measures` | 2 × 15g = 30g |
| `"1 filé médio"` | Lookup em `household_measures` | 1 × 120g = 120g |
| `"1 porção"` / `"2 servings"` | Usa `serving_size_g` do alimento | depende do produto |

Micronutrientes do JSONB também são calculados proporcionalmente:

```json
{
  "foodName": "Acerola, crua",
  "calculatedWeightG": 200.00,
  "energyKcal": 66.00,
  "micronutrients": {
    "vitamin_c_mg": 1882.80,
    "calcium_mg": 26.00,
    "iron_mg": 0.40,
    "potassium_mg": 330.00
  }
}
```

---

### Open Food Facts Fallback

Quando um alimento industrializado não é encontrado localmente:

1. Consulta a [Open Food Facts](https://world.openfoodfacts.org/) — base colaborativa com +3 milhões de produtos
2. Retorna resultado imediatamente ao usuário
3. Salva no banco local **assincronamente** — próximas buscas são instantâneas
4. **Graceful degradation**: se a OFF estiver indisponível, retorna resultados locais sem erro

---

## API Reference

Base URL local: `http://localhost:8080`

Todos os erros seguem o padrão **RFC 7807 (Problem Details)**:

```json
{
  "type": "about:blank",
  "title": "Porção inválida",
  "status": 422,
  "detail": "Não foi possível interpretar: 'uma porção gigante'.",
  "hint": "Exemplos válidos: '250g', '2 colheres de sopa', '1 porção'",
  "timestamp": "2026-05-03T10:00:00-03:00"
}
```

---

### Busca de Alimentos

#### `GET /api/v1/foods/search`

Busca alimentos com fuzzy search. Se não encontrar localmente, consulta a Open Food Facts.

**Query Parameters**

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `q` | string | Sim | Termo de busca (suporta typos e sem acentos) |
| `source` | enum | Não | Filtrar por `TACO`, `OPEN_FOOD_FACTS` ou `CUSTOM` |
| `tenantId` | UUID | Não | Incluir alimentos customizados do nutricionista |
| `page` | int | Não | Página (default: 0) |
| `size` | int | Não | Itens por página (default: 20, máx: 50) |

**Exemplos**

```bash
# Busca simples
curl "http://localhost:8080/api/v1/foods/search?q=frango"

# Com typo (encontra "frango" mesmo assim)
curl "http://localhost:8080/api/v1/foods/search?q=frnago&source=TACO"

# Sem acento (encontra "feijão")
curl "http://localhost:8080/api/v1/foods/search?q=feijao&source=TACO"

# Industrializado com fallback OFF
curl "http://localhost:8080/api/v1/foods/search?q=whey+protein"

# Com paginação
curl "http://localhost:8080/api/v1/foods/search?q=frango&page=0&size=10"
```

**Response `200 OK`**

```json
{
  "content": [
    {
      "id": "d3e6c359-382b-453a-a6cd-06c404668d1d",
      "name": "Frango, peito, sem pele, grelhado",
      "source": "TACO",
      "category": "CARNES_BOVINAS",
      "energyKcal": 159.00,
      "proteinsG": 32.00,
      "carbohydratesG": 0.00,
      "fatTotalG": 3.20,
      "sodiumMg": 74.00,
      "micronutrients": { "iron_mg": 0.4, "zinc_mg": 1.5 },
      "verified": true,
      "measures": [
        { "name": "1 filé médio", "weightG": 120.00, "defaultMeasure": true },
        { "name": "1 escumadeira", "weightG": 85.00, "defaultMeasure": false }
      ]
    }
  ],
  "totalElements": 12,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

---

#### `GET /api/v1/foods/{id}`

Retorna um alimento pelo ID com todas as medidas caseiras.

```bash
curl "http://localhost:8080/api/v1/foods/d3e6c359-382b-453a-a6cd-06c404668d1d"
```

**Responses:**
- `200 OK` — mesmo formato do item dentro de `/search`
- `404 Not Found` — alimento não encontrado ou desativado

---

#### `GET /api/v1/foods/barcode/{barcode}`

Busca por código de barras EAN-8 ou EAN-13. Se não encontrar localmente, consulta a Open Food Facts.

```bash
curl "http://localhost:8080/api/v1/foods/barcode/7891000100103"
```

**Responses:**
- `200 OK` — alimento encontrado
- `400 Bad Request` — barcode com formato inválido (deve ter 8–13 dígitos)
- `404 Not Found` — não encontrado localmente nem na Open Food Facts

---

### Cálculo de Macros

#### `POST /api/v1/foods/calculate`

Calcula macronutrientes e micronutrientes para a porção informada.

**Request Body**

```json
{
  "foodId": "d3e6c359-382b-453a-a6cd-06c404668d1d",
  "portionInput": "250g"
}
```

**Formatos aceitos em `portionInput`**

```
"250g"               -> 250 gramas
"250 gramas"         -> 250 gramas
"250"                -> 250 gramas (número puro assume gramas)
"2 colheres de sopa" -> resolve pela medida caseira cadastrada
"1 filé médio"       -> resolve pela medida caseira cadastrada
"1 porção"           -> usa serving_size_g do alimento
"2 servings"         -> 2x serving_size_g
```

**Exemplos**

```bash
# Por gramas
curl -X POST http://localhost:8080/api/v1/foods/calculate \
  -H "Content-Type: application/json" \
  -d '{"foodId":"d3e6c359-...","portionInput":"250g"}'

# Por medida caseira
curl -X POST http://localhost:8080/api/v1/foods/calculate \
  -H "Content-Type: application/json" \
  -d '{"foodId":"d3e6c359-...","portionInput":"1 filé médio"}'
```

**Response `200 OK`**

```json
{
  "foodId": "d3e6c359-...",
  "foodName": "Frango, peito, sem pele, grelhado",
  "calculatedWeightG": 250.00,
  "portionDescription": "250g",
  "energyKcal": 397.50,
  "energyKj": 1662.50,
  "proteinsG": 80.00,
  "carbohydratesG": 0.00,
  "fatTotalG": 8.00,
  "ofWhichSaturatedG": 2.25,
  "sodiumMg": 185.00,
  "micronutrients": {
    "iron_mg": 1.00,
    "zinc_mg": 3.75,
    "potassium_mg": 875.00
  }
}
```

**Responses de erro:**
- `400 Bad Request` — `foodId` ou `portionInput` ausentes
- `404 Not Found` — alimento não encontrado
- `422 Unprocessable Entity` — porção não pode ser interpretada

---

### Cálculo em Batch

#### `POST /api/v1/foods/calculate/batch`

Calcula macros de até 50 alimentos em uma requisição e retorna os **totais agregados da refeição**. Itens com erro não cancelam o batch.

**Request Body**

```json
{
  "items": [
    {
      "foodId": "d3e6c359-...",
      "portionInput": "150g",
      "correlationId": "proteina-principal"
    },
    {
      "foodId": "cf6a3961-...",
      "portionInput": "1 xícara de chá",
      "correlationId": "carboidrato"
    }
  ]
}
```

**Response `200 OK`**

```json
{
  "items": [
    {
      "correlationId": "proteina-principal",
      "success": true,
      "macros": {
        "foodName": "Frango, peito, sem pele, grelhado",
        "calculatedWeightG": 150.00,
        "energyKcal": 238.50,
        "proteinsG": 48.00,
        "carbohydratesG": 0.00,
        "fatTotalG": 4.80
      }
    },
    {
      "correlationId": "carboidrato",
      "success": true,
      "macros": {
        "foodName": "Arroz, branco, cozido",
        "calculatedWeightG": 160.00,
        "energyKcal": 204.80,
        "proteinsG": 4.00,
        "carbohydratesG": 44.96
      }
    }
  ],
  "totals": {
    "totalEnergyKcal": 443.30,
    "totalProteinsG": 52.00,
    "totalCarbohydratesG": 44.96,
    "totalFatTotalG": 4.80,
    "totalFiberG": 1.28,
    "totalSodiumMg": 112.60
  },
  "errorCount": 0
}
```

---

### Alimentos Customizados

#### `POST /api/v1/foods`

Cadastra um alimento customizado (receita própria, produto local). Valores nutricionais informados por **100g**.

**Request Body**

```json
{
  "name": "Shake Pré-Treino Caseiro",
  "energyKcal": 280,
  "proteinsG": 30,
  "carbohydratesG": 35,
  "fatTotalG": 4,
  "servingSizeG": 350,
  "servingDescription": "1 copo (350ml)",
  "category": "SUPLEMENTOS",
  "measures": [
    { "name": "1 copo", "weightG": 350, "defaultMeasure": true }
  ]
}
```

**Response `201 Created`** — alimento criado com `source: CUSTOM` e `verified: false`

---

#### `DELETE /api/v1/foods/{id}`

Desativa um alimento (soft delete — o registro não é removido do banco).

**Response `204 No Content`**

---

### Admin

> **Atenção:** estes endpoints devem ser protegidos por autenticação em produção. A implementação de Spring Security + JWT está planejada para a próxima versão.

#### `POST /api/v1/admin/import/taco`

Importa todos os 597 alimentos da Tabela TACO. Operação idempotente — alimentos já existentes são ignorados.

```bash
curl -X POST http://localhost:8080/api/v1/admin/import/taco
```

**Response `200 OK`**

```json
{
  "status": "completed",
  "total": 597,
  "saved": 581,
  "skipped": 16,
  "failed": 0
}
```

#### `POST /api/v1/admin/cache/evict`

Invalida todos os caches Redis.

```bash
curl -X POST http://localhost:8080/api/v1/admin/cache/evict
```

#### `DELETE /api/v1/admin/cache/food/{id}`

Invalida o cache de um alimento específico.

---

### Observabilidade

| Endpoint | Descrição |
|---|---|
| `GET /actuator/health` | Status da aplicação (`{"status":"UP"}`) |
| `GET /actuator/info` | Informações da build |
| `GET /actuator/prometheus` | Métricas no formato Prometheus |

**Métricas disponíveis:**
- `http_server_requests_seconds` — latência por endpoint
- `cache_gets_total` — hits e misses do Redis por cache
- `hikaricp_connections` — pool de conexões do banco

---

## Stack

| Camada | Tecnologia | Versão |
|---|---|---|
| Runtime | Java / Spring Boot | 21 / 3.3 |
| Banco de dados | PostgreSQL | 16 |
| Fuzzy search | pg_trgm (nativo PostgreSQL) | — |
| Migrations | Flyway | 10 |
| Cache | Redis (Spring Cache) | 7 |
| HTTP Client | Spring WebFlux (WebClient) | — |
| ORM | Spring Data JPA / Hibernate | 6.5 |
| Mapeamento | MapStruct | 1.5.5 |
| Observabilidade | Micrometer + Prometheus | — |
| Testes | JUnit 5 + Testcontainers | — |

---

## Como Rodar Localmente

**Pré-requisitos:** Java 21+, Docker, Maven 3.9+

```bash
# 1. Clone
git clone https://github.com/Schnorren/fuelmatch-nutrition-service.git
cd fuelmatch-nutrition-service

# 2. Sobe PostgreSQL + Redis
docker-compose up postgres redis -d

# 3. Roda a aplicação
./mvnw spring-boot:run

# 4. Importa a Tabela TACO (597 alimentos)
curl -X POST http://localhost:8080/api/v1/admin/import/taco

# 5. Testa
curl "http://localhost:8080/api/v1/foods/search?q=frango&source=TACO"
```

**Ou tudo em Docker:**

```bash
docker-compose up --build
```

**Script de testes de integração (58 casos):**

```bash
chmod +x test_fuelmatch.sh
./test_fuelmatch.sh
# Resultado esperado: 58/58 Todos os testes passaram!
```

---

## Deploy em Produção

### Render + Supabase

> **Sim, o serviço funciona publicamente no Render + Supabase.** Qualquer cliente (frontend, mobile, Postman) consegue fazer consultas com dois ajustes:

**Ajuste 1 — Ativar extensões no Supabase**

Antes da primeira migration, ative no painel **Database → Extensions**:
- `uuid-ossp`
- `pg_trgm`
- `unaccent`

**Ajuste 2 — Redis**

O Render oferece Redis como serviço gerenciado. Crie um Redis e configure as variáveis abaixo. Para desabilitar temporariamente:

```
SPRING_CACHE_TYPE=none
```

**Configuração no Render** (Environment Variables):

```
DB_URL=jdbc:postgresql://db.seuproject.supabase.co:5432/postgres
DB_USER=postgres
DB_PASS=sua-senha-supabase
REDIS_HOST=seu-redis.onrender.com
REDIS_PORT=6379
REDIS_PASS=senha-do-redis
PORT=8080
```

Após o deploy, execute a importação da TACO:

```bash
curl -X POST https://seu-servico.onrender.com/api/v1/admin/import/taco
```

---

## Variáveis de Ambiente

| Variável | Descrição | Default |
|---|---|---|
| `DB_URL` | JDBC URL do PostgreSQL | `jdbc:postgresql://localhost:5432/fuelmatch_nutrition` |
| `DB_USER` | Usuário do banco | `nutrition` |
| `DB_PASS` | Senha do banco | `nutrition` |
| `REDIS_HOST` | Host do Redis | `localhost` |
| `REDIS_PORT` | Porta do Redis | `6379` |
| `REDIS_PASS` | Senha do Redis (opcional) | _(vazio)_ |
| `PORT` | Porta HTTP | `8080` |
| `OFF_BASE_URL` | Base URL da Open Food Facts | `https://world.openfoodfacts.org` |
| `OFF_TIMEOUT_SECONDS` | Timeout chamadas à OFF | `5` |
| `OFF_ENABLED` | Habilitar fallback OFF | `true` |
| `SPRING_CACHE_TYPE` | Tipo de cache (`redis` ou `none`) | `redis` |

---

## Dados da TACO

O serviço inclui a **Tabela TACO 4ª Edição** (UNICAMP/NEPA) — referência oficial de composição de alimentos brasileiros.

| Categoria | Alimentos |
|---|---|
| Carnes e derivados | 123 |
| Verduras e hortaliças | 99 |
| Frutas e derivados | 96 |
| Cereais e derivados | 63 |
| Alimentos preparados | 32 |
| Leguminosas e derivados | 30 |
| Leite, ovos e derivados | 31 |
| Pescados e frutos do mar | 50 |
| Açúcares e doces | 20 |
| Gorduras e óleos | 14 |
| Bebidas | 14 |
| Outros | 25 |
| **Total** | **597** |

**Macros por alimento** (colunas dedicadas):
`energia (kcal/kJ)`, `proteínas`, `lipídeos`, `carboidratos`, `fibra`, `sódio`

**Micronutrientes por alimento** (JSONB, quando disponíveis na TACO):
`cálcio`, `magnésio`, `manganês`, `fósforo`, `ferro`, `potássio`, `cobre`, `zinco`,
`retinol`, `RE`, `RAE`, `tiamina`, `riboflavina`, `piridoxina`, `niacina`, `vitamina C`,
`colesterol`, `cinzas`

---

## Licença

MIT © FuelMatch
