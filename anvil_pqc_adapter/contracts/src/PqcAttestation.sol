// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.24;

/// @title PqcAttestation
/// @notice EVM-side mirror of Canton's oracle-attestation pattern
/// (PqcIdentity.daml: PqcIdentity / PqcAttestationRequest / PqcAttestation).
/// The same core_kms KmsDriver key and signature carry through this
/// contract's request/attest flow on a different chain, unchanged.
///
/// Oracle-attested, not consensus-enforced — no on-chain ML-DSA
/// verification here (see besu_pqc_precompile for that path). The oracle's
/// attestation is an ordinary Ethereum transaction signed by its own
/// account key, mirroring Canton's Accept choice.
contract PqcAttestation {
    address public immutable oracle;

    struct Request {
        address requester;
        bytes32 payloadHash;
        bytes mlDsaPublicKey;
        bytes mlDsaSignature;
        bool attested;
        bool verified;
    }

    mapping(uint256 => Request) public requests;
    uint256 public nextRequestId;

    event RequestSubmitted(uint256 indexed requestId, address indexed requester, bytes32 payloadHash);
    event Attested(uint256 indexed requestId, bool verified);

    constructor(address _oracle) {
        oracle = _oracle;
    }

    /// @notice Submit a payload hash + ML-DSA signature + public key for the oracle to review.
    /// No on-chain crypto — just records the claim, same role as Canton's
    /// PqcAttestationRequest contract.
    function submitRequest(
        bytes32 payloadHash,
        bytes calldata mlDsaPublicKey,
        bytes calldata mlDsaSignature
    ) external returns (uint256 requestId) {
        requestId = nextRequestId++;
        requests[requestId] = Request({
            requester: msg.sender,
            payloadHash: payloadHash,
            mlDsaPublicKey: mlDsaPublicKey,
            mlDsaSignature: mlDsaSignature,
            attested: false,
            verified: false
        });
        emit RequestSubmitted(requestId, msg.sender, payloadHash);
    }

    /// @notice Oracle calls this after independently verifying the ML-DSA
    /// signature off-chain (via KmsDriver.verify). The contract only
    /// enforces that `oracle` signed the tx — it has no idea whether the
    /// ML-DSA check itself was correct, same caveat as the Canton version.
    function attest(uint256 requestId, bool verified) external {
        require(msg.sender == oracle, "PqcAttestation: only oracle");
        require(!requests[requestId].attested, "PqcAttestation: already attested");

        requests[requestId].attested = true;
        requests[requestId].verified = verified;
        emit Attested(requestId, verified);
    }

    function isVerified(uint256 requestId) external view returns (bool) {
        return requests[requestId].attested && requests[requestId].verified;
    }
}
