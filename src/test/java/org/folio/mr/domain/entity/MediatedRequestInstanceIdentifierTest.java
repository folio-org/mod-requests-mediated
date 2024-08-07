package org.folio.mr.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MediatedRequestInstanceIdentifierTest {
  @ParameterizedTest
  @CsvSource(value = {
    // Non-null values
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, valueA, true",
    "bb97a683-d710-4c73-9010-a85c5160e299, 61e01a2a-da55-4d52-a286-b58e9b92436c, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, 61e01a2a-da55-4d52-a286-b58e9b92436c, 70a334e2-e295-4105-aa77-56fc731f147d, c87e8af7-a4e5-4b9c-8c40-d0e12d03dff9, valueA, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, 61e01a2a-da55-4d52-a286-b58e9b92436c, 70a334e2-e295-4105-aa77-56fc731f147d, c87e8af7-a4e5-4b9c-8c40-d0e12d03dff9, valueA, valueB, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, c87e8af7-a4e5-4b9c-8c40-d0e12d03dff9, valueA, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, c87e8af7-a4e5-4b9c-8c40-d0e12d03dff9, valueA, valueB, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, valueB, false",
    // Null values
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, null, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, true",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, null, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, null, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, null, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, null, null, valueA, valueA, true",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, null, null, valueA, null, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, null, null, null, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, null, null, null, null, true",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, null, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, null, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, null, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, null, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, null, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, null, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, null, null, valueA, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, null, null, valueA, null, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, null, null, null, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, null, null, null, null, null, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, valueA, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, null, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, valueA, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, null, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, valueA, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, null, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, valueA, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, null, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, null, null, valueA, valueA, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, null, null, valueA, null, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, null, null, null, valueA, false",
    "null, bb97a683-d710-4c73-9010-a85c5160e299, null, null, null, null, false",
    "null, null, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, valueA, true",
    "null, null, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, null, false",
    "null, null, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, false",
    "null, null, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, true",
    "null, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, valueA, false",
    "null, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, null, false",
    "null, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, valueA, false",
    "null, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, null, false",
    "null, null, null, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, valueA, false",
    "null, null, null, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, null, false",
    "null, null, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, valueA, false",
    "null, null, null, 70a334e2-e295-4105-aa77-56fc731f147d, null, null, false",
    "null, null, null, null, valueA, valueA, true",
    "null, null, null, null, valueA, null, false",
    "null, null, null, null, null, valueA, false",
    "null, null, null, null, null, null, true",
  })
  void testEqualsMethod(String mediatedRequestIdA, String mediatedRequestIdB,
    String identifierTypeIdA, String identifierTypeIdB, String valueA, String valueB, boolean equals) {

    var mediatedRequestA = new MediatedRequestEntity();
    mediatedRequestA.setId(uuidFromString(mediatedRequestIdA));
    var identifierA = new MediatedRequestInstanceIdentifier();
    identifierA.setMediatedRequest(mediatedRequestA);
    identifierA.setIdentifierTypeId(uuidFromString(identifierTypeIdA));
    identifierA.setValue(valueA);

    var mediatedRequestB = new MediatedRequestEntity();
    mediatedRequestB.setId(uuidFromString(mediatedRequestIdB));
    var identifierB = new MediatedRequestInstanceIdentifier();
    identifierB.setMediatedRequest(mediatedRequestB);
    identifierB.setIdentifierTypeId(uuidFromString(identifierTypeIdB));
    identifierB.setValue(valueB);

    assertEquals(equals, identifierA.equals(identifierB));
  }

  private UUID uuidFromString(String uuidString) {
    return uuidString == null || "null".equals(uuidString)
      ? null
      : UUID.fromString(uuidString);
  }
}
