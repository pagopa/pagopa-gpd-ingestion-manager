# PGP KeyVault Management

This guide explains how to manage the PGP secrets used by the application through Azure Key Vault.

Use the helper script from the repository root:

```sh
sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh <environment> <operation> [arguments]
```

Supported environments are `dev`, `uat`, and `prod`.

> ℹ️ **Info**
>
> Production is currently the intended environment for RTP audit files, because they only refer to production data. Use `dev` and `uat` only for testing purposes.


## Prerequisites

Before running any operation, make sure that:

- Azure CLI is installed.
- You are authenticated with `az login`.
- Your account has permission to read and write secrets on the target Key Vault.
- You are running commands from the repository root.

The script selects the Azure subscription and Key Vault from the selected environment.

## Secret Names

By default, the script manages these Key Vault secrets:

| Secret | Default Key Vault name |
| --- | --- |
| Private PGP key | `pgp-rpt-audit-private-key` |
| Public PGP key | `gp-rpt-audit-private-key` |
| PGP passphrase | `pgp-rpt-audit-passphrase` |

If needed, override the names by exporting these variables before running the script:

```sh
export PRIVATE_SECRET_NAME="custom-private-secret-name"
export PUBLIC_SECRET_NAME="custom-public-secret-name"
export PASSPHRASE_SECRET_NAME="custom-passphrase-secret-name"
```

## Prepare Local Secret Files

Place the ASCII-armored PGP key files inside the `scripts` directory before running `add` or `update`.

Recommended file names:

```text
scripts/pgp-key-management/<environment>-private.asc
scripts/pgp-key-management/<environment>-public.asc
```

Example:

```text
scripts/pgp-key-management/dev-private.asc
scripts/pgp-key-management/dev-public.asc
```

> ⚠️ **Warning**
> 
> The repository `.gitignore` already excludes `*.asc` and `*.passphrase`. Keep secret files matching those patterns to reduce the risk of committing secrets to the public repository.
> Before committing any change, always verify that no secret file is staged.

## Add PGP Keys

Use `add` only when the private and public key secrets do not already exist in Key Vault.

```sh
sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh dev add scripts/pgp-key-management/dev-private.asc scripts/pgp-key-management/dev-public.asc
```

Replace `dev` with `uat` or `prod` as needed.

If the secrets already exist, the script stops and asks you to use `update`.

## Update PGP Keys

Use `update` when both private and public key secrets already exist and must be overwritten with new file contents.

```sh
sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh dev update scripts/pgp-key-management/dev-private.asc scripts/pgp-key-management/dev-public.asc
```

Replace `dev` with `uat` or `prod` as needed.

## Download PGP Keys

Use `get` to download both PGP key secrets from Key Vault to local files.

```sh
sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh dev get scripts/pgp-key-management/dev-private.asc scripts/pgp-key-management/dev-public.asc
```

The downloaded files contain secrets. Keep them inside `scripts` and make sure their names match the ignored `*.asc` pattern.

## Add or Update the Passphrase

Use `add-passphrase` to create or overwrite the PGP passphrase secret.

```sh
sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh dev add-passphrase "your-passphrase"
```

Replace `dev` with `uat` or `prod` as needed.

Avoid committing passphrases or storing them in tracked files. If you temporarily write a passphrase to a file, use a `*.passphrase` file name so it matches `.gitignore`, then delete it when it is no longer needed.

## Print the Passphrase

Use `get-passphrase` to print the passphrase secret value to stdout.

```sh
sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh dev get-passphrase
```

You can also capture it in a shell variable:

```sh
pgp_passphrase=$(sh scripts/pgp-key-management/manage_pgp_kv_secrets.sh dev get-passphrase)
```

Handle the printed value carefully. Do not paste it into tickets, logs, pull requests, or committed files.

## Cleanup

After completing the operation:

1. Remove local key files if they are no longer needed.
2. Clear terminal scrollback if secret values were printed.
3. Run `git status --short` and confirm that no secret material is staged or tracked.