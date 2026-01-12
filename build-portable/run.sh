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

# Default values
CONFIG_FILE="./run.cfg"

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

# Cleanup function
cleanup() {
  log "Shutting down services..."

  # Stop Karnak if running
  if [[ -n "${KARNAK_PID:-}" ]] && kill -0 "$KARNAK_PID" 2>/dev/null; then
    log "Stopping Karnak (PID: $KARNAK_PID)"
    kill "$KARNAK_PID" 2>/dev/null || true
    # Wait briefly for Karnak to stop
    sleep 1
  fi

  log "Cleanup complete"
}

trap cleanup EXIT INT TERM

# Run Karnak
log "Starting Karnak from '$KARNAK_BIN'"
"$KARNAK_BIN" &
KARNAK_PID=$!

# Wait for Karnak to finish
wait $KARNAK_PID
