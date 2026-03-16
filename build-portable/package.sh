#!/bin/bash
# This script makes the multiplatform build of Karnak with jpackage
#
# Initial script by Nicolas Roduit

# Build Parameters
REVISON_INC="1"

# Options
NAME="Karnak"
IDENTIFIER="org.karnak.launcher"

# Aux functions:
die ( ) {
  echo
  echo -e "ERROR: $*"
  exit 1
}

POSITIONAL=()
while [[ $# -gt 0 ]]
do
  key="$1"

  case $key in
    -h|--help)
echo "Usage: package.sh <options>"
echo "Sample usages:"
echo "    Build an installer for the current platform with the minimal required parameters"
echo "        package.sh --jdk /home/user/jdk-21"
echo ""
echo "Options:"
echo " --help -h
Print the usage text with a list and description of each valid
option the output stream, and exit"
echo " --input -i
Path of the karnak-native directory"
echo " --output -o
Path of the base output directory.
Default value is the current directory"
echo " --jdk -j
Path of the jdk with the jpackage module"
echo " --temp
Path of the temporary directory during build"
echo " --mac-signing-key-user-name
Key user name of the certificate to sign the bundle"
exit 0
;;
-j|--jdk)
JDK_PATH_UNIX="$2"
shift # past argument
shift # past value
;;
-i|--input)
INPUT_PATH="$2"
shift # past argument
shift # past value
;;
-o|--output)
OUTPUT_PATH="$2"
shift # past argument
shift # past value
;;
--temp)
TEMP_PATH="$2"
shift # past argument
shift # past value
;;
--mac-signing-key-user-name)
CERTIFICATE="$2"
shift # past argument
shift # past value
;;
*)    # unknown option
POSITIONAL+=("$1") # save it in an array for later
shift # past argument
;;
esac
done
set -- "${POSITIONAL[@]}" # restore positional parameters


curPath=$(dirname "$(readlink -f "$0")")
rootdir="$(dirname "$curPath")"

echo "rootdir: $rootdir"

if [ ! -d "${INPUT_PATH}" ] ; then
  INPUT_PATH="${rootdir}/target"
fi

if [ ! -d "${INPUT_PATH}" ] ; then
  die "The input path ${INPUT_PATH} doesn't exist, provide a valid value for --input"
fi

# Detect ARC_OS from the unique folder in `INPUT_PATH/classes/lib`
lib_dir="$INPUT_PATH/classes/lib"
if [ ! -d "$lib_dir" ]; then
  die "The library path $lib_dir doesn't exist, provide a valid value for --input"
fi

# Collect immediate subdirectories (folder names only)
_subdirs=()
while IFS= read -r -d '' dir; do
  _subdirs+=("$(basename "$dir")")
done < <(find "$lib_dir" -mindepth 1 -maxdepth 1 -type d -print0 2>/dev/null)

if [ ${#_subdirs[@]} -eq 1 ]; then
  ARC_OS="${_subdirs[0]}"
else
  ARC_OS=""
  for cand in "${_subdirs[@]}"; do
    case "$cand" in
      *windows*) ARC_OS="$cand"; break;;
      *macosx*) ARC_OS="$cand"; break;;
      *linux*) ARC_OS="$cand"; break;;
    esac
  done
  # fallback to first if nothing matched
  if [ -z "$ARC_OS" ] && [ ${#_subdirs[@]} -gt 0 ]; then
    ARC_OS="${_subdirs[0]}"
  fi
fi

if [ -z "$ARC_OS" ] ; then
  die "Cannot get Java system architecture from $lib_dir"
fi
machine=$(echo "${ARC_OS}" | cut -d'-' -f1)
arc=$(echo "${ARC_OS}" | cut -d'-' -f2-3)

echo "Platform: $machine"

if [ "$machine" = "windows" ] ; then
  INPUT_PATH_UNIX=$(cygpath -u "$INPUT_PATH")
  RES="${curPath}\resources\\${machine}"
else
  INPUT_PATH_UNIX="$INPUT_PATH"
  RES="${curPath}/resources/$machine"
fi

# Set custom JDK path (>= JDK 11)
export JAVA_HOME=$JDK_PATH_UNIX

echo "System: ${ARC_OS}"
echo "JDK path: ${JDK_PATH_UNIX}"
echo "Karnak version: ${KARNAK_VERSION}"
echo "Input path: ${INPUT_PATH}"
if [ "$machine" = "windows" ]
then
  echo "Input unix path: ${INPUT_PATH_UNIX}"
fi

# Specify the required Java version.
# Only major version is checked. Minor version or any other version string info is left out.
REQUIRED_TEXT_VERSION="${JAVA_VERSION}"
# Extract major version number for comparisons from the required version string.
# In order to do that, remove leading "1." if exists, and minor and security versions.
REQUIRED_MAJOR_VERSION=$(echo "$REQUIRED_TEXT_VERSION" | sed -e 's/^1\.//' -e 's/\..*//')

# Check jlink command.
if [ -x "$JDK_PATH_UNIX/bin/jpackage" ] ; then
  JPKGCMD="$JDK_PATH_UNIX/bin/jpackage"
  JAVACMD="$JDK_PATH_UNIX/bin/java"
else
  die "JAVA_HOME is not set and no 'jpackage' command could be found in your PATH. Specify a jdk path >=$REQUIRED_TEXT_VERSION."
fi

# Then, get the installed version
INSTALLED_VERSION=$($JAVACMD -version 2>&1 | awk '/version [0-9]*/ {print $3;}')
echo "Found java version $INSTALLED_VERSION"
echo "Java command path: $JAVACMD"

# Remove double quotes, remove leading "1." if it exists and remove everything apart from the major version number.
INSTALLED_MAJOR_VERSION=$(echo "$INSTALLED_VERSION" | sed -e 's/"//g' -e 's/^1\.//' -e 's/\..*//' -e 's/-.*//')
echo "Java major version: $INSTALLED_MAJOR_VERSION"
if (( INSTALLED_MAJOR_VERSION < REQUIRED_MAJOR_VERSION )) ; then
  die "Your version of java is too low to run this script.\nPlease update to $REQUIRED_TEXT_VERSION or higher"
fi

if [ -z "$OUTPUT_PATH" ] ; then
  APP_PACKAGE_FOLDER="karnak-$ARC_OS-jdk$INSTALLED_MAJOR_VERSION-$KARNAK_VERSION"
  OUTPUT_PATH="target/$APP_PACKAGE_FOLDER"
  if [ -n "${GITHUB_ENV:-}" ] && [ -f "$GITHUB_ENV" ]; then
      echo "APP_PACKAGE_FOLDER=$APP_PACKAGE_FOLDER" >> "$GITHUB_ENV"
  fi
fi


if [ "$machine" = "windows" ] ; then
  INPUT_DIR="$INPUT_PATH\portable"
else
  INPUT_DIR="$INPUT_PATH_UNIX/portable"
fi

KARNAK_CLEAN_VERSION=$(echo "$KARNAK_VERSION" | sed -e 's/"//g' -e 's/-.*//' -e 's/\(\([0-9]\+\.\)\{2\}[0-9]\+\)\.[0-9]\+/\1/')


# Remove previous package
if [ -d "${OUTPUT_PATH}" ] ; then
  rm -rf "${OUTPUT_PATH}"
fi

if [ -z "$TEMP_PATH" ] ; then
  declare -a tmpArgs=()
else
  declare -a tmpArgs=("--temp" "$TEMP_PATH")
fi

if [ -d "${TEMP_PATH}" ] ; then
  rm -rf "${TEMP_PATH}"
fi

# Code signing options
if [ "$machine" = "macosx" ] ; then
  if [[ -n "$MAC_DEVELOPER_ID" ]] ; then
   CERTIFICATE="$MAC_DEVELOPER_ID"
  fi

  if [[ -n "$CERTIFICATE" ]] ; then
    declare -a signArgs=("--mac-package-identifier" "$IDENTIFIER" "--mac-signing-key-user-name" "$CERTIFICATE"  "--mac-sign")
  else
    declare -a signArgs=("--mac-package-identifier" "$IDENTIFIER")
  fi
  echo "Sign args: ${signArgs[*]}"
else
  declare -a signArgs=()
fi

# Common options
declare -a commonOptions=(
"--java-options" "-Dspring.profiles.active=portable" \
"--java-options" "-Djava.library.path=\$APPDIR/dicom-opencv" \
"--java-options" "--enable-native-access=ALL-UNNAMED");

mkdir -p "$INPUT_PATH_UNIX/portable/dicom-opencv"
cp "$INPUT_PATH_UNIX/karnak-${KARNAK_VERSION}".jar "$INPUT_PATH_UNIX/portable/karnak-${KARNAK_VERSION}.jar"
cp -r "$INPUT_PATH_UNIX/classes/lib/${ARC_OS}"/* "$INPUT_PATH_UNIX/portable/dicom-opencv/"

if [ "$machine" = "windows" ] ; then
  declare -a consoleArgs=("--win-console")
else
  declare -a consoleArgs=()
fi

$JPKGCMD --type app-image --input "$INPUT_DIR" --dest "$OUTPUT_PATH" --name "$NAME" \
--main-jar karnak-"${KARNAK_VERSION}".jar --main-class org.springframework.boot.loader.launch.JarLauncher \
--module-path "$JDK_PATH_UNIX/jmods" --add-modules ALL-MODULE-PATH \
--resource-dir "$RES" --app-version "$KARNAK_CLEAN_VERSION" \
"${tmpArgs[@]}" --verbose "${signArgs[@]}" "${commonOptions[@]}" "${consoleArgs[@]}"

# MacOS code signing
if [ "$machine" = "macosx" ] ; then
    APP_BUNDLE="$OUTPUT_PATH/$NAME.app"

    if [[ -n "$CERTIFICATE" ]] ; then
      SIGN_ID="$CERTIFICATE"
    else
      SIGN_ID=""
    fi

    if [[ -n "$SIGN_ID" ]] ; then
      echo "Signing all binaries in $APP_BUNDLE with certificate: $SIGN_ID"

      # Sign all nested native libraries first (deepest to shallowest)
      find "$APP_BUNDLE" -type f \( -name "*.dylib" -o -name "*.jnilib" \) | while read -r lib; do
        echo "Signing: $lib"
        codesign --force --options runtime --timestamp \
          --sign "$SIGN_ID" "$lib" 2>&1 || echo "Warning: Could not sign $lib"
      done

      LAST_PWD=$(pwd)
      echo "Last PWD: $LAST_PWD"

      # Process JAR files containing native libraries
      APP_DIR="$APP_BUNDLE/Contents/app"
      MAIN_JAR="$APP_DIR/karnak-${KARNAK_VERSION}.jar"

      if [ -f "$MAIN_JAR" ]; then
        echo "Processing Spring Boot JAR: $MAIN_JAR"
        TEMP_JAR_DIR=$(mktemp -d)

        # Extract JAR
        MAIN_JAR_ABS="$(cd "$(dirname "$MAIN_JAR")" && pwd)/$(basename "$MAIN_JAR")"
        cd "$TEMP_JAR_DIR" && jar xf "$MAIN_JAR_ABS"

        # Find and process nested JARs containing native libraries
        find "$TEMP_JAR_DIR" -type f -name "*.jar" | grep -E '(.*-native-.*|jna-.*)' | while read -r nested_jar; do
          echo "Processing nested JAR: $nested_jar"

          NESTED_TEMP=$(mktemp -d)
          cd "$NESTED_TEMP" && jar xf "$nested_jar"

          # Sign native libraries in this nested JAR
          find "$NESTED_TEMP" -type f \( -name "*.dylib" -o -name "*.jnilib" \) | while read -r lib; do
            echo "Signing native library in nested JAR: $lib"
            codesign --force --options runtime --timestamp \
              --sign "$SIGN_ID" "$lib" 2>&1
          done

          # Repackage the nested JAR
          jar cf "$nested_jar" -C "$NESTED_TEMP" .
          rm -rf "$NESTED_TEMP"

          echo "Repackaged nested JAR: $nested_jar"
        done

        # Clean up extracted native libraries from the main JAR
        rm -rf "$TEMP_JAR_DIR/BOOT-INF/classes/lib"
        rm -rf "$TEMP_JAR_DIR/BOOT-INF/lib/license-checker-"*".jar"

        # Repackage JAR
        jar cf "$MAIN_JAR_ABS" -C "$TEMP_JAR_DIR" .
        rm -rf "$TEMP_JAR_DIR"

        echo "Repackaged and signed: $MAIN_JAR_ABS"
      fi

      cd "$LAST_PWD" || die "Cannot change directory to $curPath"


      # Sign the entire app bundle
      echo "Signing app bundle: $APP_BUNDLE"
      codesign --deep --force --options runtime --timestamp \
        --entitlements "$RES/uri-launcher.entitlements" \
        --sign "$SIGN_ID" "$APP_BUNDLE"

      # Verify
      echo "Verifying signature..."
      codesign --verify --deep --strict --verbose=2 "$APP_BUNDLE"
      spctl --assess --verbose=4 --type execute "$APP_BUNDLE"
    fi
fi

cp "$curPath/run.cfg" "$OUTPUT_PATH/"
if [ "$machine" = "windows" ] ; then
  cp "$curPath/run.bat" "$OUTPUT_PATH/"
else
  cp "$curPath/run.sh" "$OUTPUT_PATH/"
  chmod +x "$OUTPUT_PATH"/run.sh
fi
