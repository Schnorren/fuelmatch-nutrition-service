#!/usr/bin/env python3
"""
convert_taco_csv.py
===================
Converte o CSV bruto da TACO 4ª Edição (com header multi-linha e categorias
intercaladas) em um CSV limpo e normalizado, pronto para o TacoImportService.

Uso:
    python3 convert_taco_csv.py <arquivo_entrada.csv> <arquivo_saida.csv>

Exemplo:
    python3 convert_taco_csv.py Taco-4a-Edicao_CMVCol_taco3_.csv taco_4ed.csv
"""

import sys
import csv
import math
import pandas as pd

# ── Mapeamento de categorias TACO → enum do FuelMatch ────────────────────────
CAT_MAP = {
    'Cereais e derivados':                  'CEREAIS_GRAOS',
    'Verduras, hortaliças e derivados':     'HORTALICAS',
    'Frutas e derivados':                   'FRUTAS',
    'Gorduras e óleos':                     'OLEOS_GORDURAS',
    'Pescados e frutos do mar':             'PESCADOS',
    'Carnes e derivados':                   'CARNES_BOVINAS',
    'Leite e derivados':                    'OVOS_LATICINIOS',
    'Bebidas (alcoólicas e não alcoólicas)':'BEBIDAS',
    'Ovos e derivados':                     'OVOS_LATICINIOS',
    'Produtos açucarados':                  'ACUCARES_DOCES',
    'Miscelâneas':                          'OUTROS',
    'Outros alimentos industrializados':    'INDUSTRIALIZADOS',
    'Alimentos preparados':                 'ALIMENTOS_PREPARADOS',
    'Leguminosas e derivados':              'LEGUMINOSAS',
    'Nozes e sementes':                     'OUTROS',
}

# ── Colunas de saída ──────────────────────────────────────────────────────────
HEADER = [
    'taco_id', 'name', 'category',
    # Macros
    'energy_kcal', 'energy_kj',
    'proteins_g', 'fat_total_g', 'carbohydrates_g',
    'fiber_g', 'cholesterol_mg', 'ash_g',
    # Minerais
    'calcium_mg', 'magnesium_mg', 'manganese_mg',
    'phosphorus_mg', 'iron_mg', 'sodium_mg',
    'potassium_mg', 'copper_mg', 'zinc_mg',
    # Vitaminas
    'retinol_mcg', 're_mcg', 'rae_mcg',
    'thiamine_mg', 'riboflavin_mg', 'pyridoxine_mg',
    'niacin_mg', 'vitamin_c_mg',
]

def safe(val):
    """Converte valor para string limpa, substituindo NaN/Tr/* por vazio."""
    if val is None:
        return ''
    s = str(val).strip()
    if s.lower() in ('nan', 'tr', 'tr.', '*', '-', 'nd', ''):
        return ''
    # Troca vírgula decimal por ponto
    return s.replace(',', '.')

def convert(input_path: str, output_path: str):
    print(f"Lendo: {input_path}")
    df = pd.read_csv(input_path, header=None, encoding='latin-1')
    print(f"Total de linhas no arquivo: {len(df)}")

    # Pula as 3 linhas de header (0, 1, 2)
    dados = df.iloc[3:]

    # Constrói mapa de id → categoria percorrendo linha a linha
    cat_by_id = {}
    current_cat = 'OUTROS'

    for _, row in dados.iterrows():
        val = str(row[0]).strip()
        if val in CAT_MAP:
            current_cat = CAT_MAP[val]
        elif val.isdigit():
            cat_by_id[int(val)] = current_cat

    # Filtra apenas linhas de alimentos (col 0 é número inteiro)
    alimentos = dados[dados[0].apply(lambda x: str(x).strip().isdigit())]
    print(f"Alimentos encontrados: {len(alimentos)}")

    rows_out = []
    for _, row in alimentos.iterrows():
        taco_id = int(str(row[0]).strip())
        rows_out.append([
            taco_id,
            str(row[1]).strip(),                    # name
            cat_by_id.get(taco_id, 'OUTROS'),       # category
            safe(row[3]),   # energy_kcal
            safe(row[4]),   # energy_kj
            safe(row[5]),   # proteins_g
            safe(row[6]),   # fat_total_g
            safe(row[8]),   # carbohydrates_g
            safe(row[9]),   # fiber_g
            safe(row[7]),   # cholesterol_mg
            safe(row[10]),  # ash_g
            safe(row[11]),  # calcium_mg
            safe(row[12]),  # magnesium_mg
            safe(row[14]),  # manganese_mg
            safe(row[15]),  # phosphorus_mg
            safe(row[16]),  # iron_mg
            safe(row[17]),  # sodium_mg
            safe(row[18]),  # potassium_mg
            safe(row[19]),  # copper_mg
            safe(row[20]),  # zinc_mg
            safe(row[21]),  # retinol_mcg
            safe(row[22]),  # re_mcg
            safe(row[23]),  # rae_mcg
            safe(row[24]),  # thiamine_mg
            safe(row[25]),  # riboflavin_mg
            safe(row[26]),  # pyridoxine_mg
            safe(row[27]),  # niacin_mg
            safe(row[28]),  # vitamin_c_mg
        ])

    with open(output_path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(HEADER)
        writer.writerows(rows_out)

    print(f"CSV limpo gerado: {output_path}")
    print(f"Linhas exportadas: {len(rows_out)}")

    # Resumo por categoria
    from collections import Counter
    cats = Counter(r[2] for r in rows_out)
    print("\nAlimentos por categoria:")
    for cat, count in sorted(cats.items()):
        print(f"  {cat:<30} {count:>3} alimentos")

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Uso: python3 convert_taco_csv.py <entrada.csv> <saida.csv>")
        sys.exit(1)
    convert(sys.argv[1], sys.argv[2])
