package com.corekms.adapter;

public interface ChainAdapter<Intent, Prepared, Receipt> {

    /** Registers keyId's public key as an identity on this chain (a contract, an account, etc). */
    String registerIdentity(String keyId) throws Exception;

    /** Builds the canonical unsigned payload for an intent — exactly what gets signed/encrypted next. */
    Prepared prepareTransaction(Intent intent) throws Exception;

    /** Delegates to KmsDriver — identical call shape regardless of which chain this adapter is for. */
    byte[] requestSignature(String keyId, Prepared prepared) throws Exception;

    /** Submits the signed/prepared payload using whatever this chain's enforcement model is. */
    Receipt submitTransaction(Prepared prepared, byte[] signature) throws Exception;
}
