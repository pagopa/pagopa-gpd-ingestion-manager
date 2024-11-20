Feature: All about ingestion events consumed by Azure functions gpd-ingestion-manager

  Scenario: CRUD operations on payment position table in GPD database are published into data lake event hub
    Given a create operation on payment position table with id '11111111' and fiscal code '77777777777' and company name 'SkyLab Inc.' in GPD database
    And an update operation on field company name with new value 'Updated company name' on the same payment position in GPD database
    And a delete operation on the same payment position in GPD database
    When the payment position operations have been properly published on data lake event hub after 10000 ms
    Then the data lake topic returns the payment position 'create' operation with id '11111111-ing-c'
    And the data lake topic returns the payment position 'update' operation with id '11111111-ing-u'
    And the data lake topic returns the payment position 'delete' operation with id '11111111-ing-d'
    And the payment position operations have id 11111111
    And the operations have the fiscal code tokenized
    And the payment position update operation has the company name updated

  Scenario: CRUD operations on payment option table in GPD database are published into data lake event hub
    Given a payment position with id '22222222' and fiscal code '77777777777' and company name 'SkyLab Inc.' in GPD database
    And a create operation on payment option table with id '21111111' and description 'Canone Unico Patrimoniale - SkyLab Inc.' and associated to payment position with id 22222222 in GPD database
    And an update operation on field description with new value 'Updated description' on the same payment option in GPD database
    And a delete operation on the same payment option in GPD database
    When the payment option operations have been properly published on data lake event hub after 10000 ms
    Then the data lake topic returns the payment option 'create' operation with id '21111111-ing-c'
    And the data lake topic returns the payment option 'update' operation with id '21111111-ing-u'
    And the data lake topic returns the payment option 'delete' operation with id '21111111-ing-d'
    And the payment option operations have id 21111111
    And the payment option update operation has the description updated