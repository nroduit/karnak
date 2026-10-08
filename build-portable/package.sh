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

# jlink de-duplicates the runtime legal files into symlinks
materialize_symlinks ( ) {
  local root="$1"
  local link target count=0

  while IFS= read -r -d '' link; do
    target=$(readlink -f "$link") && [ -e "$target" ] \
      || die "Dangling symlink in the app image: $link"
    rm -f "$link"
    cp -pR "$target" "$link" || die "Cannot materialize the symlink $link"
    count=$((count + 1))
  done < <(find "$root" -type l -print0)

  echo "Materialized $count symlink(s) in $root"
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
  # The natives of every platform can be present, so pick the host one. Matching on the
  # candidates instead would return whichever directory `find` happened to list first.
  case "$(uname -s)" in
    Darwin)                 host_os="macosx";;
    Linux)                  host_os="linux";;
    CYGWIN*|MINGW*|MSYS*)   host_os="windows";;
    *)                      host_os="";;
  esac
  case "$(uname -m)" in
    arm64|aarch64)          host_arc="aarch64";;
    x86_64|amd64)           host_arc="x86-64";;
    *)                      host_arc="";;
  esac

  ARC_OS=""
  # Exact os-arch first, then any build for the host os.
  for want in "$host_os-$host_arc" "$host_os"; do
    [ -n "$host_os" ] || break
    for cand in "${_subdirs[@]}"; do
      case "$cand" in
        "$want"|"$want"-*) ARC_OS="$cand"; break 2;;
      esac
    done
  done

  if [ -z "$ARC_OS" ] ; then
    die "No natives for $host_os-$host_arc in $lib_dir (found: ${_subdirs[*]})."
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

if [ ! -d "$JDK_PATH_UNIX/jmods" ]; then
  die "The JDK at '$JDK_PATH_UNIX' has no 'jmods' directory (packaged modules) required by jpackage/jlink.\nUse a full JDK >= ${REQUIRED_TEXT_VERSION} that ships jmods. Temurin 24+ enables JEP 493 and\ndistributes the jmods separately: install the 'jdk+jmods' package, or download the jmods\narchive from the Adoptium API and extract it as '$JDK_PATH_UNIX/jmods'."
fi

$JPKGCMD --type app-image --input "$INPUT_DIR" --dest "$OUTPUT_PATH" --name "$NAME" \
--main-jar karnak-"${KARNAK_VERSION}".jar --main-class org.springframework.boot.loader.launch.JarLauncher \
--module-path "$JDK_PATH_UNIX/jmods" --add-modules ALL-MODULE-PATH \
--resource-dir "$RES" --app-version "$KARNAK_CLEAN_VERSION" \
"${tmpArgs[@]}" --verbose "${signArgs[@]}" "${commonOptions[@]}" "${consoleArgs[@]}" \
  || die "jpackage failed to build the app image. See the output above."

# MacOS code signing
if [ "$machine" = "macosx" ] ; then
    APP_BUNDLE="$OUTPUT_PATH/$NAME.app"

    # Must run before any signing: it changes the files that the seal covers. jpackage already
    # signed the image (ad hoc when no identity was given), so the bundle is re-signed below in
    # both branches.
    materialize_symlinks "$APP_BUNDLE"

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

      # Process JAR files containing native libraries
      APP_DIR="$APP_BUNDLE/Contents/app"
      MAIN_JAR="$APP_DIR/karnak-${KARNAK_VERSION}.jar"

      if [ -f "$MAIN_JAR" ]; then
        echo "Processing Spring Boot JAR: $MAIN_JAR"
        MAIN_JAR_ABS="$(cd "$(dirname "$MAIN_JAR")" && pwd)/$(basename "$MAIN_JAR")"
        WORK_DIR=$(mktemp -d)

        # The jar is edited in place with zip rather than extracted and rebuilt. `jar cf`
        # regenerates the manifest, which drops Main-Class, Start-Class and the Spring-Boot-*
        # entries, and it deflates the nested jars that the Spring Boot loader requires to be
        # stored. zip rewrites only the entries it is handed and copies the rest verbatim.
        nested_jars=$(unzip -Z1 "$MAIN_JAR_ABS" 'BOOT-INF/lib/*.jar' 2>/dev/null \
          | grep -E '(-native-|/jna-)' || true)

        for entry in $nested_jars ; do
          # Pull the nested jar out keeping its BOOT-INF/lib/... path, so it can be zipped back
          # under the same name.
          rm -rf "$WORK_DIR/outer" "$WORK_DIR/inner"
          mkdir -p "$WORK_DIR/outer" "$WORK_DIR/inner"
          unzip -q -o "$MAIN_JAR_ABS" "$entry" -d "$WORK_DIR/outer" \
            || die "Cannot extract $entry from $MAIN_JAR_ABS"

          libs=$(unzip -Z1 "$WORK_DIR/outer/$entry" '*.dylib' '*.jnilib' 2>/dev/null || true)
          [ -n "$libs" ] || continue
          echo "Processing nested JAR: $entry"

          # Only the Mach-O files are unpacked, signed and put back, so the nested jar keeps its
          # own manifest and every other entry untouched.
          ( cd "$WORK_DIR/inner" && unzip -q -o "$WORK_DIR/outer/$entry" '*.dylib' '*.jnilib' ) \
            || die "Cannot extract the native libraries of $entry"

          while IFS= read -r lib; do
            echo "Signing native library in nested JAR: $lib"
            codesign --force --options runtime --timestamp \
              --sign "$SIGN_ID" "$WORK_DIR/inner/$lib" \
              || die "Cannot sign $lib in $entry"
          done <<< "$libs"

          # shellcheck disable=SC2086
          ( cd "$WORK_DIR/inner" && zip -q "$WORK_DIR/outer/$entry" $libs ) \
            || die "Cannot update the native libraries of $entry"
          # -0 keeps the nested jar stored, as the Spring Boot loader requires.
          ( cd "$WORK_DIR/outer" && zip -q -X -0 "$MAIN_JAR_ABS" "$entry" ) \
            || die "Cannot put $entry back into $MAIN_JAR_ABS"

          echo "Repackaged nested JAR: $entry"
        done

        # Drop the natives (already installed next to the app image) and the license checker.
        # zip exits 12 when a pattern matches nothing, which is not an error here.
        for pattern in 'BOOT-INF/classes/lib/*' 'BOOT-INF/lib/license-checker-*.jar' ; do
          zip -q -d "$MAIN_JAR_ABS" "$pattern" || [ $? -eq 12 ] \
            || die "Cannot remove $pattern from $MAIN_JAR_ABS"
        done

        rm -rf "$WORK_DIR"

        # The launcher only reports a broken jar at runtime, so fail the build here instead.
        unzip -p "$MAIN_JAR_ABS" META-INF/MANIFEST.MF | grep -q '^Start-Class:' \
          || die "The repackaged jar lost its Start-Class manifest entry."
        if unzip -v "$MAIN_JAR_ABS" | awk '$NF ~ /^BOOT-INF\/lib\/.*\.jar$/ {print $2}' \
             | grep -qv '^Stored$' ; then
          die "The repackaged jar has deflated nested jars; the Spring Boot loader needs them stored."
        fi

        echo "Repackaged and signed: $MAIN_JAR_ABS"
      fi


      # Sign the entire app bundle
      echo "Signing app bundle: $APP_BUNDLE"
      codesign --deep --force --options runtime --timestamp \
        --entitlements "$RES/uri-launcher.entitlements" \
        --sign "$SIGN_ID" "$APP_BUNDLE"
    else
      # jpackage signs the app image ad hoc on Apple Silicon, so the seal has to be rebuilt
      # after the symlinks were materialized, even without a Developer ID.
      echo "No signing identity given, re-signing $APP_BUNDLE ad hoc"
      codesign --deep --force --sign - "$APP_BUNDLE"
    fi

    # Fatal: a broken seal makes Gatekeeper kill the app on first launch, and the failure only
    # shows up on the machine that downloads it. Notarization runs later, so spctl (which also
    # checks for a ticket) stays informational here.
    echo "Verifying signature..."
    codesign --verify --deep --strict --verbose=2 "$APP_BUNDLE" \
      || die "The app bundle seal is invalid. See the codesign output above."
    spctl --assess --verbose=4 --type execute "$APP_BUNDLE" || true
fi

cp "$curPath/run.cfg" "$OUTPUT_PATH/"
if [ "$machine" = "windows" ] ; then
  cp "$curPath/run.bat" "$OUTPUT_PATH/"
else
  cp "$curPath/run.sh" "$OUTPUT_PATH/"
  chmod +x "$OUTPUT_PATH"/run.sh
fi
