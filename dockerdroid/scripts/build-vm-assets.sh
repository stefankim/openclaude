#!/usr/bin/env bash
# Build the native payload for DockerDroid's on-device VM backend:
#   1. a static qemu-system-aarch64 for Android arm64 (shipped as a jniLib), and
#   2. a guest = Linux kernel + a rootfs image with Docker preinstalled.
#
# This is the heavyweight, machine-specific step that the app's QemuVmManager and
# VmProvisioner consume. It is intentionally separate from the app build (large
# binaries, cross-compilation toolchains) and runs on a Linux build host with the
# Android NDK installed. The outputs are published as release assets whose SHA-256
# hashes are pinned into VmImages.kt.
#
# This script documents the pipeline; fill in the marked TODOs for your toolchain.
set -euo pipefail

: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to your NDK path}"
OUT="${OUT:-$(pwd)/vm-assets}"
mkdir -p "$OUT"

echo "[1/3] Cross-compile qemu-system-aarch64 for android-arm64 …"
# TODO: configure QEMU with the NDK toolchain, e.g.:
#   ./configure --target-list=aarch64-softmmu \
#       --cross-prefix="$ANDROID_NDK_HOME/.../aarch64-linux-android34-" \
#       --static --disable-kvm --enable-tcg
#   make -j"$(nproc)"
# Then rename the binary to the lib name the app execs:
#   cp build/qemu-system-aarch64 "$OUT/libqemu-system-aarch64.so"
# Place it at: app/src/main/jniLibs/arm64-v8a/libqemu-system-aarch64.so

echo "[2/3] Build the guest kernel (arm64 'virt', virtio drivers, overlayfs, cgroups) …"
# TODO: build or fetch a minimal aarch64 kernel image → "$OUT/vmlinuz"

echo "[3/3] Build a Docker-enabled rootfs (raw image) …"
# Recommended: Alpine + the 'docker' package, with an init service that runs:
#   dockerd -H tcp://0.0.0.0:2375 --storage-driver overlay2
# Produce a raw ext4 image, gzip it:
#   gzip -c rootfs.img > "$OUT/docker-rootfs-arm64.img.gz"

echo "Outputs in $OUT. Next:"
echo "  - copy libqemu-system-aarch64.so → app/src/main/jniLibs/arm64-v8a/"
echo "  - upload vmlinuz + docker-rootfs-arm64.img.gz as release assets"
echo "  - pin their sha256 into app/.../vm/VmImages.kt"
