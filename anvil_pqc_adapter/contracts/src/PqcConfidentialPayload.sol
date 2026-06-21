// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.24;

/// @title PqcConfidentialPayload
/// @notice Carries a hybrid-encrypted (X25519 + ML-KEM-768) ciphertext bundle
/// as opaque bytes. Zero cryptographic logic on-chain — the contract only
/// stores/emits bytes it cannot read; only the intended recipient, holding
/// the matching private keys off-chain, can decrypt it.
contract PqcConfidentialPayload {

    struct Bundle {
        address sender;
        bytes ephemeralXPub;
        bytes kemEncapsulation;
        bytes nonce;
        bytes aesCiphertext;
    }

    mapping(uint256 => Bundle) public bundles;
    uint256 public nextBundleId;

    event PayloadSubmitted(uint256 indexed bundleId, address indexed sender);

    function submitPayload(
        bytes calldata ephemeralXPub,
        bytes calldata kemEncapsulation,
        bytes calldata nonce,
        bytes calldata aesCiphertext
    ) external returns (uint256 bundleId) {
        bundleId = nextBundleId++;
        bundles[bundleId] = Bundle({
            sender: msg.sender,
            ephemeralXPub: ephemeralXPub,
            kemEncapsulation: kemEncapsulation,
            nonce: nonce,
            aesCiphertext: aesCiphertext
        });
        emit PayloadSubmitted(bundleId, msg.sender);
    }
}
