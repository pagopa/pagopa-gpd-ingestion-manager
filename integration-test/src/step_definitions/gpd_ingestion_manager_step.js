const assert = require('assert');
const { After, Given, When, Then, setDefaultTimeout, AfterAll } = require('@cucumber/cucumber');
const { sleep } = require("./common");
const { readFromRedisWithKey, shutDownClient } = require("./redis_client");
const { shutDownPool, insertPaymentPosition, updatePaymentPosition, deletePaymentPosition, insertPaymentOption, updatePaymentOption, deletePaymentOption } = require("./pg_gpd_client");

// set timeout for Hooks function, it allows to wait for long task
setDefaultTimeout(360 * 1000);

// initialize variables
this.paymentPositionId = null;
this.paymentPositionCreateOp = null;
this.paymentPositionUpdateOp = null;
this.paymentPositionDeleteOp = null;
this.paymentPositionFiscalCode = null

AfterAll(async function () {
  shutDownPool();
  shutDownClient();
});

// After each Scenario
After(async function () {
  // remove event
  if (this.paymentPositionId != null) {
    await deletePaymentPosition(this.paymentPositionId);
  }

  this.listOfPaymentOptionId = null;
  this.paymentPositionFiscalCode = null;
});

/////////////////////////////
// Payment Positions steps //
/////////////////////////////

Given('a payment position created with id {string} and fiscal code {string} in GPD database', async function (id, fiscalCode) {
  await insertPaymentPosition(id, fiscalCode);
  this.paymentPositionId = id;
  this.paymentPositionFiscalCode = fiscalCode;
});

Given('an update on the same payment position in GPD database', async function () {
  await updatePaymentPosition(this.paymentPositionId);
});

Given('a delete on the same payment position in GPD database', async function () {
  await deletePaymentPosition(this.paymentPositionId);
});

When('the payment position operations have been properly published on data lake event hub after {int} ms', async function (time) {
  // boundary time spent by azure function to process event
  await sleep(time);
});

Then('the data lake topic returns the payment position {string} operation with id {string}', async function (operation, id) {
  let pp = await readFromRedisWithKey(id);
  if (operation === "create") {
    this.paymentPositionCreateOp = JSON.parse(pp);
    assert.strictEqual(this.paymentPositionCreateOp.op, "c");
  } else if (operation === "update") {
    this.paymentPositionUpdateOp = JSON.parse(pp);
    assert.strictEqual(this.paymentPositionUpdateOp.op, "u");
  } else if (operation === "delete") {
    this.paymentPositionDeleteOp = JSON.parse(pp);
    assert.strictEqual(this.paymentPositionDeleteOp.op, "d");
  }
});

Then('the operations have id {int}', function (id) {
  assert.strictEqual(this.paymentPositionCreateOp.after.id, id);
  assert.strictEqual(this.paymentPositionUpdateOp.before.id, id);
  assert.strictEqual(this.paymentPositionUpdateOp.after.id, id);
  assert.strictEqual(this.paymentPositionDeleteOp.after.id, id);
});

Then('the operations have the fiscal code tokenized', function () {
  assert.notStrictEqual(this.paymentPositionCreateOp.after.fiscalCode, this.paymentPositionFiscalCode);
  assert.notStrictEqual(this.paymentPositionUpdateOp.before.fiscalCode, this.paymentPositionFiscalCode);
  assert.notStrictEqual(this.paymentPositionUpdateOp.after.fiscalCode, this.paymentPositionFiscalCode);
});

Then('the payment position update operation has the company name changed from before and after', function () {
  assert.notStrictEqual(this.paymentPositionUpdateOp.after.companyName, this.paymentPositionUpdateOp.before.companyName);
});