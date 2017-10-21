package org.interledger.cryptoconditions;

import static org.interledger.cryptoconditions.CryptoConditionType.PREFIX_SHA256;

import org.interledger.cryptoconditions.der.DerEncodingException;
import org.interledger.cryptoconditions.der.DerOutputStream;
import org.interledger.cryptoconditions.der.DerTag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Implementation of a crypto condition based on a prefix, a sub-condition and the SHA-256
 * function.
 */
public interface PrefixSha256Condition extends CompoundCondition {

  /**
   * Constructs an instance of the condition.
   *
   * @param prefix           The prefix to use when creating the fingerprint.
   * @param maxMessageLength The maximum length of the message.
   * @param subcondition     A condition on which this condition depends.
   */
  static PrefixSha256Condition of(
      final byte[] prefix, final long maxMessageLength, final Condition subcondition
  ) {
    return new Impl(prefix, maxMessageLength, subcondition);
  }

  /**
   * Constructs an instance of the condition.
   * <p/>
   * Note this constructor is package-private because it is used primarily for testing purposes.
   *
   * @param cost        The cost of this condition.
   * @param fingerprint The calculated fingerprint.
   * @param subtypes    A set of condition rsa for the conditions that this one depends on.
   */
  static PrefixSha256Condition of(
      final long cost, final byte[] fingerprint, final EnumSet<CryptoConditionType> subtypes
  ) {
    return new Impl(cost, fingerprint, subtypes);
  }

  /**
   * An extension of {@link CompoundSha256Condition} that implements {@link PrefixSha256Condition}.
   */
  final class Impl extends CompoundSha256Condition implements PrefixSha256Condition {

    /**
     * Constructs an instance of the condition.
     *
     * @param prefix           The prefix to use when creating the fingerprint.
     * @param maxMessageLength The maximum length of the message.
     * @param subcondition     A condition on which this condition depends.
     */
    private Impl(
        final byte[] prefix, final long maxMessageLength, final Condition subcondition
    ) {
      super(
          PREFIX_SHA256,
          calculateCost(prefix, maxMessageLength, subcondition.getCost()),
          hashFingerprintContents(
              constructFingerprintContents(prefix, maxMessageLength, subcondition)
          ),
          calculateSubtypes(subcondition)
      );
    }

    /**
     * Constructs an instance of the condition.
     * <p/>
     * Note this constructor is package-private because it is used primarily for testing purposes.
     *
     * @param cost        The cost of this condition.
     * @param fingerprint The calculated fingerprint.
     * @param subtypes    A set of condition rsa for the conditions that this one depends on.
     */
    private Impl(
        final long cost, final byte[] fingerprint, final EnumSet<CryptoConditionType> subtypes
    ) {
      super(PREFIX_SHA256, cost, fingerprint, subtypes);
    }

    /**
     * Constructs the fingerprint for this condition.
     * <p/>
     * Note: This method is package-private as (opposed to private) for testing purposes.
     */
    static final byte[] constructFingerprintContents(
        final byte[] prefix, final long maxMessageLength, final Condition subcondition
    ) {
      Objects.requireNonNull(prefix);
      Objects.requireNonNull(subcondition);

      try {
        // Build prefix and subcondition
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DerOutputStream out = new DerOutputStream(baos);
        out.writeTaggedObject(0, prefix);
        out.writeTaggedObject(1, BigInteger.valueOf(maxMessageLength).toByteArray());
        out.writeTaggedConstructedObject(2, CryptoConditionWriter.writeCondition(subcondition));
        out.close();
        byte[] buffer = baos.toByteArray();

        // Wrap SEQUENCE
        baos = new ByteArrayOutputStream();
        out = new DerOutputStream(baos);
        out.writeEncoded(DerTag.CONSTRUCTED.getTag() + DerTag.SEQUENCE.getTag(), buffer);
        out.close();
        return baos.toByteArray();

      } catch (IOException e) {
        throw new UncheckedIOException("DER Encoding Error", e);
      } catch (DerEncodingException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Determines the cost associated with this condition. This is determined as length_of_prefix +
     * max_message_length + subcondition_cost + 1024
     *
     * @param prefix           The prefix included in this condition.
     * @param maxMessageLength The maximum length of the message.
     * @param subconditionCost The cost of the sub condition.
     *
     * @return The calculated cost of this condition.
     */
    private static final long calculateCost(
        final byte[] prefix, final long maxMessageLength, final long subconditionCost
    ) {
      return Objects.requireNonNull(prefix).length + maxMessageLength + subconditionCost + 1024;
    }

    /**
     * Determines the set of condition rsa that are ultimately held via the sub condition.
     *
     * @param subcondition The sub condition that this condition depends on.
     *
     * @return The set of condition rsa related to the sub condition.
     */
    private static EnumSet<CryptoConditionType> calculateSubtypes(Condition subcondition) {
      EnumSet<CryptoConditionType> subtypes = EnumSet.of(subcondition.getType());
      if (subcondition instanceof CompoundCondition) {
        subtypes.addAll(((CompoundCondition) subcondition).getSubtypes());
      }

      // Remove our own type
      if (subtypes.contains(PREFIX_SHA256)) {
        subtypes.remove(PREFIX_SHA256);
      }

      return subtypes;
    }
  }
}