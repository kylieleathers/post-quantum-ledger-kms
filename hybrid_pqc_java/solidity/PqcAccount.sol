// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.24;

/// @title PqcAccount
/// @notice Smart-contract account whose authorization check is an ML-DSA
/// signature instead of secp256k1. The relayer EOA submitting the
/// surrounding Ethereum transaction only pays gas — authorization happens
/// entirely inside execute(), re-run by every validator as ordinary EVM
/// execution, making PQC verification consensus-enforced without changing
/// Besu's base protocol.
///
/// Verification is delegated to a custom precompile (MlDsaVerifyPrecompile)
/// rather than implemented in Solidity, since hand-rolled lattice-crypto
/// Solidity is easy to get subtly wrong. This contract only does
/// bookkeeping: nonce, public key storage, dispatch.
contract PqcAccount {
    address public constant ML_DSA_VERIFY_PRECOMPILE = address(0x0100); // pick an address not used by existing precompiles

    bytes public mlDsaPublicKey;
    uint256 public nonce;

    event Executed(address indexed target, uint256 value, bytes data, uint256 nonce);

    constructor(bytes memory _mlDsaPublicKey) {
        mlDsaPublicKey = _mlDsaPublicKey;
    }

    /// @param target   address to call
    /// @param value    wei to forward
    /// @param data     calldata for the target call
    /// @param signature ML-DSA signature over keccak256(abi.encode(target, value, data, nonce, address(this)))
    function execute(
        address target,
        uint256 value,
        bytes calldata data,
        bytes calldata signature
    ) external returns (bytes memory) {
        bytes32 payloadHash = keccak256(abi.encode(target, value, data, nonce, address(this)));

        require(_verifyMlDsa(mlDsaPublicKey, payloadHash, signature), "PqcAccount: invalid ML-DSA signature");

        nonce += 1;

        (bool success, bytes memory returnData) = target.call{value: value}(data);
        require(success, "PqcAccount: target call failed");

        emit Executed(target, value, data, nonce - 1);
        return returnData;
    }

    function _verifyMlDsa(
        bytes memory pubKey,
        bytes32 payloadHash,
        bytes memory signature
    ) internal view returns (bool) {
        bytes memory input = abi.encodePacked(
            uint32(pubKey.length), pubKey,
            uint32(32), payloadHash,
            uint32(signature.length), signature
        );

        (bool ok, bytes memory result) = ML_DSA_VERIFY_PRECOMPILE.staticcall(input);
        if (!ok || result.length != 32) return false;

        return uint256(bytes32(result)) == 1;
    }

    receive() external payable {}
}
