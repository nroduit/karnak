#!/usr/bin/env bash
# Script to run Karnak with environment variables from run.cfg
# Usage: ./run.sh [OPTIONS]
#   --config <file>          Config file to source (default: ./run.cfg)
#   --help                   Show this help message

set -euo pipefail

log() { echo "[run.sh] $*"; }
die() { echo "ERROR: $*" >&2; exit 1; }

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

start_deidentify() {
  if [[ "${DEIDENTIFY_IMAGE_ENABLED:-true}" != "true" ]]; then
    log "De-identification image service disabled (DEIDENTIFY_IMAGE_ENABLED)"
    return
  fi
  local name="${DEIDENTIFY_IMAGE_SERVICE_NAME:-image-ocr-identifier}"
  local bin="$APP_DIR/$name/$name"
  if [[ ! -x "$bin" ]]; then
    log "De-identification binary not found at '$bin' — skipping"
    return
  fi
  log "Starting de-identification image service..."
  "$bin" &
  DEIDENT_PID=$!
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

# Start the de-identification sidecar now that APP_DIR and the config are loaded
start_deidentify

# Cleanup function
SHUTTING_DOWN=0
cleanup() {
  # Guard against running twice (e.g. INT trap then EXIT trap)
  [[ "$SHUTTING_DOWN" == 1 ]] && return
  SHUTTING_DOWN=1
  trap - EXIT INT TERM

  # Stop the de-identification sidecar if running
    if [[ -n "${DEIDENT_PID:-}" ]] && kill -0 "$DEIDENT_PID" 2>/dev/null; then
      log "Stopping de-identification service (PID: $DEIDENT_PID)"
      kill -TERM "$DEIDENT_PID" 2>/dev/null || true
      wait "$DEIDENT_PID" 2>/dev/null || true
    fi

  # Stop Karnak if still running
  if [[ -n "${KARNAK_PID:-}" ]] && kill -0 "$KARNAK_PID" 2>/dev/null; then
    log "Stopping Karnak (PID: $KARNAK_PID), waiting up to ${SHUTDOWN_TIMEOUT}s for graceful shutdown..."
    kill -TERM "$KARNAK_PID" 2>/dev/null || true

    # Poll for graceful shutdown, then escalate to SIGKILL
    waited=0
    while kill -0 "$KARNAK_PID" 2>/dev/null; do
      if (( waited >= SHUTDOWN_TIMEOUT )); then
        log "Karnak did not stop within ${SHUTDOWN_TIMEOUT}s, sending SIGKILL"
        kill -KILL "$KARNAK_PID" 2>/dev/null || true
        break
      fi
      sleep 1
      waited=$((waited + 1))
    done

    # Reap the child so it does not linger as a zombie (<defunct>)
    wait "$KARNAK_PID" 2>/dev/null || true
  fi

  log "Cleanup complete"
}

trap cleanup EXIT INT TERM

# Run Karnak
log "Starting Karnak from '$KARNAK_BIN'"
"$KARNAK_BIN" &
KARNAK_PID=$!

# Wait for Karnak to finish. A trapped signal interrupts wait and runs cleanup;
# `|| true` keeps `set -e` from aborting before cleanup can reap the child.
wait "$KARNAK_PID" || true
