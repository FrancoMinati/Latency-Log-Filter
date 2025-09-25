#!/bin/bash
# Script: relaciones_be_mix.sh
# Descripción: Guarda los BE conectados al MIX consultado, asi como el nivel de detalle de los logs.

USUARIO="jenkins-linux"
FECHA=$(date +%Y%m%d_%H%M%S)
SALIDA="/opt/jenkins/fixscann/log_relaciones_be_mix_$FECHA.txt"
exec > "$SALIDA" 2>&1

SERVIDORES=(
    # ALGO - MITCH DATA
    BYYPASBUE535 BYYPASBUE589 BYYPASBUE590
)

for SERVER in "${SERVIDORES[@]}"; do
    echo -e "\n\n\n====================\nRevisando $SERVER..." | tee -a "$SALIDA"
    ssh -o ConnectTimeout=5 -i /opt/jenkins/.ssh/jl-rsa "${USUARIO}@${SERVER}" bash <<'EOF'

INSTANCIAS=(mitch-fix-pd mitch-fix-ext mitch-fix-ext2 mitch-fix-od mitch-data)

validos=()
for dir in "${INSTANCIAS[@]}"; do
    if [ -d "/caja/etc/$dir" ]; then
        validos+=("$dir")
    fi
done

echo -e "===== Nodo: $(hostname) ====="

for inst in "${validos[@]}"; do

    echo -e "\n\n===== Instancia: $inst"

    echo -e "\n>>> Archivos senderseqnums ($(date +"%b %-d")):"
    ACCEPTOR_DIR="/caja/var/$inst/logs/acceptor/"
    if [ -d "$ACCEPTOR_DIR" ]; then
        find "$ACCEPTOR_DIR" -type f -name "*senderseqnums*" -newermt "$(date +%Y-%m-%d)" ! -newermt "$(date -d tomorrow +%Y-%m-%d)"
    else
        echo "Directorio $ACCEPTOR_DIR no existe."
    fi

    echo -e "\n>>> userlogin.log:"
    LOGIN_FILE="/caja/var/$inst/logs/userlogin.log"
    if [ -f "$LOGIN_FILE" ]; then
        cat "$LOGIN_FILE"
    else
        echo "Archivo $LOGIN_FILE no encontrado."
    fi

    echo -e "\n>>> Configuración log4j.properties:"
    CONFIG_DIR="/caja/etc/$inst/config/"
    if [ -d "$CONFIG_DIR" ]; then
        grep -m 1 '^log4j\.rootLogger=' "$CONFIG_DIR/log4j.properties"
    else
        echo "Directorio $CONFIG_DIR no existe."
    fi
done
EOF

    rc=$?
    if [ $rc -ne 0 ]; then
        echo "Fallo la conexión al servidor $SERVER (rc=$rc)" | tee -a "$SALIDA"
    fi

done

echo -e "\n Finalizado. Detalle en: $SALIDA"

scp -rp "$SALIDA" "appusr@byypasbue490.cajval.sba.com.ar:/caja/var/relaciones-be-mix"
rc=$?
if [ $rc -ne 0 ]; then
    echo "Falló la copia del log con scp (rc=$rc)" | tee -a "$SALIDA"
    exit 1
fi

exit 0
