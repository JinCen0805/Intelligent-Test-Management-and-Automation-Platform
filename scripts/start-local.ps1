$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root 'frontend'
$backend = Join-Path $root 'backend'
$jar = Join-Path $backend 'target\testpilot-platform-0.1.0-SNAPSHOT.jar'

if (-not (Test-Path -LiteralPath $jar)) {
  throw "Backend jar not found: $jar. Run 'mvn package -DskipTests' in backend first."
}

foreach ($port in 5173, 18080) {
  $listener = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
  if ($listener) {
    $owners = ($listener | Select-Object -ExpandProperty OwningProcess -Unique) -join ', '
    throw "Port $port is already in use by process $owners. Stop it or choose a clean development session."
  }
}

Start-Process -FilePath 'npm.cmd' `
  -ArgumentList 'run', 'dev:local' `
  -WorkingDirectory $frontend `
  -WindowStyle Hidden

Start-Process -FilePath 'java' `
  -ArgumentList '-jar', $jar, '--spring.profiles.active=local' `
  -WorkingDirectory $backend `
  -WindowStyle Hidden

Write-Host 'TestPilot local services started.'
Write-Host 'Frontend: http://127.0.0.1:5173/'
Write-Host 'Backend:  http://127.0.0.1:18080/'
