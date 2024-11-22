const { ENTITY_IDENTIFIER, INVALID_CF, VALID_CF } = require('./common.js');
const { Pool } = require('pg');

//COMMON
const serverName = process.env.PG_GPD_SERVER_NAME;
const databaseName = process.env.PG_GPD_DATABASE_NAME;
//SECRETS
const password = process.env.PG_GPD_PASSWORD;
const username = process.env.PG_GPD_USERNAME;

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

async function shutDownPool() {
  await pool.end();
}

async function insertPaymentPositionWithValidFiscalCode(id) {
  await connection.query(`INSERT INTO apd.apd.payment_position (id, city, civic_number, company_name, country, email, fiscal_code, full_name, inserted_date, iupd, last_updated_date, max_due_date, min_due_date, office_name, organization_fiscal_code, phone, postal_code, province, publish_date, region, status, street_name, "type", validity_date, "version", switch_to_expired, payment_date, pull, pay_stand_in, service_type) VALUES('${id}', 'Pizzo Calabro', '11', 'SkyLab Inc. - Edit', 'IT', 'micheleventimiglia@skilabmail.com', '${VALID_CF}', 'Michele Ventimiglia', '2024-11-12 16:09:43.477', '${ENTITY_IDENTIFIER}', '2024-11-12 16:09:43.479', '2024-12-12 16:09:43.323', '2024-12-12 16:09:43.323', 'SkyLab - Sede via Washington - Edit', 'ORG_FISCAL_CODE_${id}', '333-123456789', '89812', 'VV', '2024-11-12 16:09:43.479', 'CA', 'VALID', 'via Washington', 'F', '2024-11-12 16:09:43.479', 0, false, NULL, true, false, 'GPD');`);
}

async function insertPaymentPositionWithInvalidFiscalCode(id) {
  await connection.query(`INSERT INTO apd.apd.payment_position (id, city, civic_number, company_name, country, email, fiscal_code, full_name, inserted_date, iupd, last_updated_date, max_due_date, min_due_date, office_name, organization_fiscal_code, phone, postal_code, province, publish_date, region, status, street_name, "type", validity_date, "version", switch_to_expired, payment_date, pull, pay_stand_in, service_type) VALUES('${id}', 'Pizzo Calabro', '11', 'SkyLab Inc. - Edit', 'IT', 'micheleventimiglia@skilabmail.com', '${INVALID_CF}', 'Michele Ventimiglia', '2024-11-12 16:09:43.477', '${ENTITY_IDENTIFIER}', '2024-11-12 16:09:43.479', '2024-12-12 16:09:43.323', '2024-12-12 16:09:43.323', 'SkyLab - Sede via Washington - Edit', 'ORG_FISCAL_CODE_${id}', '333-123456789', '89812', 'VV', '2024-11-12 16:09:43.479', 'CA', 'VALID', 'via Washington', 'F', '2024-11-12 16:09:43.479', 0, false, NULL, true, false, 'GPD');`);
}

async function deletePaymentPositions() {
  await connection.query(`DELETE FROM apd.apd.payment_position WHERE iupd='${ENTITY_IDENTIFIER}'`);
}

module.exports = {
  shutDownPool,
  insertPaymentPositionWithValidFiscalCode, insertPaymentPositionWithInvalidFiscalCode, deletePaymentPositions
}