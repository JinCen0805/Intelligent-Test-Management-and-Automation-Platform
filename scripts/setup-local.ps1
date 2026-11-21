$ErrorActionPreference = 'Stop'

$configPath = Join-Path $PSScriptRoot 'local.env.ps1'

if (Test-Path -LiteralPath $configPath) {
    Write-Host "Local configuration already exists: $configPath"
    Write-Host 'Edit that file if you need to change the values.'
    exit 0
}

$dbUsername = Read-Host 'MySQL username [root]'
if ([string]::IsNullOrWhiteSpace($dbUsername)) {
    $dbUsername = 'root'
}

$dbPassword = Read-Host 'MySQL password'
if ([string]::IsNullOrWhiteSpace($dbPassword)) {
    throw 'MySQL password cannot be empty.'
}

$encryptionKey = Read-Host 'ITMAP encryption key [generate local key]'
if ([string]::IsNullOrWhiteSpace($encryptionKey)) {
    $encryptionKey = 'local-' + ([guid]::NewGuid().ToString('N'))
}

$content = @"
# Local-only settings. This file is ignored by Git.
`$env:DB_USERNAME = "$($dbUsername.Replace('"', '``"'))"
`$env:DB_PASSWORD = "$($dbPassword.Replace('"', '``"'))"
`$env:TESTPILOT_ENCRYPTION_KEY = "$($encryptionKey.Replace('"', '``"'))"
"@

[IO.File]::WriteAllText($configPath, $content, [Text.UTF8Encoding]::new($false))
Write-Host "Created local configuration: $configPath"
Write-Host 'You can now run .\scripts\start-local.ps1 or double-click start-local.cmd.'
