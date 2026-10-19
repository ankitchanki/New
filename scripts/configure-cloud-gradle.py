"""Configure workspace-local Java networking using the managed environment's proxy and CA store."""
import os
from pathlib import Path
from urllib.parse import urlparse

root = Path(os.environ.get("GRADLE_USER_HOME", "/workspace/.gradle"))
root.mkdir(parents=True, exist_ok=True)
path = root / "gradle.properties"
values = {}
proxy = urlparse(os.environ.get("HTTPS_PROXY", ""))
if proxy.hostname:
    for protocol in ("http", "https"):
        values[f"systemProp.{protocol}.proxyHost"] = proxy.hostname
        values[f"systemProp.{protocol}.proxyPort"] = str(proxy.port or 8080)
    values["systemProp.http.nonProxyHosts"] = "localhost|127.*"
trust = Path("/etc/ssl/certs/java/cacerts")
if trust.exists():
    values["systemProp.javax.net.ssl.trustStore"] = str(trust)
values["systemProp.org.gradle.internal.http.connectionTimeout"] = "30000"
values["systemProp.org.gradle.internal.http.socketTimeout"] = "30000"
lines = path.read_text().splitlines() if path.exists() else []
# Preserve unrelated settings. Store only the proxy hostname/port, never credentials.
lines = [line for line in lines if line.split("=", 1)[0] not in values]
lines += [f"{key}={value}" for key, value in values.items()]
path.write_text("\n".join(lines) + "\n")
