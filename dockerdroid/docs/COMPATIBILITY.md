# Kernel Compatibility & the Podman Fallback

> The hardest part of "Docker on Android" is not the app — it's getting Docker to
> actually run across the enormous variety of Android kernels.

Stock Android kernels are built for phones, not containers. Vendors frequently
disable or omit the features `dockerd` depends on. DockerDroid treats this as a
first-class concern: it probes the kernel **before** installing and chooses a runtime
accordingly, instead of letting the daemon crash with an opaque error.

## What gets probed

`CompatibilityChecker` runs these checks (each → PASS / WARN / FAIL):

| Check       | What it verifies                          | Failure impact                  |
|-------------|-------------------------------------------|---------------------------------|
| root        | A Magisk root shell is grantable          | **Blocking** — nothing works    |
| arch        | `uname -m` is aarch64/arm64               | **Blocking** — wrong binaries   |
| storage     | ≥ 2 GiB free on `/data`                   | Warn — images may not fit       |
| cgroups     | cgroup v2 unified, else v1                | Warn — partial resource limits  |
| namespaces  | pid/net/mnt/uts/ipc present               | **Blocking** — no isolation     |
| overlayfs   | `overlay` in `/proc/filesystems`          | Warn — fall back to `vfs`       |
| netfilter   | `iptables`/`nft` available                | Warn — bridge networking off    |
| selinux     | Enforcing vs permissive                   | Warn — labels not applied       |

## Runtime decision

```
root FAIL  ∨ arch FAIL ∨ namespaces FAIL   → UNSUPPORTED
overlayfs missing ∧ netfilter missing      → PODMAN  (rootless, vfs, host net)
otherwise                                  → DOCKER  (overlay2, bridge:none)
```

- **DOCKER** — full Docker Engine. `daemon.json` uses `overlay2`, disables the
  built-in bridge (`bridge: none`, `iptables: false`) because Android's netfilter is
  unreliable; containers use host or user-defined networking.
- **PODMAN** — when overlayfs and netfilter are both missing, a hardened kernel runs
  far more reliably under rootless Podman/containerd with the `vfs` storage driver
  and host networking. The same Compose/templates path drives it.
- **UNSUPPORTED** — the device fundamentally cannot run containers; the UI explains
  why and blocks install.

## Practical notes

- **overlayfs on `/data`**: even when the kernel supports overlay, SELinux or the
  f2fs/ext4 mount options can reject it. We detect at install and degrade to `vfs`.
- **cgroup delegation**: cgroup v2 with the controllers file is strongly preferred;
  without delegated controllers, `--memory`/`--cpus` limits are advisory.
- **MOUNT_MASTER**: the root shell runs in Magisk's global mount namespace so the
  daemon's bind mounts and overlay mounts are visible system-wide.
- **Hardened kernels** (e.g. some Samsung/Knox, GrapheneOS-like setups) may block
  `clone(CLONE_NEWNET)` or seccomp profiles; these surface as namespace FAIL or
  runtime errors captured in `dockerd.log`.
