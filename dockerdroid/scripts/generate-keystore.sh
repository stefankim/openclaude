#!/usr/bin/env bash
# Generate a release keystore for DockerDroid and print the GitHub secrets to set,
# so CI (dockerdroid-release.yml) produces a *signed* release APK instead of the
# debug-signed fallback.
#
# Usage: scripts/generate-keystore.sh [output.jks]
# Then add the four printed values as repository secrets:
#   DOCKERDROID_KEYSTORE_BASE64, DOCKERDROID_STORE_PASSWORD,
#   DOCKERDROID_KEY_ALIAS, DOCKERDROID_KEY_PASSWORD
set -euo pipefail

OUT="${1:-dockerdroid-release.jks}"
ALIAS="${DOCKERDROID_KEY_ALIAS:-dockerdroid}"

# Prompt for passwords without echoing.
read -rsp "Store password: " STORE_PW; echo
read -rsp "Key password (blank = same as store): " KEY_PW; echo
KEY_PW="${KEY_PW:-$STORE_PW}"

keytool -genkeypair -v \
  -keystore "$OUT" \
  -alias "$ALIAS" \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$STORE_PW" -keypass "$KEY_PW" \
  -dname "CN=DockerDroid, OU=DockerDroid, O=DockerDroid, L=, ST=, C=US"

echo
echo "== Add these as GitHub repository secrets =="
echo "DOCKERDROID_KEYSTORE_BASE64 = (paste the line below)"
base64 -w0 "$OUT" 2>/dev/null || base64 "$OUT" | tr -d '\n'; echo
echo "DOCKERDROID_STORE_PASSWORD  = $STORE_PW"
echo "DOCKERDROID_KEY_ALIAS       = $ALIAS"
echo "DOCKERDROID_KEY_PASSWORD    = $KEY_PW"
echo
echo "Keystore written to $OUT — keep it safe and OUT of git."
