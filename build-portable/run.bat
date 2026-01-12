@echo off
setlocal EnableExtensions EnableDelayedExpansion

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

rem Validate environment
if not exist "%KARNAK_BIN%" (echo ERROR: Karnak executable not found & exit /b 1)

rem Start Karnak
echo [run.bat] Starting Karnak from '%KARNAK_BIN%'
start "Karnak" "%KARNAK_BIN%"

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
