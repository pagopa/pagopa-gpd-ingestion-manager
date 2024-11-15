function sleep(ms) {
	return new Promise(resolve => setTimeout(resolve, ms));
}

function buildCreatePaymentOptionEvent(id) {
	return buildDataCaptureMessage(
		null,
		buildPaymentOption(id, "Canone Unico Patrimoniale - SkyLab Inc."),
		"c",
		"payment_option"
	)
}

function buildReadPaymentOptionEvent(id) {
	return buildDataCaptureMessage(
		null,
		buildPaymentOption(id, "Canone Unico Patrimoniale - SkyLab Inc."),
		"r",
		"payment_option"
	)
}

function buildUpdatePaymentOptionEvent(id) {
	return buildDataCaptureMessage(
		buildPaymentOption(id, "Canone Unico Patrimoniale - SkyLab Inc."),
		buildPaymentOption(id, "Updated description"),
		"u",
		"payment_option"
	)
}

function buildDeletePaymentOptionEvent(id) {
	return buildDataCaptureMessage(
		{
			"id": id,
			"amount": 0,
			"description": "",
			"due_date": 0,
			"fee": 0,
			"flow_reporting_id": null,
			"receipt_id": null,
			"inserted_date": 0,
			"is_partial_payment": false,
			"iuv": "",
			"last_updated_date": 0,
			"organization_fiscal_code": "",
			"payment_date": null,
			"payment_method": null,
			"psp_company": null,
			"reporting_date": null,
			"retention_date": null,
			"status": "",
			"payment_position_id": 0,
			"notification_fee": 0,
			"last_updated_date_notification_fee": null,
			"nav": ""
		},
		null,
		"c",
		"payment_option"
	)
}

function buildDataCaptureMessage(before, after, operation, tableName) {
	return {
		"before": before,
		"after": after,
		"source": {
			"version": "3.0.0.Final",
			"connector": "postgresql",
			"name": "cdc-raw-auto",
			"ts_ms": 1731429124919,
			"snapshot": "first_in_data_collection",
			"db": "apd",
			"sequence": "[null,\"318800584\"]",
			"ts_us": 1731429124919483,
			"ts_ns": 1731429124919483000,
			"schema": "apd",
			"table": tableName,
			"txId": 2014,
			"lsn": 318800584,
			"xmin": null
		},
		"transaction": null,
		"op": operation,
		"ts_ms": Date.now(),
		"ts_us": 1731429125331424,
		"ts_ns": 1731429125331424940
	};
}

function buildPaymentOption(id, description) {
	return {
		"id": id,
		"amount": 30000,
		"description": description,
		"due_date": 1734019783323000,
		"fee": 0,
		"flow_reporting_id": null,
		"receipt_id": null,
		"inserted_date": 1731427783477968,
		"is_partial_payment": false,
		"iuv": "09455575462301733",
		"last_updated_date": 1731427783477968,
		"organization_fiscal_code": "77777777777",
		"payment_date": null,
		"payment_method": null,
		"psp_company": null,
		"reporting_date": null,
		"retention_date": 1739203783323000,
		"status": "PO_UNPAID",
		"payment_position_id": 1,
		"notification_fee": 0,
		"last_updated_date_notification_fee": null,
		"nav": "309455575462301733"
	}
}

module.exports = {
 sleep
}