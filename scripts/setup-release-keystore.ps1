$ErrorActionPreference = "Stop"

$repository = "trvny/autka"
$displayName = "Autka"
$keyAlias = "autka"
$base64Secret = "KEYSTORE_BASE64"
$keystoreDirectory = Join-Path $env:USERPROFILE "AndroidKeystores"
$keystorePath = Join-Path $keystoreDirectory "autka-release.jks"
$base64Path = "$keystorePath.base64.txt"

function ConvertTo-PlainText {
    param([Security.SecureString]$SecureValue)

    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Read-ConfirmedPassword {
    param([string]$Label)

    while ($true) {
        $first = ConvertTo-PlainText (Read-Host $Label -AsSecureString)
        $second = ConvertTo-PlainText (Read-Host "Repeat $Label" -AsSecureString)

        if ($first.Length -lt 6) {
            Write-Warning "Password must contain at least 6 characters."
            continue
        }
        if ($first -ne $second) {
            Write-Warning "Passwords do not match."
            continue
        }
        return $first
    }
}

function Set-RepositorySecret {
    param(
        [string]$Name,
        [string]$Value,
        [string]$GhPath
    )

    & $GhPath secret set $Name --repo $repository --body $Value
    if ($LASTEXITCODE -ne 0) {
        throw "Uploading Actions secret $Name failed."
    }
}

$ghCommand = Get-Command gh.exe -ErrorAction SilentlyContinue
$ghPath = if ($ghCommand) { $ghCommand.Source } else { $null }
$ghAuthenticated = $false
if ($ghPath) {
    & $ghPath auth status --hostname github.com *> $null
    if ($LASTEXITCODE -eq 0) {
        $ghAuthenticated = $true
        $secretNames = @(& $ghPath secret list --repo $repository --json name --jq ".[].name")
        if ($LASTEXITCODE -ne 0) {
            throw "Could not inspect existing Actions secrets in $repository."
        }

        $signingSecrets = @($base64Secret, "KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
        $existingSigningSecrets = @($secretNames | Where-Object { $signingSecrets -contains $_ })
        if ($existingSigningSecrets.Count -gt 0) {
            throw "Release signing secrets already exist in $repository ($($existingSigningSecrets -join ', ')). Restore the existing keystore instead of generating a new certificate."
        }
    }
}

if (-not $ghAuthenticated) {
    Write-Warning "GitHub CLI is unavailable or not authenticated, so existing signing secrets cannot be checked."
    $confirmation = Read-Host "Type CREATE-FIRST-KEY only if this app has never had a release signing key"
    if ($confirmation -cne "CREATE-FIRST-KEY") {
        throw "Cancelled without creating a key."
    }
}

$keytoolCommand = Get-Command keytool.exe -ErrorAction SilentlyContinue
$keytoolPath = if ($keytoolCommand) { $keytoolCommand.Source } else { $null }
$candidates = @(
    $(if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\keytool.exe" }),
    $(if ($env:ProgramFiles) { Join-Path $env:ProgramFiles "Android\Android Studio\jbr\bin\keytool.exe" }),
    $(if ($env:LOCALAPPDATA) { Join-Path $env:LOCALAPPDATA "Programs\Android Studio\jbr\bin\keytool.exe" })
) | Where-Object { $_ }

if (-not $keytoolPath) {
    $keytoolPath = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if (-not $keytoolPath) {
    throw "keytool.exe was not found. Install Android Studio/JDK 17 or set JAVA_HOME."
}

New-Item -ItemType Directory -Force -Path $keystoreDirectory | Out-Null
if (Test-Path $keystorePath) {
    throw "Keystore already exists: $keystorePath`nThe script will not overwrite a release key."
}

$storePassword = Read-ConfirmedPassword "Keystore password"
$keyPassword = Read-ConfirmedPassword "Key password"

& $keytoolPath `
    -genkeypair `
    -v `
    -keystore $keystorePath `
    -storetype JKS `
    -alias $keyAlias `
    -keyalg RSA `
    -keysize 4096 `
    -validity 10000 `
    -storepass $storePassword `
    -keypass $keyPassword `
    -dname "CN=Autka, O=travny, C=PL"

if ($LASTEXITCODE -ne 0) {
    throw "keytool failed with exit code $LASTEXITCODE."
}

$base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystorePath))
[IO.File]::WriteAllText($base64Path, $base64, [Text.Encoding]::ASCII)

$uploaded = $false
if ($ghAuthenticated) {
    Set-RepositorySecret $base64Secret $base64 $ghPath
    Set-RepositorySecret "KEYSTORE_PASSWORD" $storePassword $ghPath
    Set-RepositorySecret "KEY_ALIAS" $keyAlias $ghPath
    Set-RepositorySecret "KEY_PASSWORD" $keyPassword $ghPath
    $uploaded = $true
}

Write-Host ""
Write-Host "$displayName release key created:" -ForegroundColor Green
Write-Host "  $keystorePath"
Write-Host "  Base64 copy: $base64Path"
Write-Host "  Alias: $keyAlias"
if ($uploaded) {
    Write-Host "GitHub Actions secrets were uploaded to $repository." -ForegroundColor Green
}
else {
    Write-Host "Add these repository Actions secrets manually:" -ForegroundColor Yellow
    Write-Host "  $base64Secret = contents of $base64Path"
    Write-Host "  KEYSTORE_PASSWORD = the keystore password"
    Write-Host "  KEY_ALIAS = $keyAlias"
    Write-Host "  KEY_PASSWORD = the key password"
}
Write-Host ""
Write-Host "Back up the .jks file and both passwords offline. Never replace this key for later releases." -ForegroundColor Yellow

$storePassword = $null
$keyPassword = $null
$base64 = $null
