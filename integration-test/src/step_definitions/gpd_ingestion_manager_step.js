const assert = require('assert');
const { After, Given, When, Then, setDefaultTimeout } = require('@cucumber/cucumber');
const { sleep } = require("./common");
const { readFromRedisWithKey } = require("./redis_client");
const { insertPaymentPosition, updatePaymentPosition, deletePaymentPosition, insertPaymentOption, updatePaymentOption, deletePaymentOption } = require("./pg_gpd_client");

// set timeout for Hooks function, it allows to wait for long task
setDefaultTimeout(360 * 1000);

// initialize variables
this.paymentPositionId = null;
this.paymentPosition = null;
this.paymentPositionFiscalCode = null

// After each Scenario
After(async function () {
    // remove event
    if (this.paymentPositionId != null) {
        await deletePaymentPosition(this.paymentPositionId);
    }

    this.listOfPaymentOptionId = null;
    this.paymentPositionFiscalCode = null;
});

Given('a payment position is created with id {string} and fiscal code {string} published in GPD database', async function (id, fiscalCode) {
    await insertPaymentPosition(id, fiscalCode);
    this.paymentPositionId = id;
    this.paymentPositionFiscalCode = fiscalCode;
  });

  When('the payment position operation has been properly published on data lake event hub after {int} ms', async function (time) {
  // boundary time spent by azure function to process event
    await sleep(time);
  });

  Then('the data lake topic returns the payment position with id {string}', async function (id) {
    pp = await readFromRedisWithKey(id);
    this.paymentPosition = JSON.parse(pp);
  });

  Then('the payment position has id {int}', function (id) {
    assert.strictEqual(this.paymentPosition.op, "c");
    assert.strictEqual(this.paymentPosition.after.id, id);
  });

  Then('the payment position has the fiscal code tokenized', function () {
    assert.notStrictEqual(this.paymentPosition.after.fiscalCode, this.paymentPositionFiscalCode);
  });