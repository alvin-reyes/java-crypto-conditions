package org.interledger.cryptoconditions;

import java.util.EnumSet;
import java.util.List;

/**
 * A helper class for asserting crypto conditions.
 *
 * @deprecated Determine if this class should be removed since issues #52 allows URIs to be compared
 *     for string equality.
 */
@Deprecated
public class CryptoConditionAssert {

  /**
   * Asserts the the set of rsa given are equal.
   *
   * @param message  A detail message to record if the assertion fails.
   * @param expected A list of expected condition rsa.
   * @param actual   A set of condition rsa to compare against the ones expected.
   */
  public static void assertSetOfTypesIsEqual(String message, List<String> expected,
      EnumSet<CryptoConditionType> actual) {
    EnumSet<CryptoConditionType> expectedSet = CryptoConditionType
        .getEnumOfTypesFromString(String.join(",", expected.toArray(new String[expected.size()])));

    if (!expectedSet.containsAll(actual)) {
      throw new AssertionError(message + " - expected does not contain all values from actual.");
    }
    expectedSet.removeAll(actual);
    if (!expectedSet.isEmpty()) {
      throw new AssertionError(message + " - expected contains values not in actual.");
    }
  }

}
