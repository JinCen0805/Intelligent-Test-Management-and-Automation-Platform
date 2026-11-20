$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$configPath = Join-Path $PSScriptRoot 'local.env.ps1'
$frontend = Join-Path $root 'frontend'
$backend = Join-Path $root 'backend'
$redisFolderName = -join ([char]0x8F6F, [char]0x4EF6, [char]0x5B89, [char]0x88C5)
$redisHome = Join-Path 'E:\' (Join-Path $redisFolderName 'Redis-x64-5.0.14.1')
$redisServer = Join-Path $redisHome 'redis-server.exe'
$redisCli = Join-Path $redisHome 'redis-cli.exe'
$redisConfig = Join-Path $redisHome 'redis.windows.conf'
$logDir = Join-Path $root 'run-local-logs'
$redisPort = 6379
$frontendPort = 5173
$backendPort = 8080
$frontendUrl = "http://127.0.0.1:$frontendPort"
$backendUrl = "http://127.0.0.1:$backendPort"

if (-not (Test-Path -LiteralPath $configPath)) {
    $examplePath = Join-Path $PSScriptRoot 'local.env.example.ps1'
    throw "Local configuration not found: $configPath. Run .\scripts\setup-local.ps1 once, then start-local.cmd can be used for one-click startup. Example: $examplePath"
}

. $configPath

if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME)) {
    throw "DB_USERNAME is empty in $configPath."
}
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    throw "DB_PASSWORD is empty in $configPath."
}
if ([string]::IsNullOrWhiteSpace($env:TESTPILOT_ENCRYPTION_KEY)) {
    throw "TESTPILOT_ENCRYPTION_KEY is empty in $configPath."
}

function Test-HttpEndpoint([string]$url) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 3
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
    } catch {
        return $false
    }
}

function Wait-HttpEndpoint([string]$url, [string]$serviceName, [int]$timeoutSeconds = 120) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-HttpEndpoint $url) {
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "$serviceName startup timed out. Check logs in $logDir."
}

function Get-ListeningProcess([int]$port) {
    return Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique
}

function Test-RedisEndpoint {
    if (-not (Test-Path -LiteralPath $redisCli)) {
        return $false
    }
    try {
        $reply = (& $redisCli -h 127.0.0.1 -p $redisPort ping 2>$null | Out-String).Trim()
        return $LASTEXITCODE -eq 0 -and $reply -eq 'PONG'
    } catch {
        return $false
    }
}

function Wait-RedisEndpoint([int]$timeoutSeconds = 30) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-RedisEndpoint) {
            return
        }
        Start-Sleep -Seconds 1
    }
    throw "Redis startup timed out. Check logs in $logDir."
}

if (-not (Test-Path -LiteralPath (Join-Path $frontend 'package.json'))) {
    throw "Frontend project not found: $frontend"
}
if (-not (Test-Path -LiteralPath (Join-Path $backend 'pom.xml'))) {
    throw "Backend project not found: $backend"
}

if (-not (Get-Command npm.cmd -ErrorAction SilentlyContinue)) {
    throw 'npm.cmd not found. Please install Node.js and reopen PowerShell.'
}
if (-not (Get-Command mvn.cmd -ErrorAction SilentlyContinue)) {
    throw 'mvn.cmd not found. Please install Maven and reopen PowerShell.'
}
# Redis is optional for the local profile (application-local.yml: redis.enabled=false unless REDIS_ENABLED=true).
$redisRequired = "$env:REDIS_ENABLED".Trim().ToLowerInvariant() -eq 'true'
$redisFilesReady = (Test-Path -LiteralPath $redisServer) -and (Test-Path -LiteralPath $redisCli) -and (Test-Path -LiteralPath $redisConfig)
if ($redisRequired -and -not $redisFilesReady) {
    throw "Redis server/CLI/config not found: $redisHome"
}

New-Item -ItemType Directory -Path $logDir -Force | Out-Null

if (-not (Test-Path -LiteralPath (Join-Path $frontend 'node_modules'))) {
    Write-Host 'Installing frontend dependencies...'
    Push-Location $frontend
    try {
        & npm.cmd install
        if ($LASTEXITCODE -ne 0) {
            throw 'Frontend dependency installation failed.'
        }
    } finally {
        Pop-Location
    }
}

if (Test-RedisEndpoint) {
    Write-Host "Redis is already running: 127.0.0.1:$redisPort"
} elseif (Get-ListeningProcess $redisPort) {
    $owners = (Get-ListeningProcess $redisPort) -join ', '
    if ($redisRequired) {
        throw "Port $redisPort is already in use by process $owners, but the configured Redis is not responding."
    }
    Write-Host "WARN: port $redisPort is used by an external process ($owners); REDIS_ENABLED is not true, continuing without Redis."
} elseif (-not $redisRequired) {
    Write-Host 'Redis is not required for local profile (REDIS_ENABLED != true); skipping Redis startup.'
} elseif (-not $redisFilesReady) {
    Write-Host 'Redis files not found; skipping Redis startup (local profile does not require Redis).'
} else {
    Write-Host 'Starting Redis...'
    Start-Process -FilePath $redisServer `
        -ArgumentList $redisConfig `
        -WorkingDirectory $redisHome `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logDir 'redis.out.log') `
        -RedirectStandardError (Join-Path $logDir 'redis.err.log') | Out-Null
    Wait-RedisEndpoint
    Write-Host "Redis started: 127.0.0.1:$redisPort"
}

$backendPid = Get-ListeningProcess $backendPort
if ($backendPid) {
    if (-not (Test-HttpEndpoint "$backendUrl/api/health")) {
        $owners = $backendPid -join ', '
        throw "Port $backendPort is already in use by process $owners, but the TestPilot backend is not healthy."
    }
    Write-Host "Backend is already running: $backendUrl"
} else {
    Write-Host 'Starting backend...'
    Start-Process -FilePath 'mvn.cmd' `
        -ArgumentList @('spring-boot:run', '-Dspring-boot.run.profiles=local') `
        -WorkingDirectory $backend `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logDir 'backend.out.log') `
        -RedirectStandardError (Join-Path $logDir 'backend.err.log') | Out-Null
    Wait-HttpEndpoint "$backendUrl/api/health" 'Backend'
    Write-Host "Backend started: $backendUrl"
}

$frontendPid = Get-ListeningProcess $frontendPort
if ($frontendPid) {
    if (-not (Test-HttpEndpoint $frontendUrl)) {
        $owners = $frontendPid -join ', '
        throw "Port $frontendPort is already in use by process $owners, but the TestPilot frontend is not reachable."
    }
    Write-Host "Frontend is already running: $frontendUrl"
} else {
    Write-Host 'Starting frontend...'
    Start-Process -FilePath 'npm.cmd' `
        -ArgumentList @('run', 'dev:local') `
        -WorkingDirectory $frontend `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logDir 'frontend.out.log') `
        -RedirectStandardError (Join-Path $logDir 'frontend.err.log') | Out-Null
    Wait-HttpEndpoint $frontendUrl 'Frontend'
    Write-Host "Frontend started: $frontendUrl"
}

Write-Host ''
Write-Host 'TestPilot local services are ready.'
Write-Host "Open: $frontendUrl/"
Write-Host "Logs: $logDir"
