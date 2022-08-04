#!/bin/sh

czertainlyHome="/opt/czertainly"
source ${czertainlyHome}/static-functions

if [ -f ${czertainlyHome}/trusted-certificates.pem ]
then
  log "INFO" "Adding additional trusted certificates to cacerts"
  ./update-cacerts.sh /opt/czertainly/trusted-certificates.pem
else
  log "INFO" "No trusted certificates were provided, continue!"
fi

log "INFO" "Launching the Core"
java -jar ./app.jar

#exec "$@"