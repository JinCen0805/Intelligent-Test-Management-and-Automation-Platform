#Requires -Version 5.1
<#
.SYNOPSIS
    TestPilot unified verification entry (used by local dev and CI).

.DESCRIPTION
    Runs backend tests and frontend build in order. Fails with a non-zero exit
    code if any step fails. Use -Scope backend / -Scope frontend for fast,
    task-level checks.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts/verify.ps1
    powershell -ExecutionPolicy Bypass -File scripts/verify.ps1 -Scope backend
    powershell -ExecutionPolicy Bypass -File scripts/verify.ps1 -Scope frontend
#>
param(
    [ValidateSet('all', 'backend', 'frontend')]
    [string]$Scope = 'all'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$failures = New-Object System.Collections.Generic.List[string]

function Resolve-Tool {
    param([string[]]$Candidates)
    foreach ($candidate in $Candidates) {
        $command = Get-Command $candidate -ErrorAction SilentlyContinue
        if ($command) {
            return $command.Source
        }
    }
    throw "Command not found: $($Candidates -join ' / '). Install it and add it to PATH."
}

function Invoke-VerifyStep {
    param(
        [string]$Name,
        [string]$WorkingDirectory,
        [string[]]$Command
    )
    Write-Host ''
    Write-Host "==> $Name" -ForegroundColor Cyan
    Write-Host "    $WorkingDirectory"
    Push-Location -LiteralPath $WorkingDirectory
    try {
        $exe = $Command[0]
        $arguments = @()
        if ($Command.Length -gt 1) {
            $arguments = $Command[1..($Command.Length - 1)]
        }
        & $exe @arguments
        if ($LASTEXITCODE -ne 0) {
            throw "exit code $LASTEXITCODE for: $($Command -join ' ')"
        }
        Write-Host "PASSED: $Name" -ForegroundColor Green
    } catch {
        $script:failures.Add("$Name -> $($_.Exception.Message)")
        Write-Host "FAILED: $Name" -ForegroundColor Red
    } finally {
        Pop-Location
    }
}

if ($Scope -in @('all', 'backend')) {
    try {
        $mvn = Resolve-Tool @('mvn.cmd', 'mvn')
        Invoke-VerifyStep -Name 'backend tests (mvn -B test)' -WorkingDirectory (Join-Path $root 'backend') -Command @($mvn, '-B', 'test')
    } catch {
        $failures.Add("backend toolchain -> $($_.Exception.Message)")
    }
}

if ($Scope -in @('all', 'frontend')) {
    try {
        $npm = Resolve-Tool @('npm.cmd', 'npm')
        $frontend = Join-Path $root 'frontend'
        if (-not (Test-Path -LiteralPath (Join-Path $frontend 'node_modules'))) {
            if (Test-Path -LiteralPath (Join-Path $frontend 'package-lock.json')) {
                Invoke-VerifyStep -Name 'install frontend deps (npm ci)' -WorkingDirectory $frontend -Command @($npm, 'ci')
            } else {
                Invoke-VerifyStep -Name 'install frontend deps (npm install)' -WorkingDirectory $frontend -Command @($npm, 'install')
            }
        }
        Invoke-VerifyStep -Name 'frontend build (npm run build)' -WorkingDirectory $frontend -Command @($npm, 'run', 'build')
        Invoke-VerifyStep -Name 'frontend unit tests (npm run test)' -WorkingDirectory $frontend -Command @($npm, 'run', 'test')
    } catch {
        $failures.Add("frontend toolchain -> $($_.Exception.Message)")
    }
}

Write-Host ''
if ($failures.Count -gt 0) {
    Write-Host 'VERIFICATION FAILED:' -ForegroundColor Red
    foreach ($failure in $failures) {
        Write-Host "  - $failure" -ForegroundColor Red
    }
    exit 1
}

Write-Host 'ALL CHECKS PASSED.' -ForegroundColor Green
exit 0
