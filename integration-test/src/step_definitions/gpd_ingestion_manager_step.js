const assert = require('assert');
const { After, Given, When, Then, setDefaultTimeout, AfterAll } = require('@cucumber/cucumber');
const { sleep } = require("./common");
const { readFromRedisWithKey, shutDownClient } = require("./redis_client");
const { shutDownPool, insertPaymentPosition, updatePaymentPosition, deletePaymentPosition, insertPaymentOption, updatePaymentOption, deletePaymentOption } = require("./pg_gpd_client");

// set timeout for Hooks function, it allows to wait for long task
setDefaultTimeout(360 * 1000);

// initialize variables

////////////////////////////
// Payment Positions vars //
////////////////////////////
this.paymentPositionId = null;
this.paymentPositionCreateOp = null;
this.paymentPositionUpdateOp = null;
this.paymentPositionDeleteOp = null;
this.paymentPositionFiscalCode = null;
this.paymentPositionCompanyName = null;
this.paymentPositionUpdatedCompanyName = null;

///////////////////////////
// Payment Options vasrs //
///////////////////////////
this.paymentOptionId = null;
this.paymentOptionDescription = null;
this.paymentOptionUpdatedDescription = null;
this.paymentOptionCreateOp = null;
this.paymentOptionUpdateOp = null;
this.paymentOptionDeleteOp = null;

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
  if (this.paymentOptionId != null) {
    await deletePaymentOption(this.paymentOptionId);
  }

  ////////////////////////////
  // Payment Positions vars //
  ////////////////////////////
  this.paymentPositionId = null;
  this.paymentPositionCreateOp = null;
  this.paymentPositionUpdateOp = null;
  this.paymentPositionDeleteOp = null;
  this.paymentPositionFiscalCode = null;
  this.paymentPositionCompanyName = null;
  this.paymentPositionUpdatedCompanyName = null;
});

/////////////////////////////
// Payment Positions steps //
/////////////////////////////
Given('a create operation on payment position table with id {string} and fiscal code {string} and company name {string} in GPD database', async function (id, fiscalCode, companyName) {
  await insertPaymentPosition(id, fiscalCode, companyName);
  this.paymentPositionId = id;
  this.paymentPositionFiscalCode = fiscalCode;
  this.paymentPositionCompanyName = companyName;
});

Given('an update operation on field company name with new value {string} on the same payment position in GPD database', async function (companyName) {
  await updatePaymentPosition(this.paymentPositionId, companyName);
  this.paymentPositionUpdatedCompanyName = companyName;
});

Given('a delete operation on the same payment position in GPD database', async function () {
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

Then('the payment position operations have id {int}', function (id) {
  assert.strictEqual(this.paymentPositionCreateOp.after.id, id);
  assert.strictEqual(this.paymentPositionUpdateOp.after.id, id);
  assert.strictEqual(this.paymentPositionDeleteOp.before.id, id);
});

Then('the operations have the fiscal code tokenized', function () {
  assert.notStrictEqual(this.paymentPositionCreateOp.after.fiscalCode, this.paymentPositionFiscalCode);
  assert.notStrictEqual(this.paymentPositionUpdateOp.after.fiscalCode, this.paymentPositionFiscalCode);
});

Then('the payment position update operation has the company name updated', function () {
  assert.notStrictEqual(this.paymentPositionUpdateOp.after.companyName, this.paymentPositionCompanyName);
  assert.strictEqual(this.paymentPositionUpdateOp.after.companyName, this.paymentPositionUpdatedCompanyName);
});

///////////////////////////
// Payment Options steps //
///////////////////////////

Given('a payment position with id {string} and fiscal code {string} and company name {string} in GPD database', async function (id, fiscalCode, companyName) {
  await insertPaymentPosition(id, fiscalCode, companyName);
  this.paymentPositionId = id;
});


Given('a create operation on payment option table with id {string} and description {string} and associated to payment position with id {int} in GPD database', async function (id, description, paymentPositionId) {
  await insertPaymentOption(id, description, paymentPositionId);
  this.paymentOptionId = id;
  this.paymentOptionDescription = description;
});

Given('an update operation on field description with new value {string} on the same payment option in GPD database', async function (description) {
  await updatePaymentOption(this.paymentOptionId, description);
  this.paymentOptionUpdatedDescription = description;
});

Given('a delete operation on the same payment option in GPD database', async function () {
  await deletePaymentOption(this.paymentOptionId);
});

When('the payment option operations have been properly published on data lake event hub after {int} ms', async function (time) {
  // boundary time spent by azure function to process event
  await sleep(time);
});

Then('the data lake topic returns the payment option {string} operation with id {string}', async function (operation, id) {
  let po = await readFromRedisWithKey(id);
  if (operation === "create") {
    this.paymentOptionCreateOp = JSON.parse(po);
    assert.strictEqual(this.paymentOptionCreateOp.op, "c");
  } else if (operation === "update") {
    this.paymentOptionUpdateOp = JSON.parse(po);
    assert.strictEqual(this.paymentOptionUpdateOp.op, "u");
  } else if (operation === "delete") {
    this.paymentOptionDeleteOp = JSON.parse(po);
    assert.strictEqual(this.paymentOptionDeleteOp.op, "d");
  }
});

Then('the payment option operations have id {int}', function (id) {
  assert.strictEqual(this.paymentOptionCreateOp.after.id, id);
  assert.strictEqual(this.paymentOptionUpdateOp.after.id, id);
  assert.strictEqual(this.paymentOptionDeleteOp.before.id, id);
});

Then('the payment option update operation has the description updated', function () {
  assert.notStrictEqual(this.paymentOptionUpdateOp.after.description, this.paymentOptionDescription);
  assert.strictEqual(this.paymentOptionUpdateOp.after.description, this.paymentOptionUpdatedDescription);
});