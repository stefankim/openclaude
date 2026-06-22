#!/system/bin/sh
# DockerDroid installer — runs as root on the device.
#
# Subcommands:
#   download   fetch + checksum-verify + extract Docker/containerd/runc into bin/
#   network    set up the loopback bridge / iptables rules (best effort)
#
# Environment:
#   DOCKERDROID_RUNTIME   DOCKER | PODMAN  (selects which binaries to stage)
#
# The app stages this script to /data/local/docker/install-docker.sh and invokes
# it with root. Keeping installation in plain shell makes it auditable and lets
# advanced users run it standalone.
set -eu

ROOT="/data/local/docker"
BIN="$ROOT/bin"
ARCH="$(uname -m)"
DOCKER_VERSION="${DOCKER_VERSION:-27.3.1}"
RUNTIME="${DOCKERDROID_RUNTIME:-DOCKER}"

log() { echo "[dockerdroid] $*"; }

verify_sha256() {
  # $1 = file, $2 = expected hash. Skips when hash is the release placeholder.
  file="$1"; expected="$2"
  [ "$expected" = "REPLACED_AT_RELEASE" ] && { log "WARN: checksum not pinned for $file"; return 0; }
  actual="$(sha256sum "$file" | awk '{print $1}')"
  if [ "$actual" != "$expected" ]; then
    log "CHECKSUM MISMATCH for $file: got $actual expected $expected"
    return 1
  fi
  log "verified $file"
}

download_docker() {
  case "$ARCH" in
    aarch64|arm64) dlarch="aarch64" ;;
    *) log "Unsupported architecture: $ARCH"; exit 2 ;;
  esac

  url="https://download.docker.com/linux/static/stable/${dlarch}/docker-${DOCKER_VERSION}.tgz"
  tgz="$ROOT/docker.tgz"

  log "downloading $url"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$url" -o "$tgz"
  else
    wget -qO "$tgz" "$url"
  fi

  # Hash pinned by CI; see BinarySource.kt.
  verify_sha256 "$tgz" "${DOCKER_SHA256:-REPLACED_AT_RELEASE}"

  log "extracting binaries"
  tar -xzf "$tgz" -C "$ROOT"
  # The static bundle ships dockerd, docker, containerd, containerd-shim-runc-v2,
  # ctr, runc, docker-init, docker-proxy.
  cp -f "$ROOT"/docker/* "$BIN"/
  chmod 700 "$BIN"/*
  rm -rf "$ROOT/docker" "$tgz"
  log "docker binaries installed to $BIN"
}

setup_network() {
  # Bridge networking on Android kernels is unreliable; daemon.json defaults to
  # bridge:none. We just ensure forwarding + a working loopback for host mode.
  echo 1 > /proc/sys/net/ipv4/ip_forward 2>/dev/null || log "WARN: cannot enable ip_forward"
  if command -v iptables >/dev/null 2>&1; then
    iptables -t nat -C POSTROUTING -s 172.17.0.0/16 -j MASQUERADE 2>/dev/null \
      || iptables -t nat -A POSTROUTING -s 172.17.0.0/16 -j MASQUERADE 2>/dev/null \
      || log "WARN: iptables MASQUERADE failed; use host networking"
  fi
  log "network configured (runtime=$RUNTIME)"
}

case "${1:-}" in
  download)
    if [ "$RUNTIME" = "PODMAN" ]; then
      log "PODMAN fallback selected — Docker static bundle still provides runc/containerd"
    fi
    download_docker
    ;;
  network) setup_network ;;
  *) log "usage: install-docker.sh {download|network}"; exit 1 ;;
esac
