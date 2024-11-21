data "azurerm_storage_account" "tfstate_app" {
  name                = "pagopainfraterraform${var.env}"
  resource_group_name = "io-infra-rg"
}

data "azurerm_user_assigned_identity" "identity_cd" {
  resource_group_name = "${local.product}-identity-rg"
  name                = "${local.product}-${local.domain}-01-github-cd-identity"
}

data "azurerm_resource_group" "dashboards" {
  name = "dashboards"
}

data "azurerm_resource_group" "apim_resource_group" {
  name = "${local.product}-api-rg"
}

data "azurerm_kubernetes_cluster" "aks" {
  name                = local.aks_cluster.name
  resource_group_name = local.aks_cluster.resource_group_name
}

data "github_organization_teams" "all" {
  root_teams_only = true
  summary_only    = true
}

data "azurerm_key_vault" "key_vault" {
  name = "pagopa-${var.env_short}-kv"
  resource_group_name = "pagopa-${var.env_short}-sec-rg"
}

data "azurerm_key_vault" "domain_key_vault" {
  name = "pagopa-${var.env_short}-${local.domain}-kv"
  resource_group_name = "pagopa-${var.env_short}-${local.domain}-sec-rg"
}

data "azurerm_key_vault_secret" "key_vault_sonar" {
  name = "sonar-token"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_bot_token" {
  name = "bot-token-github"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_cucumber_token" {
  name = "cucumber-token"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

# data "azurerm_key_vault_secret" "key_vault_integration_test_webhook_slack" {
#   name         = "webhook-slack"
#   key_vault_id = data.azurerm_key_vault.domain_key_vault.id
# }

data "azurerm_key_vault_secret" "key_vault_integration_test_evh_conn_string" {
  name         = "cdc-gpd-test-connection-string"
  key_vault_id = data.azurerm_key_vault.domain_key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_integration_test_gpd_db_apd_user_psw" {
  name         = "db-apd-user-password"
  key_vault_id = data.azurerm_key_vault.domain_key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_integration_test_gpd_db_apd_user_name" {
  name         = "db-apd-user-name"
  key_vault_id = data.azurerm_key_vault.domain_key_vault.id
}

