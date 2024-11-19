Feature: All about ingestion events consumed by Azure functions gpd-ingestion-manager

  Scenario: CRUD operations on payment position table in GPD database are published into data lake event hub
    Given a create operation on payment position table with id '11111121' and fiscal code '77777777777' and company name 'SkyLab Inc.' in GPD database
    And an update operation on field company name with new value 'Updated company name' on the same payment position in GPD database
    And a delete operation on the same payment position in GPD database
    When the payment position operations have been properly published on data lake event hub after 10000 ms
    Then the data lake topic returns the payment position 'create' operation with id '11111121-c'
    And the data lake topic returns the payment position 'update' operation with id '11111121-u'
    And the data lake topic returns the payment position 'delete' operation with id '11111121-d'
    And the operations have id 11111121
    And the operations have the fiscal code tokenized
    And the payment position update operation has the company name updated