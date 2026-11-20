$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$frontendPort = 5173
$redisPort = 6379
$backendPort = 8080
$redisServerPath = Join-Path (Join-Path 'E:\' (-join ([char]0x8F6F, [char]0x4EF6, [char]0x5B89, [char]0x88C5))) 'Redis-x64-5.0.14.1\redis-server.exe'

$failures = New-Object System.Collections.Generic.List[string]

function Get-PortOwners([int]$port) {
    return @(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique)
}

function Get-ProcessInfo([int]$processId) {
    return Get-CimInstance Win32_Process -Filter "ProcessId=$processId" -ErrorAction SilentlyContinue
}

function Assert-ExpectedProcess([int]$port, [int]$processId, [string]$serviceName) {
    $info = Get-ProcessInfo $processId
    if (-not $info) {
        throw "Process $processId for $serviceName already exited."
    }
    $processName = [IO.Path]::GetFileNameWithoutExtension([string]$info.Name)
    $commandLine = [string]$info.CommandLine
    $parentInfo = Get-ProcessInfo ([int]$info.ParentProcessId)
    $backendProcessMatches = $processName -eq 'java' -and $commandLine -like '*com.testpilot.platform.TestPilotApplication*'
    $backendParentMatches = $parentInfo -and
        [IO.Path]::GetFileNameWithoutExtension([string]$parentInfo.Name) -eq 'java' -and
        [string]$parentInfo.CommandLine -like "*$root\backend*" -and
        [string]$parentInfo.CommandLine -like '*spring-boot:run*'
    $isExpected = switch ($port) {
        $redisPort { $processName -eq 'redis-server' -and $commandLine -like "*$redisServerPath*" }
        $frontendPort { $processName -eq 'node' -and $commandLine -like "*$root\frontend*" }
        $backendPort { $backendProcessMatches -and $backendParentMatches }
        default { $false }
    }
    if (-not $isExpected) {
        throw "Refusing to stop $serviceName on port ${port}: process $processId ($processName) does not belong to this project."
    }
    return $info
}

function Stop-PortService([int]$port, [string]$serviceName) {
    $owners = Get-PortOwners $port
    if ($owners.Count -eq 0) {
        Write-Host "$serviceName is not running."
        return
    }

    $processInfos = @(foreach ($processId in $owners) {
        Assert-ExpectedProcess $port $processId $serviceName
    })

    foreach ($info in $processInfos) {
        $processId = [int]$info.ProcessId
        Write-Host "Stopping $serviceName (PID $processId)..."
        Stop-Process -Id $processId -Force -ErrorAction Stop

        if ($port -eq $backendPort) {
            $parentId = [int]$info.ParentProcessId
            $parentInfo = Get-ProcessInfo $parentId
            if ($parentInfo -and [string]$parentInfo.CommandLine -like "*$root\backend*" -and
                    [string]$parentInfo.CommandLine -like '*spring-boot:run*') {
                Write-Host "Stopping backend Maven process (PID $parentId)..."
                Stop-Process -Id $parentId -Force -ErrorAction SilentlyContinue
            }
        }
    }

    $deadline = (Get-Date).AddSeconds(15)
    while ((Get-Date) -lt $deadline) {
        if ((Get-PortOwners $port).Count -eq 0) {
            Write-Host "$serviceName stopped."
            return
        }
        Start-Sleep -Milliseconds 500
    }
    throw "$serviceName did not stop within 15 seconds."
}

function Invoke-StopService([int]$port, [string]$serviceName, [bool]$skipIfNotManaged = $false) {
    try {
        Stop-PortService $port $serviceName
    } catch {
        $message = [string]$_.Exception.Message
        if ($skipIfNotManaged -and $message -like 'Refusing to stop*') {
            Write-Host "$serviceName is not managed by this project; skipped."
            return
        }
        Write-Host "FAILED to stop ${serviceName}: $message"
        $failures.Add($serviceName)
    }
}

# Stop in dependency order: frontend -> backend -> Redis.
Invoke-StopService $frontendPort 'Frontend'
Invoke-StopService $backendPort 'Backend'
Invoke-StopService $redisPort 'Redis' $true

if ($failures.Count -gt 0) {
    Write-Host ''
    Write-Host ("Stop finished with issues: {0}" -f ($failures -join ', '))
    exit 1
}

Write-Host ''
Write-Host 'TestPilot local services stopped.'
exit 0
