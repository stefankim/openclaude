# On-device Docker VM (no root)

DockerDroid can run a **real Docker Engine on the phone without root** by booting a
guest Linux kernel inside QEMU. The guest has its own kernel — so it has the
namespaces, cgroups, and overlayfs that the Android host kernel won't expose without
root — and the app talks to the guest's `dockerd` over loopback TCP.

This complements (does not replace) the **SSH remote mode**; both ship in the app.

## Architecture

```
DockerDroid app
  ├─ QemuVmManager ── execs ──►  libqemu-system-aarch64.so  (jniLib, no root)
  │                                   └─ boots: vmlinuz + rootfs.img (Docker preinstalled)
  │                                        └─ dockerd -H tcp://0.0.0.0:2375
  └─ DockerApiClient.tcp("127.0.0.1", 2375)  ◄── QEMU user-net hostfwd ──┘
```

- `vm/QemuVmManager.kt` — start/stop, boot-log capture, readiness polling, state Flow.
- `vm/VmProvisioner.kt` — downloads + SHA-256-verifies the guest kernel/rootfs into
  app storage (they're data, not executed).
- `vm/VmImages.kt` — asset list, guest port/mem/cpu.
- `data/remote/ConnectionManager.kt` — `Connection.Vm` mode points the active client
  at `127.0.0.1:2375`.
- UI: Welcome → "Run Docker in a local VM (no root)" → `VmScreen` (start/stop, console).

## Why two payloads, packaged differently

| Payload | Where it lives | Why |
|---|---|---|
| `qemu-system-aarch64` | **jniLib** (`app/src/main/jniLibs/arm64-v8a/libqemu-system-aarch64.so`) | Android only allows executing binaries from the read-only `nativeLibraryDir` (W^X / SELinux). `useLegacyPackaging=true` extracts it at install. |
| `vmlinuz` + `rootfs.img` | **downloaded** to `filesDir/vm/` | They're read by QEMU as files, never executed, so they can be fetched at runtime and checksum-verified. |

## Building the native payload

The app ships the *engine and wiring*; the native binaries are produced by
[`scripts/build-vm-assets.sh`](../scripts/build-vm-assets.sh) on a Linux build host
with the Android NDK:

1. Cross-compile a **static** `qemu-system-aarch64` (`--disable-kvm --enable-tcg`),
   drop it at `app/src/main/jniLibs/arm64-v8a/libqemu-system-aarch64.so`.
2. Build an arm64 **kernel** (`virt` machine, virtio, overlayfs, cgroup v2).
3. Build a **rootfs** (e.g. Alpine + `docker`) whose init runs
   `dockerd -H tcp://0.0.0.0:2375 --storage-driver overlay2`.
4. Publish `vmlinuz` + `docker-rootfs-arm64.img.gz` as release assets and pin their
   SHA-256 into `vm/VmImages.kt`.

Until the QEMU jniLib is present, the VM screen reports clearly that the engine is
not bundled — the rest of the app (root mode, SSH mode) is unaffected.

## Performance & limitations

- **No `/dev/kvm` without root → software emulation (TCG).** Expect slow boots and
  modest container performance — fine for light testing, not production workloads.
- RAM/CPU for the guest are conservative (`VmImages.GUEST_MEM_MB` / `GUEST_CPUS`);
  raise them on capable devices.
- The guest daemon is exposed only on the host's loopback via QEMU's port forward; it
  is not reachable from the network.
- For a **fast** no-root VM, a Pixel 8/9 with the Android 15 Linux Terminal (AVF) can
  run Docker with hardware acceleration — but that path is device-specific and outside
  this bundled-QEMU backend.
