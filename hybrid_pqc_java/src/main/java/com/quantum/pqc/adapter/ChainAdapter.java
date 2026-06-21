package com.quantum.pqc.adapter;

import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;

/**
 * Common shape every chain adapter implements. All signature/key operations
 * delegate to a KmsDriver — this interface never touches key material
 * directly, only chain-specific transaction construction and submission.
 *
 * TxIntent    - what the caller wants to happen (chain-agnostic)
 * PreparedTx  - the chain-specific unsigned payload built from that intent
 * SubmitResult- whatever the chain returns on submission (tx hash, receipt, etc.)
 */
public interface ChainAdapter<TxIntent, PreparedTx, SubmitResult> {

    /** Registers keyId's PQC public key as an on-chain identity (contract account, ledger record, etc). */
    String registerIdentity(String keyId) throws Exception;

    /** Builds the canonical unsigned payload for an intent — this is exactly what gets signed. */
    PreparedTx prepareTransaction(TxIntent intent) throws Exception;

    /** Delegates to KmsDriver.sign(keyId, hash(preparedTx)) — no key material here, just the call. */
    byte[] requestSignature(String keyId, PreparedTx preparedTx) throws GeneralSecurityException;

    /** Submits the signed payload using whatever enforcement model this chain supports. */
    CompletableFuture<SubmitResult> submitTransaction(PreparedTx preparedTx, byte[] signature) throws Exception;
}
