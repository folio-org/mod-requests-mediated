package org.folio.mr.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MediatedRequestInstanceIdentifierTest {
  @ParameterizedTest
  @CsvSource(value = {
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, valueA, true",
    "bb97a683-d710-4c73-9010-a85c5160e299, 61e01a2a-da55-4d52-a286-b58e9b92436c, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, 61e01a2a-da55-4d52-a286-b58e9b92436c, 70a334e2-e295-4105-aa77-56fc731f147d, c87e8af7-a4e5-4b9c-8c40-d0e12d03dff9, valueA, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, 61e01a2a-da55-4d52-a286-b58e9b92436c, 70a334e2-e295-4105-aa77-56fc731f147d, c87e8af7-a4e5-4b9c-8c40-d0e12d03dff9, valueA, valueB, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, c87e8af7-a4e5-4b9c-8c40-d0e12d03dff9, valueA, valueA, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, c87e8af7-a4e5-4b9c-8c40-d0e12d03dff9, valueA, valueB, false",
    "bb97a683-d710-4c73-9010-a85c5160e299, bb97a683-d710-4c73-9010-a85c5160e299, 70a334e2-e295-4105-aa77-56fc731f147d, 70a334e2-e295-4105-aa77-56fc731f147d, valueA, valueB, false",
  })
  void testEqualsMethod(String mediatedRequestIdA, String mediatedRequestIdB,
    String identifierTypeIdA, String identifierTypeIdB, String valueA, String valueB, boolean equals) {

    var mediatedRequestA = new MediatedRequestEntity();
    mediatedRequestA.setId(UUID.fromString(mediatedRequestIdA));
    var identifierA = new MediatedRequestInstanceIdentifier();
    identifierA.setMediatedRequest(mediatedRequestA);
    identifierA.setIdentifierTypeId(UUID.fromString(identifierTypeIdA));
    identifierA.setValue(valueA);

    var mediatedRequestB = new MediatedRequestEntity();
    mediatedRequestB.setId(UUID.fromString(mediatedRequestIdB));
    var identifierB = new MediatedRequestInstanceIdentifier();
    identifierB.setMediatedRequest(mediatedRequestB);
    identifierB.setIdentifierTypeId(UUID.fromString(identifierTypeIdB));
    identifierB.setValue(valueB);

    assertEquals(equals, identifierA.equals(identifierB));
  }
}
