package cfb.ict;

import java.math.BigInteger;
import java.util.Arrays;

public class Transaction {

    static final int SIGNATURE_OR_MESSAGE_OFFSET = 0, SIGNATURE_OR_MESSAGE_LENGTH = 6561;
    static final int EXTRA_DATA_DIGEST_OFFSET = SIGNATURE_OR_MESSAGE_OFFSET + SIGNATURE_OR_MESSAGE_LENGTH, EXTRA_DATA_DIGEST_LENGTH = 243;
    static final int ADDRESS_OFFSET = EXTRA_DATA_DIGEST_OFFSET + EXTRA_DATA_DIGEST_LENGTH, ADDRESS_LENGTH = 243;
    static final int VALUE_OFFSET = ADDRESS_OFFSET + ADDRESS_LENGTH, VALUE_LENGTH = 81;
    static final int ISSUANCE_TIMESTAMP_OFFSET = VALUE_OFFSET + VALUE_LENGTH, ISSUANCE_TIMESTAMP_LENGTH = 27;
    static final int TIMELOCK_LOWER_BOUND_OFFSET = ISSUANCE_TIMESTAMP_OFFSET + ISSUANCE_TIMESTAMP_LENGTH, TIMELOCK_LOWER_BOUND_LENGTH = 27;
    static final int TIMELOCK_UPPER_BOUND_OFFSET = TIMELOCK_LOWER_BOUND_OFFSET + TIMELOCK_LOWER_BOUND_LENGTH, TIMELOCK_UPPER_BOUND_LENGTH = 27;
    static final int BUNDLE_NONCE_OFFSET = TIMELOCK_UPPER_BOUND_OFFSET + TIMELOCK_UPPER_BOUND_LENGTH, BUNDLE_NONCE_LENGTH = 81;
    static final int TRUNK_TRANSACTION_HASH_OFFSET = BUNDLE_NONCE_OFFSET + BUNDLE_NONCE_LENGTH, TRUNK_TRANSACTION_HASH_LENGTH = 243;
    static final int BRANCH_TRANSACTION_HASH_OFFSET = TRUNK_TRANSACTION_HASH_OFFSET + TRUNK_TRANSACTION_HASH_LENGTH, BRANCH_TRANSACTION_HASH_LENGTH = 243;
    static final int TAG_OFFSET = BRANCH_TRANSACTION_HASH_OFFSET + BRANCH_TRANSACTION_HASH_LENGTH, TAG_LENGTH = 81;
    static final int ATTACHMENT_TIMESTAMP_OFFSET = TAG_OFFSET + TAG_LENGTH, ATTACHMENT_TIMESTAMP_LENGTH = 27;
    static final int ATTACHMENT_TIMESTAMP_LOWER_BOUND_OFFSET = ATTACHMENT_TIMESTAMP_OFFSET + ATTACHMENT_TIMESTAMP_LENGTH, ATTACHMENT_TIMESTAMP_LOWER_BOUND_LENGTH = 27;
    static final int ATTACHMENT_TIMESTAMP_UPPER_BOUND_OFFSET = ATTACHMENT_TIMESTAMP_LOWER_BOUND_OFFSET + ATTACHMENT_TIMESTAMP_LOWER_BOUND_LENGTH, ATTACHMENT_TIMESTAMP_UPPER_BOUND_LENGTH = 27;
    static final int TRANSACTION_NONCE_OFFSET = ATTACHMENT_TIMESTAMP_UPPER_BOUND_OFFSET + ATTACHMENT_TIMESTAMP_UPPER_BOUND_LENGTH, TRANSACTION_NONCE_LENGTH = 81;

    static final int LENGTH = TRANSACTION_NONCE_OFFSET + TRANSACTION_NONCE_LENGTH;
    static final int BUNDLE_ESSENCE_OFFSET = EXTRA_DATA_DIGEST_OFFSET, BUNDLE_ESSENCE_LENGTH = EXTRA_DATA_DIGEST_LENGTH + ADDRESS_LENGTH + VALUE_LENGTH + ISSUANCE_TIMESTAMP_LENGTH + TIMELOCK_LOWER_BOUND_LENGTH + TIMELOCK_UPPER_BOUND_LENGTH + BUNDLE_NONCE_LENGTH;

    static final Transaction NULL = new Transaction(new byte[LENGTH]);

    final byte[] signatureOrMessage;
    final Hash extraDataDigest;
    final Hash address;
    final BigInteger value;
    final long issuanceTimestamp;
    final long timelockLowerBound, timelockUpperBound;
    final byte[] bundleNonce;
    final Hash trunkTransactionHash, branchTransactionHash;
    final byte[] tag;
    final long attachmentTimestamp, attachmentTimestampLowerBound, attachmentTimestampUpperBound;
    final byte[] transactionNonce;

    final Hash hash;

    Transaction(final byte[] trits) {

        signatureOrMessage = Arrays.copyOfRange(trits, SIGNATURE_OR_MESSAGE_OFFSET, SIGNATURE_OR_MESSAGE_OFFSET + SIGNATURE_OR_MESSAGE_LENGTH);
        extraDataDigest = new Hash(trits, EXTRA_DATA_DIGEST_OFFSET);
        address = new Hash(trits, ADDRESS_OFFSET);
        value = Utils.value(trits, VALUE_OFFSET, VALUE_LENGTH);
        issuanceTimestamp = Utils.value(trits, ISSUANCE_TIMESTAMP_OFFSET, ISSUANCE_TIMESTAMP_LENGTH).longValueExact();
        timelockLowerBound = Utils.value(trits, TIMELOCK_LOWER_BOUND_OFFSET, TIMELOCK_LOWER_BOUND_LENGTH).longValueExact();
        timelockUpperBound = Utils.value(trits, TIMELOCK_UPPER_BOUND_OFFSET, TIMELOCK_UPPER_BOUND_LENGTH).longValueExact();
        bundleNonce = Arrays.copyOfRange(trits, BUNDLE_NONCE_OFFSET, BUNDLE_NONCE_OFFSET + BUNDLE_NONCE_LENGTH);
        trunkTransactionHash = new Hash(trits, TRUNK_TRANSACTION_HASH_OFFSET);
        branchTransactionHash = new Hash(trits, BRANCH_TRANSACTION_HASH_OFFSET);
        tag = Arrays.copyOfRange(trits, TAG_OFFSET, TAG_OFFSET + TAG_LENGTH);
        attachmentTimestamp = Utils.value(trits, ATTACHMENT_TIMESTAMP_OFFSET, ATTACHMENT_TIMESTAMP_LENGTH).longValueExact();
        attachmentTimestampLowerBound = Utils.value(trits, ATTACHMENT_TIMESTAMP_LOWER_BOUND_OFFSET, ATTACHMENT_TIMESTAMP_LOWER_BOUND_LENGTH).longValueExact();
        attachmentTimestampUpperBound = Utils.value(trits, ATTACHMENT_TIMESTAMP_UPPER_BOUND_OFFSET, ATTACHMENT_TIMESTAMP_UPPER_BOUND_LENGTH).longValueExact();
        transactionNonce = Arrays.copyOfRange(trits, TRANSACTION_NONCE_OFFSET, TRANSACTION_NONCE_OFFSET + TRANSACTION_NONCE_LENGTH);

        if (timelockLowerBound > timelockUpperBound
                || attachmentTimestamp < attachmentTimestampLowerBound || attachmentTimestamp > attachmentTimestampUpperBound) {

            throw new RuntimeException("Invalid transaction");
        }

        final byte[] hashTrits = new byte[Hash.LENGTH];
        final Curl curl = new Curl();
        curl.absorb(trits, 0, LENGTH);
        curl.squeeze(hashTrits, 0, hashTrits.length);
        hash = new Hash(hashTrits, 0);
    }

    void dump(final byte[] trits, final int offset) {

        System.arraycopy(signatureOrMessage, 0, trits, offset + SIGNATURE_OR_MESSAGE_OFFSET, SIGNATURE_OR_MESSAGE_LENGTH);
        System.arraycopy(extraDataDigest.trits, 0, trits, offset + EXTRA_DATA_DIGEST_OFFSET, EXTRA_DATA_DIGEST_LENGTH);
        System.arraycopy(address.trits, 0, trits, offset + ADDRESS_OFFSET, ADDRESS_LENGTH);
        System.arraycopy(Utils.trits(value, VALUE_LENGTH), 0, trits, offset + VALUE_OFFSET, VALUE_LENGTH);
        System.arraycopy(Utils.trits(BigInteger.valueOf(issuanceTimestamp), ISSUANCE_TIMESTAMP_LENGTH), 0, trits, offset + ISSUANCE_TIMESTAMP_OFFSET, ISSUANCE_TIMESTAMP_LENGTH);
        System.arraycopy(Utils.trits(BigInteger.valueOf(timelockLowerBound), TIMELOCK_LOWER_BOUND_LENGTH), 0, trits, offset + TIMELOCK_LOWER_BOUND_OFFSET, TIMELOCK_LOWER_BOUND_LENGTH);
        System.arraycopy(Utils.trits(BigInteger.valueOf(timelockUpperBound), TIMELOCK_UPPER_BOUND_LENGTH), 0, trits, offset + TIMELOCK_UPPER_BOUND_OFFSET, TIMELOCK_UPPER_BOUND_LENGTH);
        System.arraycopy(bundleNonce, 0, trits, offset + BUNDLE_NONCE_OFFSET, BUNDLE_NONCE_LENGTH);
        System.arraycopy(trunkTransactionHash.trits, 0, trits, offset + TRUNK_TRANSACTION_HASH_OFFSET, TRUNK_TRANSACTION_HASH_LENGTH);
        System.arraycopy(branchTransactionHash.trits, 0, trits, offset + BRANCH_TRANSACTION_HASH_OFFSET, BRANCH_TRANSACTION_HASH_LENGTH);
        System.arraycopy(tag, 0, trits, offset + TAG_OFFSET, TAG_LENGTH);
        System.arraycopy(Utils.trits(BigInteger.valueOf(attachmentTimestamp), ATTACHMENT_TIMESTAMP_LENGTH), 0, trits, offset + ATTACHMENT_TIMESTAMP_OFFSET, ATTACHMENT_TIMESTAMP_LENGTH);
        System.arraycopy(Utils.trits(BigInteger.valueOf(attachmentTimestampLowerBound), ATTACHMENT_TIMESTAMP_LOWER_BOUND_LENGTH), 0, trits, offset + ATTACHMENT_TIMESTAMP_LOWER_BOUND_OFFSET, ATTACHMENT_TIMESTAMP_LOWER_BOUND_LENGTH);
        System.arraycopy(Utils.trits(BigInteger.valueOf(attachmentTimestampUpperBound), ATTACHMENT_TIMESTAMP_UPPER_BOUND_LENGTH), 0, trits, offset + ATTACHMENT_TIMESTAMP_UPPER_BOUND_OFFSET, ATTACHMENT_TIMESTAMP_UPPER_BOUND_LENGTH);
        System.arraycopy(transactionNonce, 0, trits, offset + TRANSACTION_NONCE_OFFSET, TRANSACTION_NONCE_LENGTH);
    }
}
