#!/bin/sh
# Entrypoint of the db-seed service in deploy/compose.dev.yml. The service starts after the application is healthy,
# that is after the Liquibase migrations, and applies the SQL script from TESTSYS_DB_SEED_SCRIPT once per
# database. The script and its mark in testsys_dev_seed.applied are committed in one transaction.
set -eu

script=/opt/testsys/db-seed.sql

# Without the variable Compose mounts /dev/null, a character device.
if [ -c "$script" ]; then
  echo "TESTSYS_DB_SEED_SCRIPT is not set, nothing to apply"
  exit 0
fi
# Docker Desktop creates a directory in place of a missing file.
if [ ! -f "$script" ]; then
  echo "TESTSYS_DB_SEED_SCRIPT must be the path of an existing SQL file; the mounted path is not a regular file" >&2
  exit 1
fi

query() {
  psql -X -q -t -A -v ON_ERROR_STOP=1 -c "$1"
}

checksum=$(sha256sum "$script" | cut -d ' ' -f 1)

if [ "$(query "select to_regclass('testsys_dev_seed.applied') is not null")" = t ]; then
  applied=$(query "select sha256 from testsys_dev_seed.applied")
  if [ "$applied" = "$checksum" ]; then
    echo "The seed script (sha256 $checksum) was already applied to this database, nothing to apply"
  else
    echo "WARNING: this database was seeded by another script (sha256 $applied), the current script" \
      "(sha256 $checksum) is not applied; remove the volumes with 'sh deploy/testsys.sh dev clean' to apply it" >&2
  fi
  exit 0
fi

echo "Applying the seed script (sha256 $checksum)"
psql -X -v ON_ERROR_STOP=1 --single-transaction \
  -c "create schema testsys_dev_seed" \
  -c "create table testsys_dev_seed.applied (sha256 text not null, applied_at timestamptz not null default now())" \
  -f "$script" \
  -c "insert into testsys_dev_seed.applied (sha256) values ('$checksum')"
echo "The seed script was applied"
