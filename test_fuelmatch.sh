#!/bin/bash
# =============================================================================
# FuelMatch :: Nutrition Service — Test Suite
# Executa todos os testes de integração via curl
# Uso: ./test_fuelmatch.sh [BASE_URL]
# Exemplo: ./test_fuelmatch.sh http://localhost:8080
# =============================================================================

BASE_URL="${1:-http://localhost:8080}"
PASS=0
FAIL=0
TOTAL=0

# ── Cores ─────────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# ── Helpers ───────────────────────────────────────────────────────────────────
print_header() {
    echo ""
    echo -e "${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}${BOLD}  $1${NC}"
    echo -e "${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

run_test() {
    local TEST_NAME="$1"
    local METHOD="$2"
    local URL="$3"
    local BODY="$4"
    local EXPECTED_STATUS="$5"
    local JSON_CHECK="$6"   # campo jq para verificar (opcional)
    local EXPECTED_VALUE="$7" # valor esperado (opcional)

    TOTAL=$((TOTAL + 1))

    # Monta o comando curl
    if [ -n "$BODY" ]; then
        RESPONSE=$(curl -s -w "\n%{http_code}" -X "$METHOD" "$URL" \
            -H "Content-Type: application/json" \
            -d "$BODY" 2>/dev/null)
    else
        RESPONSE=$(curl -s -w "\n%{http_code}" -X "$METHOD" "$URL" 2>/dev/null)
    fi

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY_RESPONSE=$(echo "$RESPONSE" | head -n -1)

    # Verifica status HTTP
    if [ "$HTTP_CODE" != "$EXPECTED_STATUS" ]; then
        echo -e "  ${RED}✗ FAIL${NC} | $TEST_NAME"
        echo -e "         Status esperado: ${EXPECTED_STATUS} | Recebido: ${HTTP_CODE}"
        echo -e "         Resposta: $(echo "$BODY_RESPONSE" | head -c 200)"
        FAIL=$((FAIL + 1))
        return
    fi

    # Verifica valor JSON se especificado
    if [ -n "$JSON_CHECK" ] && [ -n "$EXPECTED_VALUE" ]; then
        ACTUAL_VALUE=$(echo "$BODY_RESPONSE" | jq -r "$JSON_CHECK" 2>/dev/null)
        if [ "$ACTUAL_VALUE" != "$EXPECTED_VALUE" ]; then
            echo -e "  ${RED}✗ FAIL${NC} | $TEST_NAME"
            echo -e "         Campo: $JSON_CHECK"
            echo -e "         Esperado: '$EXPECTED_VALUE' | Recebido: '$ACTUAL_VALUE'"
            FAIL=$((FAIL + 1))
            return
        fi
    fi

    echo -e "  ${GREEN}✓ PASS${NC} | $TEST_NAME"
    PASS=$((PASS + 1))
}

check_dependency() {
    if ! command -v "$1" &> /dev/null; then
        echo -e "${RED}Erro: '$1' não encontrado. Instale com: sudo apt install $1${NC}"
        exit 1
    fi
}

# ── Verificações iniciais ─────────────────────────────────────────────────────
check_dependency curl
check_dependency jq

echo ""
echo -e "${BOLD}FuelMatch Nutrition Service — Test Suite${NC}"
echo -e "Base URL: ${CYAN}$BASE_URL${NC}"
echo ""

# Verifica se o servidor está no ar
echo -n "Verificando servidor... "
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null)
if [ "$HTTP" != "200" ]; then
    echo -e "${RED}Servidor não está respondendo em $BASE_URL${NC}"
    echo "Certifique-se de que o Spring Boot está rodando: mvn spring-boot:run"
    exit 1
fi
echo -e "${GREEN}OK${NC}"

# =============================================================================
# 1. ACTUATOR / HEALTH
# =============================================================================
print_header "1. ACTUATOR & HEALTH"

run_test "Health check retorna UP" \
    "GET" "$BASE_URL/actuator/health" "" \
    "200" ".status" "UP"

run_test "Actuator info disponível" \
    "GET" "$BASE_URL/actuator/info" "" \
    "200"

run_test "Métricas Prometheus disponíveis" \
    "GET" "$BASE_URL/actuator/prometheus" "" \
    "200"

# =============================================================================
# 2. BUSCA DE ALIMENTOS — TACO (banco local)
# =============================================================================
print_header "2. BUSCA FUZZY — TACO (banco local)"

run_test "Busca 'arroz' retorna resultados" \
    "GET" "$BASE_URL/api/v1/foods/search?q=arroz&source=TACO" "" \
    "200" ".content | length > 0" "true"

run_test "Busca 'frango' retorna resultados" \
    "GET" "$BASE_URL/api/v1/foods/search?q=frango&source=TACO" "" \
    "200" ".content | length > 0" "true"

run_test "Busca com typo 'frnago' ainda retorna frango (fuzzy)" \
    "GET" "$BASE_URL/api/v1/foods/search?q=frnago&source=TACO" "" \
    "200" ".content | length > 0" "true"

run_test "Busca 'batata%20doce' retorna resultados" \
    "GET" "$BASE_URL/api/v1/foods/search?q=batata%20doce&source=TACO" "" \
    "200" ".content | length > 0" "true"

run_test "Busca 'feijao' (sem acento) retorna feijão" \
    "GET" "$BASE_URL/api/v1/foods/search?q=feijao&source=TACO" "" \
    "200" ".content | length > 0" "true"

run_test "Busca 'salmao' (sem acento) retorna salmão" \
    "GET" "$BASE_URL/api/v1/foods/search?q=salmao&source=TACO" "" \
    "200" ".content | length > 0" "true"

run_test "Busca com source=TACO filtra corretamente" \
    "GET" "$BASE_URL/api/v1/foods/search?q=arroz&source=TACO" "" \
    "200" ".content[0].source" "TACO"

run_test "Busca sem filtro de source funciona" \
    "GET" "$BASE_URL/api/v1/foods/search?q=arroz" "" \
    "200" ".content | length > 0" "true"

run_test "Busca com termo inexistente retorna lista vazia" \
    "GET" "$BASE_URL/api/v1/foods/search?q=zzzzprodutoinexistentezzzz&source=TACO" "" \
    "200" ".content | length" "0"

run_test "Paginação funciona (page=0, size=2)" \
    "GET" "$BASE_URL/api/v1/foods/search?q=frango&source=TACO&page=0&size=2" "" \
    "200" ".size" "2"

# =============================================================================
# 3. BUSCA POR ID
# =============================================================================
print_header "3. BUSCA POR ID"

FRANGO_ID="00000003-0000-0000-0000-000000000001"
ARROZ_ID="00000001-0000-0000-0000-000000000001"
BANANA_ID="00000007-0000-0000-0000-000000000001"
FEIJAO_ID="00000002-0000-0000-0000-000000000001"

run_test "GET /foods/{id} retorna frango com medidas" \
    "GET" "$BASE_URL/api/v1/foods/$FRANGO_ID" "" \
    "200" ".name" "Frango, peito, grelhado"

run_test "Frango tem medidas caseiras" \
    "GET" "$BASE_URL/api/v1/foods/$FRANGO_ID" "" \
    "200" ".measures | length > 0" "true"

run_test "Frango é source=TACO" \
    "GET" "$BASE_URL/api/v1/foods/$FRANGO_ID" "" \
    "200" ".source" "TACO"

run_test "Arroz retorna corretamente" \
    "GET" "$BASE_URL/api/v1/foods/$ARROZ_ID" "" \
    "200" ".name" "Arroz, branco, cozido"

run_test "Banana retorna corretamente" \
    "GET" "$BASE_URL/api/v1/foods/$BANANA_ID" "" \
    "200" ".name" "Banana, prata"

run_test "UUID inexistente retorna 404" \
    "GET" "$BASE_URL/api/v1/foods/00000000-0000-0000-0000-000000000000" "" \
    "404"

# =============================================================================
# 4. BUSCA POR BARCODE
# =============================================================================
print_header "4. BUSCA POR BARCODE"

run_test "Barcode inexistente localmente retorna 404 ou produto OFF" \
    "GET" "$BASE_URL/api/v1/foods/barcode/7891000100103" "" \
    "404"

run_test "Barcode inválido (menos de 8 dígitos) retorna 400" \
    "GET" "$BASE_URL/api/v1/foods/barcode/123" "" \
    "400"

# =============================================================================
# 5. CÁLCULO DE MACROS — PORÇÃO ÚNICA
# =============================================================================
print_header "5. CÁLCULO DE MACROS — PORÇÃO ÚNICA"

run_test "Calcular 250g de frango — kcal correta (397.50)" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    '{"foodId":"'"$FRANGO_ID"'","portionInput":"250g"}' \
    "200" ".energyKcal" "397.50"

run_test "Calcular 250g de frango — proteína correta (80.00g)" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    '{"foodId":"'"$FRANGO_ID"'","portionInput":"250g"}' \
    "200" ".proteinsG" "80.00"

run_test "Calcular 250g de frango — peso calculado correto" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    '{"foodId":"'"$FRANGO_ID"'","portionInput":"250g"}' \
    "200" ".calculatedWeightG" "250.00"

run_test "Calcular por medida caseira '1 filé médio' (120g)" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    '{"foodId":"'"$FRANGO_ID"'","portionInput":"1 fil\u00e9 m\u00e9dio"}' \
    "200" ".calculatedWeightG" "120.00"

run_test "Calcular por medida '1 escumadeira' (85g)" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    '{"foodId":"'"$FRANGO_ID"'","portionInput":"1 escumadeira"}' \
    "200" ".calculatedWeightG" "85.00"

run_test "Calcular por número puro '100' (assume gramas)" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    '{"foodId":"'"$FRANGO_ID"'","portionInput":"100"}' \
    "200" ".calculatedWeightG" "100.00"

run_test "Calcular 100g de arroz — kcal correta (128.00)" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    '{"foodId":"'"$ARROZ_ID"'","portionInput":"100g"}' \
    "200" ".energyKcal" "128.00"

run_test "Calcular 1 xícara de chá de arroz (160g)" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    '{"foodId":"'"$ARROZ_ID"'","portionInput":"1 x\u00edcara de ch\u00e1"}' \
    "200" ".calculatedWeightG" "160.00"

run_test "Calcular 1 unidade média de banana (100g)" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    '{"foodId":"'"$BANANA_ID"'","portionInput":"1 unidade m\u00e9dia"}' \
    "200" ".calculatedWeightG" "100.00"

run_test "Porção inválida retorna 422" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    '{"foodId":"'"$FRANGO_ID"'","portionInput":"uma por\u00e7\u00e3o gigante"}' \
    "422"

run_test "Porção inválida tem hint na resposta" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    '{"foodId":"'"$FRANGO_ID"'","portionInput":"invalido xyz"}' \
    "422" ".hint" "Exemplos válidos: '250g', '2 colheres de sopa', '1 porção'"

run_test "foodId ausente retorna 400" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    '{"portionInput":"250g"}' \
    "400"

run_test "portionInput ausente retorna 400" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    '{"foodId":"'"$FRANGO_ID"'"}' \
    "400"

run_test "foodId inexistente retorna 404" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    '{"foodId":"00000000-0000-0000-0000-000000000000","portionInput":"100g"}' \
    "404"

# =============================================================================
# 6. CÁLCULO EM BATCH
# =============================================================================
print_header "6. CÁLCULO EM BATCH"

BATCH_BODY='{
  "items": [
    {"foodId":"'"$FRANGO_ID"'","portionInput":"150g","correlationId":"frango"},
    {"foodId":"'"$ARROZ_ID"'","portionInput":"1 x\u00edcara de ch\u00e1","correlationId":"arroz"},
    {"foodId":"'"$BANANA_ID"'","portionInput":"1 unidade m\u00e9dia","correlationId":"banana"}
  ]
}'

run_test "Batch com 3 itens válidos retorna 3 resultados" \
    "POST" "$BASE_URL/api/v1/foods/calculate/batch" \
    "$BATCH_BODY" \
    "200" ".items | length" "3"

run_test "Batch — todos os itens com success=true" \
    "POST" "$BASE_URL/api/v1/foods/calculate/batch" \
    "$BATCH_BODY" \
    "200" ".errorCount" "0"

run_test "Batch — totais agregados presentes" \
    "POST" "$BASE_URL/api/v1/foods/calculate/batch" \
    "$BATCH_BODY" \
    "200" ".totals.totalEnergyKcal | . > 0" "true"

run_test "Batch — correlationId preservado no frango" \
    "POST" "$BASE_URL/api/v1/foods/calculate/batch" \
    "$BATCH_BODY" \
    "200" ".items[0].correlationId" "frango"

BATCH_COM_ERRO='{
  "items": [
    {"foodId":"'"$FRANGO_ID"'","portionInput":"100g","correlationId":"valido"},
    {"foodId":"00000000-0000-0000-0000-000000000000","portionInput":"100g","correlationId":"invalido"}
  ]
}'

run_test "Batch com item inválido não cancela os demais" \
    "POST" "$BASE_URL/api/v1/foods/calculate/batch" \
    "$BATCH_COM_ERRO" \
    "200" ".items[0].success" "true"

run_test "Batch — item inválido tem success=false" \
    "POST" "$BASE_URL/api/v1/foods/calculate/batch" \
    "$BATCH_COM_ERRO" \
    "200" ".items[1].success" "false"

run_test "Batch — errorCount correto (1)" \
    "POST" "$BASE_URL/api/v1/foods/calculate/batch" \
    "$BATCH_COM_ERRO" \
    "200" ".errorCount" "1"

run_test "Batch vazio retorna 400" \
    "POST" "$BASE_URL/api/v1/foods/calculate/batch" \
    '{"items":[]}' \
    "400"

# =============================================================================
# 7. CADASTRO DE ALIMENTO CUSTOMIZADO
# =============================================================================
print_header "7. CADASTRO DE ALIMENTO CUSTOMIZADO"

CUSTOM_FOOD='{
  "name": "Shake Pre-Treino Teste",
  "energyKcal": 280,
  "proteinsG": 30,
  "carbohydratesG": 35,
  "fatTotalG": 4,
  "servingSizeG": 350,
  "servingDescription": "1 copo (350ml)",
  "category": "SUPLEMENTOS"
}'

run_test "POST /foods cria alimento customizado" \
    "POST" "$BASE_URL/api/v1/foods" \
    "$CUSTOM_FOOD" \
    "201" ".name" "Shake Pre-Treino Teste"

run_test "Alimento customizado tem source=CUSTOM" \
    "POST" "$BASE_URL/api/v1/foods" \
    "$CUSTOM_FOOD" \
    "201" ".source" "CUSTOM"

run_test "Alimento customizado tem verified=false" \
    "POST" "$BASE_URL/api/v1/foods" \
    "$CUSTOM_FOOD" \
    "201" ".verified" "false"

# Captura o ID do alimento criado para usar nos próximos testes
CUSTOM_ID=$(curl -s -X POST "$BASE_URL/api/v1/foods" \
    -H "Content-Type: application/json" \
    -d "$CUSTOM_FOOD" | jq -r '.id' 2>/dev/null)

if [ -n "$CUSTOM_ID" ] && [ "$CUSTOM_ID" != "null" ]; then
    run_test "Alimento customizado pode ser buscado por ID" \
        "GET" "$BASE_URL/api/v1/foods/$CUSTOM_ID" "" \
        "200" ".name" "Shake Pre-Treino Teste"

    run_test "Calcular macros do alimento customizado (1 porção)" \
        "POST" "$BASE_URL/api/v1/foods/calculate" \
        '{"foodId":"'"$CUSTOM_ID"'","portionInput":"1 por\u00e7\u00e3o"}' \
        "200" ".calculatedWeightG" "350.00"

    run_test "Calcular macros customizado (100g) — kcal=280.00" \
        "POST" "$BASE_URL/api/v1/foods/calculate" \
        '{"foodId":"'"$CUSTOM_ID"'","portionInput":"100g"}' \
        "200" ".energyKcal" "280.00"

    run_test "DELETE /foods/{id} desativa alimento" \
        "DELETE" "$BASE_URL/api/v1/foods/$CUSTOM_ID" "" \
        "204"
else
    echo -e "  ${YELLOW}⚠ SKIP${NC} | Testes do alimento customizado (falha ao criar)"
    TOTAL=$((TOTAL + 4))
    FAIL=$((FAIL + 4))
fi

run_test "POST /foods sem nome retorna 400" \
    "POST" "$BASE_URL/api/v1/foods" \
    '{"energyKcal":100}' \
    "400"

# =============================================================================
# 8. VALIDAÇÕES E EDGE CASES
# =============================================================================
print_header "8. VALIDAÇÕES E EDGE CASES"

run_test "Endpoint inexistente retorna 404" \
    "GET" "$BASE_URL/api/v1/naoexiste" "" \
    "404"

run_test "Busca sem parâmetro 'q' retorna 400" \
    "GET" "$BASE_URL/api/v1/foods/search" "" \
    "400"

run_test "POST /calculate com JSON inválido retorna 400" \
    "POST" "$BASE_URL/api/v1/foods/calculate" \
    'json invalido' \
    "400"

# =============================================================================
# 9. ADMIN
# =============================================================================
print_header "9. ADMIN"

run_test "POST /admin/import/taco responde com status completed" \
    "POST" "$BASE_URL/api/v1/admin/import/taco" "" \
    "200" ".status" "completed"

run_test "POST /admin/cache/evict limpa caches" \
    "POST" "$BASE_URL/api/v1/admin/cache/evict" "" \
    "200" ".status" "evicted"

# =============================================================================
# 10. OPEN FOOD FACTS FALLBACK
# =============================================================================
print_header "10. OPEN FOOD FACTS — FALLBACK"

run_test "Busca 'whey protein' sem source filtra não-TACO (chama OFF)" \
    "GET" "$BASE_URL/api/v1/foods/search?q=whey+protein" "" \
    "200"

run_test "Busca 'coca cola' pode retornar resultado da OFF" \
    "GET" "$BASE_URL/api/v1/foods/search?q=coca+cola" "" \
    "200"

# =============================================================================
# RESUMO FINAL
# =============================================================================
echo ""
echo -e "${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}  RESULTADO FINAL${NC}"
echo -e "${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "  Total de testes : ${BOLD}$TOTAL${NC}"
echo -e "  ${GREEN}Passou           : $PASS${NC}"
echo -e "  ${RED}Falhou           : $FAIL${NC}"
echo ""

if [ "$FAIL" -eq 0 ]; then
    echo -e "  ${GREEN}${BOLD}✓ Todos os testes passaram!${NC}"
else
    PERCENT=$(( (PASS * 100) / TOTAL ))
    echo -e "  ${YELLOW}${BOLD}Taxa de sucesso: $PERCENT%${NC}"
fi

echo ""
exit $FAIL
