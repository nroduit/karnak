@echo off
setlocal EnableExtensions EnableDelayedExpansion
rem Starts Karnak portable with the settings of run.cfg.
rem On the first launch, the optional OCR service used by the automatic pixel
rem de-identification is proposed for download (see OCR_* in run.cfg). Once Karnak
rem is ready, the web portal opens in the default browser (KARNAK_OPEN_BROWSER).

rem Defaults
set "CONFIG_FILE=run.cfg"

rem Parse args
:parse_args
if "%~1"=="" goto args_done
if /i "%~1"=="--config" (set "CONFIG_FILE=%~2" & shift & shift & goto parse_args)
if /i "%~1"=="--help" (call :show_help & exit /b 0)
echo ERROR: Unknown option: %~1. Use --help for usage information.
exit /b 1

:args_done

rem Setup paths
set "APP_DIR=%~dp0"
cd /d "%APP_DIR%"
set "APP_BIN=%APP_DIR%Karnak"
set "KARNAK_BIN=%APP_BIN%\Karnak.exe"

rem Generate or load database password
call :generate_db_password

rem Load configuration file
if exist "%CONFIG_FILE%" (
    echo [run.bat] Loading configuration from '%CONFIG_FILE%'
    for /f "usebackq tokens=* delims=" %%a in ("%CONFIG_FILE%") do (
        set "line=%%a"
        rem Skip empty lines and comments
        if defined line (
            echo !line! | findstr /r "^[A-Z_][A-Z0-9_]*=" >nul
            if !errorlevel! equ 0 (
                set "%%a"
            )
        )
    )
) else (
    echo [run.bat] No configuration file found at '%CONFIG_FILE%', using defaults
)
if not defined KARNAK_WEB_PORT set "KARNAK_WEB_PORT=8081"

rem Validate environment
if not exist "%KARNAK_BIN%" (echo ERROR: Karnak executable not found & pause & exit /b 1)

rem Install (on request) and start the de-identification image service (optional)
if not defined OCR_ENABLED set "OCR_ENABLED=true"
set "DEIDENT_NAME=%OCR_SERVICE_NAME%"
if not defined DEIDENT_NAME set "DEIDENT_NAME=image-ocr-identifier"
if /i "%OCR_ENABLED%"=="true" (
  call :ensure_deidentify
  if exist "%APP_DIR%!DEIDENT_NAME!\!DEIDENT_NAME!.exe" (
    echo [run.bat] Starting de-identification image service...
    start "Deidentify" /min "%APP_DIR%!DEIDENT_NAME!\!DEIDENT_NAME!.exe"
  )
)

rem Start Karnak
echo [run.bat] Starting Karnak from '%KARNAK_BIN%'
start "Karnak" "%KARNAK_BIN%"

echo.
echo Karnak is starting. The web portal is available at http://localhost:%KARNAK_WEB_PORT%
echo (default login: admin / karnak). Close the Karnak window to stop it.
echo.

rem Open the web portal in the default browser once Karnak answers on its port
if not defined KARNAK_OPEN_BROWSER set "KARNAK_OPEN_BROWSER=true"
if /i "%KARNAK_OPEN_BROWSER%"=="true" (
  start "" /min powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$url = 'http://localhost:%KARNAK_WEB_PORT%';" ^
    "for ($i = 0; $i -lt 120; $i++) {" ^
    "  try { Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 2 | Out-Null; Start-Process $url; break }" ^
    "  catch { Start-Sleep -Seconds 1 }" ^
    "}"
)

exit /b 0

:ensure_deidentify
if exist "%APP_DIR%%DEIDENT_NAME%\%DEIDENT_NAME%.exe" exit /b 0
if not defined OCR_AUTO_INSTALL set "OCR_AUTO_INSTALL=ask"
if /i "%OCR_AUTO_INSTALL%"=="never" (
  echo [run.bat] OCR service not installed ^(OCR_AUTO_INSTALL=never^)
  exit /b 0
)
if not defined OCR_VERSION set "OCR_VERSION=v0.1.0"
if not defined OCR_MODEL set "OCR_MODEL=PP-OCRv5_mobile"
set "OCR_DL_URL=https://github.com/nroduit/image-ocr-identifier/releases/download/%OCR_VERSION%/image-ocr-identifier-%OCR_MODEL%-windows-x86_64.zip"
if /i "%OCR_AUTO_INSTALL%"=="ask" (
  echo.
  echo The optional service that detects and hides text burned into the images ^(OCR^)
  echo is not installed. It is only needed for the automatic pixel de-identification
  echo and is downloaded from:
  echo   %OCR_DL_URL%
  echo You can also answer 'n' and install it later from run.cfg ^(OCR_AUTO_INSTALL^).
  echo.
  set "OCR_ANSWER="
  set /p "OCR_ANSWER=Download and install it now? [Y/n] "
  if not defined OCR_ANSWER set "OCR_ANSWER=Y"
  if /i not "!OCR_ANSWER!"=="y" if /i not "!OCR_ANSWER!"=="yes" (
    echo [run.bat] OCR service skipped. Set OCR_ENABLED=false in run.cfg to stop being asked.
    exit /b 0
  )
)
echo [run.bat] Downloading %OCR_DL_URL% ...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;" ^
  "$tmp = Join-Path $env:TEMP ('karnak-ocr-' + [guid]::NewGuid());" ^
  "New-Item -ItemType Directory -Path $tmp | Out-Null;" ^
  "Invoke-WebRequest -UseBasicParsing -Uri '%OCR_DL_URL%' -OutFile (Join-Path $tmp 'ocr.zip');" ^
  "Expand-Archive -Path (Join-Path $tmp 'ocr.zip') -DestinationPath $tmp -Force;" ^
  "$src = Join-Path $tmp 'image-ocr-identifier';" ^
  "if (-not (Test-Path $src)) { $src = (Get-ChildItem -Path $tmp -Directory | Select-Object -First 1).FullName };" ^
  "$dst = Join-Path '%APP_DIR%' '%DEIDENT_NAME%';" ^
  "if (Test-Path $dst) { Remove-Item -Recurse -Force $dst };" ^
  "Move-Item -Path $src -Destination $dst;" ^
  "Remove-Item -Recurse -Force $tmp;"
if errorlevel 1 (
  echo [run.bat] The download or the installation failed. Karnak starts without the OCR service.
  exit /b 0
)
echo [run.bat] OCR service installed in '%APP_DIR%%DEIDENT_NAME%'
exit /b 0

:generate_db_password
set "PWD_FILE=%APP_DIR%.db_pwd"
if not exist "%PWD_FILE%" (
    echo [run.bat] Generating database password...
    rem Generate random password using PowerShell
    powershell -NoProfile -Command ^
      "$path = '%PWD_FILE%';" ^
      "$bytes = New-Object byte[] 32;" ^
      "(New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes($bytes);" ^
      "$pwd = [Convert]::ToBase64String($bytes) -replace '[+/=]', '';" ^
      "$pwd = $pwd.Substring(0,32);" ^
      "Set-Content -Path $path -Value $pwd -NoNewline;" ^
      "$acl = New-Object System.Security.AccessControl.FileSecurity;" ^
      "$user = [System.Security.Principal.NTAccount]::new($env:UserDomain, $env:UserName);" ^
      "$rule = New-Object System.Security.AccessControl.FileSystemAccessRule($user,'FullControl','Allow');" ^
      "$acl.SetOwner($user);" ^
      "$acl.SetAccessRuleProtection($true,$false);" ^
      "$acl.SetAccessRule($rule);" ^
      "Set-Acl -Path $path -AclObject $acl;"

    echo [run.bat] Database password stored in '%PWD_FILE%' (user-only ACL set)
)
rem Read password from file
set /p DB_FILE_PWD=<"%PWD_FILE%"
exit /b 0

:show_help
echo Usage: run.bat [OPTIONS]
echo   --config ^<file^>        Config file to source (default: ./run.cfg)
echo   --help                 Show this help message
exit /b 0
