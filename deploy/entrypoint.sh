#!/bin/bash
set -euox pipefail

PGHOST_AND_PORT=$(echo "$DATASOURCE_URL" | awk -F/ '{print $3}')
export PGHOST=$(echo $PGHOST_AND_PORT | cut -d':' -f1)
export PGPORT=$(echo $PGHOST_AND_PORT | cut -d':' -f2)

export PGUSER=$DATASOURCE_USERNAME
export PGPASSWORD=$DATASOURCE_PASSWORD

until psql -c "select 1"; do
    echo "$(date): Waiting for ${PGHOST}:${PGPORT} to be up"
    sleep 10
done

echo "$(date): ${PGHOST}:${PGPORT} is up! Starting app"

catalina.sh run
