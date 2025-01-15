const assert = require('assert');
const { After, Given, When, Then, setDefaultTimeout, AfterAll } = require('@cucumber/cucumber');
const { sleep } = require("./common");
const { readFromRedisWithKey, shutDownClient } = require("./redis_client");
const { shutDownPool, insertPaymentPosition, updatePaymentPosition, deletePaymentPosition, insertPaymentOption, updatePaymentOption, deletePaymentOption, insertTransfer, updateTransfer, deleteTransfer } = require("./pg_gpd_client");

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
// Payment Options vars  //
///////////////////////////
this.paymentOptionId = null;
this.paymentOptionDescription = null;
this.paymentOptionUpdatedDescription = null;
this.paymentOptionCreateOp = null;
this.paymentOptionUpdateOp = null;
this.paymentOptionDeleteOp = null;

////////////////////
// Transfer vars  //
////////////////////
this.transferId = null;
this.transferCategory = null;
this.transferUpdatedCategory = null;
this.transferCreateOp = null;
this.transferUpdateOp = null;
this.transferDeleteOp = null;

AfterAll(async function () {
  shutDownPool();
  shutDownClient();
});

// After each Scenario
After(async function () {
  // remove event
  if (this.transferId != null) {
    await deleteTransfer(this.transferId);
  }
  if (this.paymentOptionId != null) {
    await deletePaymentOption(this.paymentOptionId);
  }
  if (this.paymentPositionId != null) {
    await deletePaymentPosition(this.paymentPositionId);
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

  ///////////////////////////
  // Payment Options vars  //
  ///////////////////////////
  this.paymentOptionId = null;
  this.paymentOptionDescription = null;
  this.paymentOptionUpdatedDescription = null;
  this.paymentOptionCreateOp = null;
  this.paymentOptionUpdateOp = null;
  this.paymentOptionDeleteOp = null;

  ////////////////////
  // Transfer vars  //
  ////////////////////
  this.transferId = null;
  this.transferCategory = null;
  this.transferUpdatedCategory = null;
  this.transferCreateOp = null;
  this.transferUpdateOp = null;
  this.transferDeleteOp = null;
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
  let operationMessage = await readFromRedisWithKey(id);
  let pp = JSON.parse(operationMessage).value;
  if (operation === "create") {
    this.paymentPositionCreateOp = pp;
    assert.strictEqual(this.paymentPositionCreateOp.op, "c");
  } else if (operation === "update") {
    this.paymentPositionUpdateOp = pp;
    assert.strictEqual(this.paymentPositionUpdateOp.op, "u");
  } else if (operation === "delete") {
    this.paymentPositionDeleteOp = pp;
    assert.strictEqual(this.paymentPositionDeleteOp.op, "d");
  }
});

Then('the payment position operations have id {int}', function (id) {
  assert.strictEqual(this.paymentPositionCreateOp.after.id, id);
  assert.strictEqual(this.paymentPositionUpdateOp.after.id, id);
  assert.strictEqual(this.paymentPositionDeleteOp.before.id, id);
});

Then('the operations have the fiscal code tokenized', function () {
  assert.notStrictEqual(this.paymentPositionCreateOp.after.fiscal_code, undefined);
  assert.notStrictEqual(this.paymentPositionCreateOp.after.fiscal_code, this.paymentPositionFiscalCode);
  assert.notStrictEqual(this.paymentPositionUpdateOp.after.fiscal_code, undefined);
  assert.notStrictEqual(this.paymentPositionUpdateOp.after.fiscal_code, this.paymentPositionFiscalCode);
});

Then('the payment position update operation has the company name updated', function () {
  assert.notStrictEqual(this.paymentPositionUpdateOp.after.company_name, undefined);
  assert.notStrictEqual(this.paymentPositionUpdateOp.after.company_name, this.paymentPositionCompanyName);
  assert.strictEqual(this.paymentPositionUpdateOp.after.company_name, this.paymentPositionUpdatedCompanyName);
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
  let operationMessage = await readFromRedisWithKey(id);
  let po = JSON.parse(operationMessage).value;
  if (operation === "create") {
    this.paymentOptionCreateOp = po;
    assert.strictEqual(this.paymentOptionCreateOp.op, "c");
  } else if (operation === "update") {
    this.paymentOptionUpdateOp = po;
    assert.strictEqual(this.paymentOptionUpdateOp.op, "u");
  } else if (operation === "delete") {
    this.paymentOptionDeleteOp = po;
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

////////////////////
// Transfer steps //
////////////////////
Given('a payment option with id {string} and description {string} and associated to payment position with id {int} in GPD database', async function (id, description, paymentPositionId) {
  await insertPaymentOption(id, description, paymentPositionId);
  this.paymentOptionId = id;
});

Given('a create operation on transfer table with id {string} and category {string} and associated to payment option with id {int} in GPD database', async function (id, category, paymentOptionId) {
  await insertTransfer(id, category, paymentOptionId);
  this.transferId = id;
  this.transferCategory = category;
});

Given('an update operation on field category with new value {string} on the same transfer in GPD database', async function (category) {
  await updateTransfer(this.transferId, category);
  this.transferUpdatedCategory = category;
});

Given('a delete operation on the same transfer in GPD database', async function () {
  await deleteTransfer(this.transferId);
});

When('the transfer operations have been properly published on data lake event hub after {int} ms', async function (time) {
  // boundary time spent by azure function to process event
  await sleep(time);
});

Then('the data lake topic returns the transfer {string} operation with id {string}', async function (operation, id) {
  let operationMessage = await readFromRedisWithKey(id);
  let po = JSON.parse(operationMessage).value;
  if (operation === "create") {
    this.transferCreateOp = po;
    assert.strictEqual(this.transferCreateOp.op, "c");
  } else if (operation === "update") {
    this.transferUpdateOp = po;
    assert.strictEqual(this.transferUpdateOp.op, "u");
  } else if (operation === "delete") {
    this.transferDeleteOp = po;
    assert.strictEqual(this.transferDeleteOp.op, "d");
  }
});

Then('the transfer operations have id {int}', function (id) {
  assert.strictEqual(this.transferCreateOp.after.id, id);
  assert.strictEqual(this.transferUpdateOp.after.id, id);
  assert.strictEqual(this.transferDeleteOp.before.id, id);
});

Then('the transfer update operation has the category updated', function () {
  assert.notStrictEqual(this.transferUpdateOp.after.category, this.transferCategory);
  assert.strictEqual(this.transferUpdateOp.after.category, this.transferUpdatedCategory);
});