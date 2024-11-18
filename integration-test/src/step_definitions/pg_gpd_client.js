const { Pool } = require('pg');

const username = process.env.PG_GPD_USERNAME;
const password = process.env.PG_GPD_PASSWORD;
const serverName = process.env.PG_GPD_SERVER_NAME;
const databaseName = process.env.PG_GPD_DATABASE_NAME;

const pool = new Pool({
  user: username,
  database: databaseName,
  password: password,
  host: serverName,
  port: 5432,
  ssl: true
});

const connection = {
  pool,
  query: (...args) => {
    return pool.connect().then((client) => {
      return client.query(...args).then((res) => {
        client.release();
        return res.rows;
      });
    });
  },
};

async function insertPaymentPosition(id, fiscalCode) {
  await connection.query(`INSERT INTO apd.apd.payment_position (id, city, civic_number, company_name, country, email, fiscal_code, full_name, inserted_date, iupd, last_updated_date, max_due_date, min_due_date, office_name, organization_fiscal_code, phone, postal_code, province, publish_date, region, status, street_name, "type", validity_date, "version", switch_to_expired, payment_date, pull, pay_stand_in, service_type) VALUES('${id}', 'Pizzo Calabro', '11', 'SkyLab Inc. - Edit', 'IT', 'micheleventimiglia@skilabmail.com', 'VNTMHL76M09H501D', 'Michele Ventimiglia', '2024-11-12 16:09:43.477', 'IUPD_INTEGRATION_TEST_GPD_INGESTION', '2024-11-12 16:09:43.479', '2024-12-12 16:09:43.323', '2024-12-12 16:09:43.323', 'SkyLab - Sede via Washington - Edit', '${fiscalCode}', '333-123456789', '89812', 'VV', '2024-11-12 16:09:43.479', 'CA', 'VALID', 'via Washington', 'F', '2024-11-12 16:09:43.479', 0, false, NULL, true, false, 'GPD');`);
}

async function updatePaymentPosition(iupd) {
  await connection.query(`UPDATE apd.apd.payment_position SET company_name='Updated company name' WHERE id='${id}'`);
}

async function deletePaymentPosition(id) {
  await connection.query(`DELETE FROM apd.apd.payment_position WHERE id='${id}'`);
}

async function insertPaymentOption(id) {
  await connection.query(`INSERT INTO apd.apd.payment_option (id, amount, description, due_date, fee, flow_reporting_id, receipt_id, inserted_date, is_partial_payment, iuv, last_updated_date, organization_fiscal_code, payment_date, payment_method, psp_company, reporting_date, retention_date, status, payment_position_id, notification_fee, last_updated_date_notification_fee, nav) VALUES('${id}', 30000, 'Canone Unico Patrimoniale - SkyLab Inc.', '2024-12-12 16:09:43.323', 0, NULL, NULL, '2024-11-12 16:09:43.477', false, '09455575462301733', '2024-11-12 16:09:43.477', '77777777777', NULL, NULL, NULL, NULL, '2025-02-10 16:09:43.323', 'PO_UNPAID', 1, 0, NULL, '309455575462301733')`);
}

async function updatePaymentOption(id) {
  await connection.query(`UPDATE apd.apd.payment_option SET description='Updated description' WHERE id='${id}'`);
}

async function deletePaymentOption(id) {
  await connection.query(`DELETE FROM apd.apd.payment_option WHERE id='${id}'`);
}

module.exports = {
  insertPaymentPosition, updatePaymentPosition, deletePaymentPosition,
  insertPaymentOption, updatePaymentOption, deletePaymentOption
}