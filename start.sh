#!/bin/sh

# Inicia o stunnel em foreground no background controlado
stunnel /etc/stunnel/stunnel.conf &

# Espera 2 segundos para garantir que o túnel subiu
sleep 2

# Inicia a aplicação Java
exec java -jar /app/app.jar