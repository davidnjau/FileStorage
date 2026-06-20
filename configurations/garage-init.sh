#!/usr/bin/env bash
# First-time Garage node setup. Run once after containers are healthy.
# Usage: ./garage-init.sh [path/to/.env]
set -e

ENV_FILE="${1:-.env}"

if [[ ! -f "$ENV_FILE" ]]; then
    echo "Error: env file not found: $ENV_FILE" >&2
    exit 1
fi

# shellcheck disable=SC1090
source "$ENV_FILE"

: "${GARAGE_ACCESS_KEY:?GARAGE_ACCESS_KEY is not set in $ENV_FILE}"
: "${GARAGE_SECRET_KEY:?GARAGE_SECRET_KEY is not set in $ENV_FILE}"

echo "Waiting for Garage to be ready..."
until docker exec garage garage status &>/dev/null; do sleep 2; done

NODE=$(docker exec garage garage node id 2>/dev/null | head -1)
if [[ -z "$NODE" ]]; then
    echo "Error: could not retrieve Garage node ID" >&2
    exit 1
fi

echo "Assigning layout to node $NODE ..."
docker exec garage garage layout assign -z dc1 -c 1G "$NODE"
docker exec garage garage layout apply --version 1

echo "Importing key ..."
docker exec garage garage key import \
    --key-id "$GARAGE_ACCESS_KEY" \
    --secret-key "$GARAGE_SECRET_KEY" \
    my-key

echo "Granting bucket permissions ..."
docker exec garage garage bucket allow --read --write --owner ecommerce-public  --key my-key
docker exec garage garage bucket allow --read --write --owner ecommerce-private --key my-key

echo "Garage initialisation complete."
