# FuelMatch :: Nutrition Service

Microsserviço de banco de dados nutricionais — motor de busca e cálculo de macros para a plataforma **FuelMatch** (SaaS de nutrição esportiva com periodização dinâmica de macronutrientes).

## Stack

| Camada | Tecnologia |
|--------|------------|
| Runtime | Java 21 / Spring Boot 3.3 |
| Banco de dados | PostgreSQL 16 (pg_trgm, JSONB, unaccent) |
| Migrations | Flyway |
| Cache | Redis 7 (Spring Cache) |
| HTTP Client | Spring WebFlux (WebClient) |
| ORM | Spring Data JPA / Hibernate 6 |
| Mapeamento | MapStruct |
| Observabilidade | Micrometer + Prometheus |

---

## Arquitetura

```
fuelmatch-nutrition-service/
├── api/
│   ├── controller/         ← Controllers REST (FoodController)
│   ├── advice/             ← GlobalExceptionHandler (RFC 7807)
│   └── dto/
│       ├── request/        ← CreateFoodRequest, CalculateMacrosRequest
│       └── response/       ← FoodResponse, MacroResultResponse
│
├── application/
│   ├── service/            ← Lógica de negócio
│   │   ├── FoodSearchService       ← Busca + fallback OFF + dedup
│   │   ├── FoodPersistenceService  ← Cache assíncrono (fire-and-forget)
│   │   └── MacroCalculatorService  ← Motor de cálculo de macros
│   ├── port/               ← Interfaces (Hexagonal: Output Ports)
│   │   └── FoodRepository
│   └── mapper/             ← MapStruct mappers
│
├── domain/
│   └── model/              ← Objetos de domínio puros (sem framework)
│       ├── Food
│       ├── HouseholdMeasure
│       └── MacroResult
│
└── infrastructure/
    ├── persistence/
    │   ├── entity/         ← JPA Entities (FoodEntity, HouseholdMeasureEntity)
    │   ├── repository/     ← Spring Data JPA Repositories
    │   └── adapter/        ← FoodRepositoryAdapter (implementa o port)
    ├── integration/
    │   └── openfoodfacts/  ← OpenFoodFactsClient + DTOs
    └── cache/              ← CacheConfig (Redis)
```

### Princípios Aplicados

- **Hexagonal Architecture (Ports & Adapters)**: A camada `application` não depende de JPA ou HTTP. Depende apenas de interfaces (`FoodRepository` port). Isso permite trocar implementações sem mudar lógica de negócio.
- **Domain Isolation**: `Food`, `HouseholdMeasure` e `MacroResult` são objetos de domínio puros (imutáveis via Lombok `@Value`), sem anotações de framework.
- **Async Non-blocking**: A persistência de itens da Open Food Facts usa `@Async` com pool dedicado, liberando a thread de request imediatamente.

---

## Banco de Dados

### Decisões de Modelagem

**Tabela única `foods`** para TACO, Open Food Facts e CUSTOM:
- Simplifica as queries de busca (não precisa UNION)
- Diferenciados pelo campo `source` (enum)
- Alimentos customizados de tenant identificados por `tenant_id`

**Macros sempre em base 100g**:
- Permite cálculo proporcional uniforme independente da fonte
- Alimentos com porção de rótulo (ex: OFF) salvam também `serving_size_g`

**`micronutrients JSONB`**:
- Evita dezenas de colunas nullable para vitaminas, minerais etc.
- Consultável via `@>` e GIN index

**`pg_trgm` (trigram similarity)**:
- Fuzzy search nativa no PostgreSQL sem Elasticsearch
- Suporta erros de digitação: "frango" encontra "Frango grelhado", "frangão", etc.
- `GIN index` em `name_unaccented` (com `unaccent`) para alta performance

---

## Fluxo de Busca (Fallback Chain)

```
GET /api/v1/foods/search?q=whey+protein
        │
        ▼
[1] Cache Redis  ─── HIT ──→  Retorna resposta
        │ MISS
        ▼
[2] PostgreSQL Fuzzy Search (pg_trgm)
        │
        ├── ≥3 resultados ──→  Retorna resultados locais
        │
        └── <3 resultados (alimento comercial?)
                │
                ▼
        [3] Open Food Facts API (timeout: 5s, retry: 2x)
                │
                ├── Não encontrado ──→  Retorna resultados locais
                │
                └── Encontrado
                        │
                        ├──→  Retorna resultado (síncrono)
                        │
                        └──→  [4] Background Job (assíncrono)
                                   Salva no banco local como cache
```

---

## Motor de Cálculo de Macros

O `MacroCalculatorService` resolve o input do usuário para um peso em gramas e aplica a regra de três:

```
macro_resultado = (macro_por_100g × peso_g) / 100
```

### Formatos de Input Suportados

| Input | Interpretação |
|-------|--------------|
| `"250g"` / `"250 gramas"` | 250g explícitos |
| `"2 colheres de sopa"` | Lookup na tabela de medidas caseiras |
| `"1 porção"` / `"2 servings"` | Usa `serving_size_g` do alimento |
| `"150"` | Número puro → assume gramas |

### Exemplo

```java
// Frango grelhado: 32g proteína / 100g
MacroResult result = calculator.calculate(frango, "2 escumadeiras");
// 2 escumadeiras = 2 × 85g = 170g
// Proteínas: (32 × 170) / 100 = 54.4g
```

---

## Como Rodar

### Desenvolvimento local

```bash
# Sobe PostgreSQL + Redis
docker-compose up postgres redis -d

# Roda a aplicação
./mvnw spring-boot:run
```

### Tudo em Docker

```bash
docker-compose up --build
```

### Testes

```bash
./mvnw test
```

---

## Endpoints Principais

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/api/v1/foods/search?q={query}` | Busca fuzzy com fallback OFF |
| `GET` | `/api/v1/foods/{id}` | Detalhe do alimento com medidas |
| `GET` | `/api/v1/foods/barcode/{ean}` | Lookup por código de barras |
| `POST` | `/api/v1/foods` | Cadastro de alimento customizado |
| `POST` | `/api/v1/foods/calculate` | Cálculo de macros para uma porção |
| `DELETE` | `/api/v1/foods/{id}` | Soft-delete (desativação) |
| `GET` | `/actuator/health` | Health check |
| `GET` | `/actuator/prometheus` | Métricas Prometheus |
