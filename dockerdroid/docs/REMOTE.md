# Remote Docker over SSH

DockerDroid can manage a Docker Engine running on **another machine** (e.g. Docker
Desktop on a Mac) instead of on the phone. In this mode the phone is purely a
client — **no root and no on-device daemon are required**, so it works on locked
(unrootable) devices.

## How it works

The app opens an SSH session to the host and, for every Docker API call, runs
`docker system dial-stdio` over an SSH `exec` channel. That command pipes
stdin/stdout straight to the host's Docker socket — the exact mechanism the Docker
CLI uses for `DOCKER_HOST=ssh://…`. Consequences:

- Nothing is exposed on the network (no open TCP port, no `socat`).
- Traffic is encrypted and you authenticate with your normal SSH credentials.
- The host just needs the `docker` CLI in the SSH user's `PATH`.

```
Phone (DockerApiClient/OkHttp)
   └─ SSH exec: "docker system dial-stdio"   ──►  Mac sshd ──► docker.sock ──► dockerd (in Docker Desktop VM)
```

## Set up the Mac (host)

1. **Run Docker Desktop** (the daemon must be up).
2. **Enable SSH:** System Settings → General → Sharing → turn on **Remote Login**.
   Note the username and the Mac's LAN IP (System Settings → Wi-Fi → Details).
3. (Optional) Confirm `docker` is on the non-interactive PATH:
   `ssh you@mac 'docker version'`. If it fails with "command not found", set the
   full path in the app's host config, e.g. `/usr/local/bin/docker system dial-stdio`
   (Apple Silicon Homebrew: `/opt/homebrew/bin/docker …`).

## Connect from the phone

Welcome screen → **Connect to a remote host (SSH)** → enter:

- **Host** — the Mac's IP/hostname
- **Port** — `22`
- **Username** — your macOS account
- **Password** *or* **Private key (PEM)** (+ passphrase)

On success the app pings the daemon, saves the host (credentials encrypted via
`EncryptedSharedPreferences` / Android Keystore), and drops you on the Dashboard.
Containers, Images, Compose, templates, and monitoring all operate against the Mac.

## Security notes

- Credentials are stored encrypted (AES-256, key in the Android Keystore).
- **Host-key verification is not yet enforced** (`StrictHostKeyChecking=no`). Traffic
  is encrypted and the *server* authenticates *you*, but the app does not yet pin the
  server's key, so use this on trusted networks until trust-on-first-use lands.
- Prefer **key-based** auth over passwords.

## Switching back

Settings → switch to the on-device daemon, or **Forget remote host** to erase the
stored credentials.
