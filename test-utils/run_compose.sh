#!/bin/bash

export INGESTION_EVENTHUB_CONN_STRING=$1
export INGESTION_EVENTHUB_TOPICS=$2

docker compose up -d --remove-orphans --force-recreate --build