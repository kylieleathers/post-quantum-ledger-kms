#!/usr/bin/env bash
# Cross-chain proof: one core_kms KmsDriver key/signature, carried through
# the oracle-attestation pattern on a chain that has nothing to do with
# Canton. Mirrors PqcIdentity.daml's PqcIdentity/PqcAttestationRequest/
# PqcAttestation flow, but on a local Anvil EVM chain via PqcAttestation.sol.
#
# Requires: anvil, forge, cast (foundryup), java 17+, and core_kms +
# anvil_pqc_adapter/cli already built (`mvn install` in core_kms/, then
# `mvn package` in cli/).
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

export PATH="$HOME/.foundry/bin:$PATH"
JAVA=${JAVA:-/opt/homebrew/opt/openjdk@17/bin/java}
RPC=http://127.0.0.1:8545

DEPLOYER_KEY=0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80
ORACLE_KEY=0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d
ORACLE_ADDR=0x70997970C51812dc3A010C7d01b50e0d17dc79C8

echo "--- starting anvil ---"
anvil > anvil.log 2>&1 &
ANVIL_PID=$!
trap 'kill $ANVIL_PID 2>/dev/null || true' EXIT
sleep 1

echo "--- deploying PqcAttestation.sol (oracle = account 1) ---"
DEPLOY_OUT=$(cd contracts && forge create src/PqcAttestation.sol:PqcAttestation \
  --rpc-url $RPC --private-key $DEPLOYER_KEY --broadcast \
  --constructor-args $ORACLE_ADDR 2>&1)
echo "$DEPLOY_OUT"
CONTRACT=$(echo "$DEPLOY_OUT" | grep "Deployed to:" | awk '{print $3}')
echo "contract: $CONTRACT"

echo ""
echo "--- core_kms: generate ML-DSA key + sign payload (chain-agnostic step) ---"
OUT=$($JAVA -jar cli/target/anvil-pqc-cli-jar-with-dependencies.jar generate-and-sign alice "transfer 100 units to bob")
PUBKEY=0x$(echo "$OUT" | sed -n '1p')
HASH=0x$(echo "$OUT" | sed -n '2p')
SIG=0x$(echo "$OUT" | sed -n '3p')
echo "payload hash: $HASH"

echo ""
echo "--- submitting request on-chain ---"
cast send "$CONTRACT" "submitRequest(bytes32,bytes,bytes)" "$HASH" "$PUBKEY" "$SIG" \
  --rpc-url $RPC --private-key $DEPLOYER_KEY > /dev/null
echo "submitted."

echo ""
echo "--- oracle: independently re-verifies via core_kms (off-chain) ---"
VERDICT=$($JAVA -jar cli/target/anvil-pqc-cli-jar-with-dependencies.jar verify alice "${PUBKEY#0x}" "${HASH#0x}" "${SIG#0x}")
echo "verdict: $VERDICT"
if [ "$VERDICT" != "VALID" ]; then
  echo "FAIL: expected VALID"; exit 1
fi

echo ""
echo "--- oracle: submits attestation, signed by its own account key ---"
cast send "$CONTRACT" "attest(uint256,bool)" 0 true \
  --rpc-url $RPC --private-key $ORACLE_KEY > /dev/null

RESULT=$(cast call "$CONTRACT" "isVerified(uint256)(bool)" 0 --rpc-url $RPC)
echo "isVerified(0): $RESULT"

if [ "$RESULT" = "true" ]; then
  echo ""
  echo "PROOF SUCCEEDED: one core_kms key/signature, carried through the same"
  echo "oracle-attestation pattern on an unrelated chain, no core_kms changes."
else
  echo "FAIL: expected true"; exit 1
fi
