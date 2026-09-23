#!/usr/bin/env sh
# -----------------------------------------------------------------------------
# manage_pgp_kv_secrets.sh
#
# Description:
#   Script to upload, update, or download an ASCII-armored PGP key pair to/from
#   Azure Key Vault.
#
# Main features:
#   - add: uploads the two files as new Key Vault secrets.
#   - update: updates the two existing Key Vault secrets with the file contents.
#   - get: downloads the two Key Vault secrets to local files.
#   - add-passphrase: adds or updates the PGP passphrase secret from an input string.
#   - get-passphrase: prints the PGP passphrase secret value to stdout.
#
# Requirements:
#   - Azure CLI (az) authenticated and configured with Key Vault permissions.
#
# Usage:
#   sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh <dev|uat|prod> <add|update|get> [private_key_file] [public_key_file]
#   sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh <dev|uat|prod> add-passphrase <passphrase>
#   sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh <dev|uat|prod> get-passphrase
#
# Examples:
#   sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh dev add ./private.asc ./public.asc
#   sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh uat update ./private.asc ./public.asc
#   sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh prod get ./prod-private.asc ./prod-public.asc
#   sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh dev add-passphrase "my-passphrase"
#   sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh dev get-passphrase
#
# Note:
#   - Key Vault and subscription names are derived from the environment.
#   - File names are optional. If omitted, these defaults are used:
#       <environment>-private.asc
#       <environment>-public.asc
#   - Secret names can be overridden by exporting:
#       PRIVATE_SECRET_NAME
#       PUBLIC_SECRET_NAME
#       PASSPHRASE_SECRET_NAME
# -----------------------------------------------------------------------------

set -eu

usage() {
  echo "> sh $0 <dev|uat|prod> <add|update|get> [private_key_file] [public_key_file]"
  echo "> sh $0 <dev|uat|prod> add-passphrase <passphrase>"
  echo "> sh $0 <dev|uat|prod> get-passphrase"
}

info() {
  echo "$@" >&2
}

if [ $# -lt 2 ]; then
  usage
  exit 1
fi

environment="$1"
operation="$2"
private_key_file="${3:-$environment-private.asc}"
public_key_file="${4:-$environment-public.asc}"
passphrase="${3:-}"

private_secret_name="${PRIVATE_SECRET_NAME:-pgp-rpt-audit-private-key}"
public_secret_name="${PUBLIC_SECRET_NAME:-gp-rpt-audit-private-key}"
passphrase_secret_name="${PASSPHRASE_SECRET_NAME:-pgp-rpt-audit-passphrase}"

case "$environment" in
  dev)
    shortenv="d"
    subscriptionid="bbe47ad4-08b3-4925-94c5-1278e5819b86"
    ;;
  uat)
    shortenv="u"
    subscriptionid="26abc801-0d8f-4a6e-ac5f-8e81bcc09112"
    ;;
  prod)
    shortenv="p"
    subscriptionid="PROD-pagoPA"
    ;;
  *)
    echo "Environment not found: $environment"
    usage
    exit 1
    ;;
esac

case "$operation" in
  add|update|get|add-passphrase|get-passphrase)
    ;;
  *)
    echo "Operation not found: $operation"
    usage
    exit 1
    ;;
esac

kv_name="pagopa-$shortenv-gps-kv"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1"
    exit 1
  fi
}

secret_exists() {
  secret_name="$1"
  az keyvault secret show \
    --vault-name "$kv_name" \
    --name "$secret_name" \
    --only-show-errors \
    >/dev/null 2>&1
}

assert_secret_missing() {
  secret_name="$1"

  if secret_exists "$secret_name"; then
    echo "Secret already exists on $kv_name: $secret_name"
    echo "Use update to overwrite it."
    exit 1
  fi
}

assert_secret_exists() {
  secret_name="$1"

  if ! secret_exists "$secret_name"; then
    echo "Secret not found on $kv_name: $secret_name"
    echo "Use add to create it."
    exit 1
  fi
}

set_secret_from_file() {
  secret_name="$1"
  file_path="$2"

  if [ ! -f "$file_path" ]; then
    echo "File not found: $file_path"
    exit 1
  fi

  az keyvault secret set \
    --vault-name "$kv_name" \
    --name "$secret_name" \
    --file "$file_path" \
    --only-show-errors \
    >/dev/null
}

download_secret_to_file() {
  secret_name="$1"
  file_path="$2"

  az keyvault secret download \
    --vault-name "$kv_name" \
    --name "$secret_name" \
    --file "$file_path" \
    --only-show-errors
}

set_secret_from_value() {
  secret_name="$1"
  secret_value="$2"

  if [ -z "$secret_value" ]; then
    echo "Missing passphrase value"
    usage
    exit 1
  fi

  az keyvault secret set \
    --vault-name "$kv_name" \
    --name "$secret_name" \
    --value "$secret_value" \
    --only-show-errors \
    >/dev/null
}

print_secret_value() {
  secret_name="$1"

  az keyvault secret show \
    --vault-name "$kv_name" \
    --name "$secret_name" \
    --query "value" \
    --output tsv \
    --only-show-errors
}

require_command "az"

info "Using subscription $subscriptionid"
az account set --subscription "$subscriptionid"

info "Using Key Vault $kv_name"

if [ "$operation" = "add" ]; then
  assert_secret_missing "$private_secret_name"
  assert_secret_missing "$public_secret_name"

  info "Adding $private_secret_name from $private_key_file"
  set_secret_from_file "$private_secret_name" "$private_key_file"

  info "Adding $public_secret_name from $public_key_file"
  set_secret_from_file "$public_secret_name" "$public_key_file"

  info "PGP secrets added"
elif [ "$operation" = "update" ]; then
  assert_secret_exists "$private_secret_name"
  assert_secret_exists "$public_secret_name"

  info "Updating $private_secret_name from $private_key_file"
  set_secret_from_file "$private_secret_name" "$private_key_file"

  info "Updating $public_secret_name from $public_key_file"
  set_secret_from_file "$public_secret_name" "$public_key_file"

  info "PGP secrets updated"
elif [ "$operation" = "get" ]; then
  assert_secret_exists "$private_secret_name"
  assert_secret_exists "$public_secret_name"

  info "Downloading $private_secret_name to $private_key_file"
  download_secret_to_file "$private_secret_name" "$private_key_file"

  info "Downloading $public_secret_name to $public_key_file"
  download_secret_to_file "$public_secret_name" "$public_key_file"

  info "PGP secrets downloaded"
elif [ "$operation" = "add-passphrase" ]; then
  info "Adding or updating $passphrase_secret_name"
  set_secret_from_value "$passphrase_secret_name" "$passphrase"

  info "PGP passphrase secret added or updated"
else
  assert_secret_exists "$passphrase_secret_name"
  print_secret_value "$passphrase_secret_name"
fi