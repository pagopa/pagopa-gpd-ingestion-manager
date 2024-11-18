Feature: All about ingestion events consumed by Azure functions gpd-ingestion-manager

  Scenario: an operation on payment position table in GPD database is published into data lake event hub
    Given a payment position is created with id '11111121' and fiscal code '77777777777' published in GPD database
    When the payment position operation has been properly published on data lake event hub after 10000 ms
    Then the data lake topic returns the payment position with id '11111121-c'
    And the payment position has id 11111121
    And the payment position has the fiscal code tokenized
    