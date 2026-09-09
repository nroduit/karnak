#!/usr/bin/env bash
# Starts Karnak portable with the settings of run.cfg.
# Usage: ./run.sh [OPTIONS]
#   --config <file>          Config file to source (default: ./run.cfg)
#   --help                   Show this help message
#
# On the first launch, the optional OCR service used by the automatic pixel
# de-identification is proposed for download (see OCR_* in run.cfg). Once Karnak
# is ready, the web portal opens in the default browser (KARNAK_OPEN_BROWSER).

set -euo pipefail

log() { echo "[run.sh] $*"; }
die() { echo "ERROR: $*" >&2; exit 1; }

# GitHub repository publishing the prebuilt OCR service
OCR_REPO="nroduit/image-ocr-identifier"

generate_db_password() {
  local pwd_file="$APP_DIR/.db_pwd"

  if [[ ! -f "$pwd_file" ]]; then
    log "Generating database password..."
    # Write to file with user-only permissions
    openssl rand -base64 32 | tr -d "=+/" | cut -c1-32 > "$pwd_file"
    chmod 600 "$pwd_file"
    log "Database password stored in '$pwd_file' (user-only access)"
  fi

  # Read and export the password
  DB_FILE_PWD=$(cat "$pwd_file")
  export DB_FILE_PWD
}

# Name of the prebuilt OCR archive for this machine, or nothing when none is published
ocr_asset() {
  case "$(uname -s):$(uname -m)" in
    Linux:x86_64) echo "linux-x86_64" ;;
    Darwin:arm64) echo "macos-arm64" ;;
    *) return 1 ;;
  esac
}

# Downloads the OCR service archive and extracts it next to this script
install_deidentify() {
  local url="$1" name="$2" tmp
  tmp="$(mktemp -d)"
  log "Downloading $url"
  if command -v curl > /dev/null 2>&1; then
    curl -fL --progress-bar -o "$tmp/ocr.tar.gz" "$url" || { log "The download failed. Karnak starts without the OCR service."; rm -rf "$tmp"; return 0; }
  elif command -v wget > /dev/null 2>&1; then
    wget -q --show-progress -O "$tmp/ocr.tar.gz" "$url" || { log "The download failed. Karnak starts without the OCR service."; rm -rf "$tmp"; return 0; }
  else
    log "Neither curl nor wget is available; download the archive manually from $url"
    rm -rf "$tmp"
    return 0
  fi
  if ! tar -xzf "$tmp/ocr.tar.gz" -C "$tmp"; then
    log "The archive could not be extracted. Karnak starts without the OCR service."
    rm -rf "$tmp"
    return 0
  fi
  # The archive holds a single folder named after the service
  local extracted="$tmp/image-ocr-identifier"
  [[ -d "$extracted" ]] || extracted="$(find "$tmp" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
  if [[ -z "$extracted" || ! -d "$extracted" ]]; then
    log "Unexpected archive layout. Karnak starts without the OCR service."
    rm -rf "$tmp"
    return 0
  fi
  rm -rf "$APP_DIR/$name"
  mv "$extracted" "$APP_DIR/$name"
  chmod +x "$APP_DIR/$name/$name" 2> /dev/null || true
  rm -rf "$tmp"
  log "OCR service installed in '$APP_DIR/$name'"
}

# Proposes to download the OCR service when it is enabled but not installed
ensure_deidentify() {
  [[ "${OCR_ENABLED:-true}" == "true" ]] || return 0
  local name="${OCR_SERVICE_NAME:-image-ocr-identifier}"
  [[ -x "$APP_DIR/$name/$name" ]] && return 0

  local mode="${OCR_AUTO_INSTALL:-ask}"
  if [[ "$mode" == "never" ]]; then
    log "OCR service not installed (OCR_AUTO_INSTALL=never)"
    return 0
  fi
  local asset
  if ! asset="$(ocr_asset)"; then
    log "No prebuilt OCR service is published for $(uname -s) $(uname -m); the automatic pixel de-identification is not available"
    return 0
  fi
  local version="${OCR_VERSION:-v0.1.0}" model="${OCR_MODEL:-PP-OCRv5_mobile}"
  local url="https://github.com/${OCR_REPO}/releases/download/${version}/image-ocr-identifier-${model}-${asset}.tar.gz"

  if [[ "$mode" == "ask" ]]; then
    if [[ ! -t 0 ]]; then
      log "OCR service not installed; run this script in a terminal to be asked, or set OCR_AUTO_INSTALL=always in run.cfg"
      return 0
    fi
    echo
    echo "The optional service that detects and hides text burned into the images (OCR)"
    echo "is not installed. It is only needed for the automatic pixel de-identification"
    echo "and is downloaded from:"
    echo "  $url"
    echo "You can also answer 'n' and install it later from run.cfg (OCR_AUTO_INSTALL)."
    echo
    local answer
    read -r -p "Download and install it now? [Y/n] " answer
    case "${answer:-Y}" in
      y | Y | yes | YES) ;;
      *)
        log "OCR service skipped. Set OCR_ENABLED=false in run.cfg to stop being asked."
        return 0
        ;;
    esac
  fi
  install_deidentify "$url" "$name"
}

start_deidentify() {
  if [[ "${OCR_ENABLED:-true}" != "true" ]]; then
    log "De-identification image service disabled (OCR_ENABLED)"
    return
  fi
  local name="${OCR_SERVICE_NAME:-image-ocr-identifier}"
  local bin="$APP_DIR/$name/$name"
  if [[ ! -x "$bin" ]]; then
    log "De-identification binary not found at '$bin' — skipping"
    return
  fi
  log "Starting de-identification image service..."
  "$bin" &
  DEIDENT_PID=$!
}

# Opens the web portal in the default browser once Karnak answers on its port
open_browser_when_ready() {
  [[ "${KARNAK_OPEN_BROWSER:-true}" == "true" ]] || return 0
  local url="http://localhost:${KARNAK_WEB_PORT:-8081}"
  local opener=""
  if [[ "$(uname -s)" == "Darwin" ]]; then
    opener="open"
  elif command -v xdg-open > /dev/null 2>&1; then
    opener="xdg-open"
  fi
  [[ -n "$opener" ]] || return 0
  (
    for _ in $(seq 1 120); do
      if curl -fs -o /dev/null "$url" 2> /dev/null || wget -q -O /dev/null "$url" 2> /dev/null; then
        "$opener" "$url" > /dev/null 2>&1 || true
        exit 0
      fi
      sleep 1
    done
  ) &
  BROWSER_PID=$!
}

# Default values
CONFIG_FILE="./run.cfg"
# Seconds to wait for a graceful shutdown before sending SIGKILL
SHUTDOWN_TIMEOUT="${KARNAK_SHUTDOWN_TIMEOUT:-30}"

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --config)
      CONFIG_FILE="$2"
      shift 2
      ;;
    --help)
      grep "^#" "$0" | grep -E "Usage:|--" | sed 's/^# *//'
      exit 0
      ;;
    *)
      die "Unknown option: $1. Use --help for usage information."
      ;;
  esac
done

# Set the application directory
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$APP_DIR"
APP_BIN="$APP_DIR/Karnak/bin"

generate_db_password

# Source configuration file and export all variables
if [[ -f "$CONFIG_FILE" ]]; then
  log "Loading configuration from '$CONFIG_FILE'"
  # Read each line, export non-commented variables
  while IFS= read -r line || [[ -n "$line" ]]; do
    # Skip empty lines and comments
    if [[ -z "$line" ]] || [[ "$line" =~ ^[[:space:]]*# ]]; then
      continue
    fi
    # Export the variable if it's an assignment
    if [[ "$line" =~ ^[[:space:]]*([A-Z_][A-Z0-9_]*)= ]]; then
      export "${line?}"
    fi
  done < "$CONFIG_FILE"
else
  log "No configuration file found at '$CONFIG_FILE', using defaults"
fi

# Check if Karnak executable exists and set path based on OS
if [[ "$(uname)" == "Darwin" ]]; then
  # macOS
  KARNAK_BIN="$APP_DIR/Karnak.app/Contents/MacOS/Karnak"
else
  # Linux
  KARNAK_BIN="$APP_BIN/Karnak"
fi
[[ ! -e "$KARNAK_BIN" ]] && die "Karnak executable not found at '$KARNAK_BIN'"

# Install (on request) and start the de-identification sidecar now that APP_DIR and the config are loaded
ensure_deidentify
start_deidentify

# Cleanup function
SHUTTING_DOWN=0
cleanup() {
  # Guard against running twice (e.g. INT trap then EXIT trap)
  [[ "$SHUTTING_DOWN" == 1 ]] && return
  SHUTTING_DOWN=1
  trap - EXIT INT TERM

  # Stop the browser watcher if it is still waiting
  if [[ -n "${BROWSER_PID:-}" ]] && kill -0 "$BROWSER_PID" 2> /dev/null; then
    kill "$BROWSER_PID" 2> /dev/null || true
    wait "$BROWSER_PID" 2> /dev/null || true
  fi

  # Stop the de-identification sidecar if running
  if [[ -n "${DEIDENT_PID:-}" ]] && kill -0 "$DEIDENT_PID" 2> /dev/null; then
    log "Stopping de-identification service (PID: $DEIDENT_PID)"
    kill -TERM "$DEIDENT_PID" 2> /dev/null || true
    wait "$DEIDENT_PID" 2> /dev/null || true
  fi

  # Stop Karnak if still running
  if [[ -n "${KARNAK_PID:-}" ]] && kill -0 "$KARNAK_PID" 2> /dev/null; then
    log "Stopping Karnak (PID: $KARNAK_PID), waiting up to ${SHUTDOWN_TIMEOUT}s for graceful shutdown..."
    kill -TERM "$KARNAK_PID" 2> /dev/null || true

    # Poll for graceful shutdown, then escalate to SIGKILL
    waited=0
    while kill -0 "$KARNAK_PID" 2> /dev/null; do
      if ((waited >= SHUTDOWN_TIMEOUT)); then
        log "Karnak did not stop within ${SHUTDOWN_TIMEOUT}s, sending SIGKILL"
        kill -KILL "$KARNAK_PID" 2> /dev/null || true
        break
      fi
      sleep 1
      waited=$((waited + 1))
    done

    # Reap the child so it does not linger as a zombie (<defunct>)
    wait "$KARNAK_PID" 2> /dev/null || true
  fi

  log "Cleanup complete"
}

trap cleanup EXIT INT TERM

# Run Karnak
log "Starting Karnak from '$KARNAK_BIN'"
"$KARNAK_BIN" &
KARNAK_PID=$!

echo
echo "Karnak is starting. The web portal is available at http://localhost:${KARNAK_WEB_PORT:-8081}"
echo "(default login: admin / karnak). Close this window or press Ctrl+C to stop Karnak."
echo
open_browser_when_ready

# Wait for Karnak to finish. A trapped signal interrupts wait and runs cleanup;
# `|| true` keeps `set -e` from aborting before cleanup can reap the child.
wait "$KARNAK_PID" || true
