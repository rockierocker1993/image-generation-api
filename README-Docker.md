# Docker setup notes

To ensure files created by the container in `./output-dir` are owned by your host user (so you can download them via FileZilla/SFTP), run the container process under your host UID/GID.

1) Create a `.env` file at the repository root with your UID/GID (Linux/macOS):

```
LOCAL_UID=1000
LOCAL_GID=1000
```

You can find your UID/GID on Linux with:

```
id -u
id -g
```

On Windows, set appropriate numeric UID/GID mapping or let the defaults (1000) be used. Note that Windows file sharing behaves differently; if you still have permission issues use the `icacls` commands mentioned in the project docs.

2) Start the service with Docker Compose

```
docker compose up --build
```

3) Verify files created in `./output-dir` are owned by your host user and accessible via FileZilla.

Notes:
- The container image also creates `/app/output-dir` and chowns it to `appuser` at build time. When you use a bind-mount, host permissions take precedence; running the container with `user: ${LOCAL_UID}:${LOCAL_GID}` ensures created files match the host ownership.
- If you prefer to avoid bind-mount permission issues, use a named Docker volume. Files in a named volume are not directly visible on the host filesystem without additional steps.

