#!/bin/bash

# Skript na porovnanie screenshotov a revert zmien, ak je rozdiel < 1%

# Cesta k novému screenshotu (napr. zo staging area)
NEW_IMAGE=$1
# Cesta k starému screenshotu (z posledného commitu)
OLD_IMAGE=$2

# Over, či sú oba súbory zadané
if [ -z "$NEW_IMAGE" ] || [ -z "$OLD_IMAGE" ]; then
  echo "Použitie: $0 <nový_obrázok> <starý_obrázok>"
  exit 1
fi

# Over, či súbory existujú
if [ ! -f "$NEW_IMAGE" ] || [ ! -f "$OLD_IMAGE" ]; then
  echo "Chyba: Jeden z obrázkov neexistuje."
  exit 1
fi

# Základný priečinok na ukladanie rozdielových obrázkov
DIFF_BASE_DIR="../../../build/images-diff"

# Získaj relatívnu cestu a odstráň prefix "../../../"
RELATIVE_PATH=$(dirname "$NEW_IMAGE" | sed 's/^\.\.\/\.\.\/\.\.\///')
DIFF_DIR="$DIFF_BASE_DIR/$RELATIVE_PATH"
mkdir -p "$DIFF_DIR"

# Vytvor názov rozdielového obrázku
DIFF_IMAGE="$DIFF_DIR/diff_$(basename "$NEW_IMAGE")"

# Použi ImageMagick na porovnanie obrázkov a ulož rozdielový obrázok
# Metrika AE (Absolute Error) vráti počet odlišných pixelov
DIFF=$(compare -metric AE "$NEW_IMAGE" "$OLD_IMAGE" "$DIFF_IMAGE" 2>&1)

# Získaj rozmery obrázka (v pixloch)
DIMENSIONS=$(identify -format "%[fx:w*h]" "$NEW_IMAGE")
TOTAL_PIXELS=$(echo "$DIMENSIONS" | awk '{printf "%.0f", $1}')

# Konvertuj DIFF na desatinné číslo, ak je vo vedeckom formáte
DIFF=$(echo "$DIFF" | awk '{printf "%.0f", $1}')

# Vypočítaj percento rozdielu
PERCENT_DIFF=$(echo "scale=2; ($DIFF / $TOTAL_PIXELS) * 100" | bc)

# Definuj šírky stĺpcov pre tabuľku
FILE_WIDTH=80
PERCENT_WIDTH=8
ACTION_WIDTH=12

# Vytvor hlavičku tabuľky (iba ak ešte nebola vypísaná)
if [ -z "${TABLE_HEADER_PRINTED+x}" ]; then
  #printf "%-${PERCENT_WIDTH}s %-${ACTION_WIDTH}s %-${FILE_WIDTH}s %-${FILE_WIDTH}s\n" "Rozdiel (%)" "Akcia" "Súbor" "Rozdielový obrázok"
  #printf "%-${PERCENT_WIDTH}s %-${ACTION_WIDTH}s %-${FILE_WIDTH}s %-${FILE_WIDTH}s\n" "------------" "--------------------" "------------------------------------------------------------" "------------------------------------------------------------"
  export TABLE_HEADER_PRINTED=1
fi

# Porovnaj s prahom (1 %)
THRESHOLD=1
if (( $(echo "$PERCENT_DIFF < $THRESHOLD" | bc -l) )); then
  printf "%-${PERCENT_WIDTH}s \033[31m%-${ACTION_WIDTH}s %-${FILE_WIDTH}s %-${FILE_WIDTH}s\033[0m\n" "$PERCENT_DIFF" "REVERTING" "$NEW_IMAGE" "$DIFF_IMAGE"
  git checkout -- "$NEW_IMAGE"
  #rm "$DIFF_IMAGE"  # Odstráň rozdielový obrázok, ak je zmena revertovaná
else
  printf "%-${PERCENT_WIDTH}s \033[32m%-${ACTION_WIDTH}s %-${FILE_WIDTH}s\033[0m %-${FILE_WIDTH}s\n" "$PERCENT_DIFF" "keep" "$NEW_IMAGE" "$DIFF_IMAGE"
fi