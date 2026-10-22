<#
  Redis instalado en WSL (Ubuntu), para quien no tenga Docker.
  Uso:
    .\scripts\redis-wsl.ps1 start     # inicia Redis y aplica la política de memoria
    .\scripts\redis-wsl.ps1 status    # responde PONG si está activo
    .\scripts\redis-wsl.ps1 keys      # lista las claves de Distrimarket con su TTL
    .\scripts\redis-wsl.ps1 monitor   # muestra en vivo cada GET/SET/DEL (como el Profiler)
    .\scripts\redis-wsl.ps1 flush     # borra solo las claves distrimarket:* (pide confirmación)
    .\scripts\redis-wsl.ps1 stop
  Instalación (una sola vez): wsl -d Ubuntu-22.04 -u root -- apt-get install -y redis-server
#>
param(
    [ValidateSet("start", "stop", "status", "keys", "monitor", "flush")]
    [string]$Accion = "status",
    [string]$Distro = "Ubuntu-22.04",
    [string]$MaxMemory = "256mb"
)

$PidFile = Join-Path $env:TEMP "distrimarket-redis-wsl.pid"

# El comando viaja en base64: así PowerShell no altera comillas ni agrega caracteres.
function Invoke-Wsl([string]$Comando, [switch]$Root) {
    $b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Comando))
    $bash = "echo $b64 | base64 -d | bash"
    if ($Root) { wsl -d $Distro -u root -- bash -c $bash } else { wsl -d $Distro -- bash -c $bash }
}

switch ($Accion) {
    "start" {
        # WSL apaga la distro poco después de cerrar la última sesión y Redis se detiene con ella:
        # un proceso oculto la mantiene encendida hasta "stop".
        $vivo = (Test-Path $PidFile) -and (Get-Process -Id (Get-Content $PidFile) -ErrorAction SilentlyContinue)
        if (-not $vivo) {
            $proceso = Start-Process wsl.exe -WindowStyle Hidden -PassThru -ArgumentList "-d $Distro -- sleep infinity"
            Set-Content $PidFile $proceso.Id
            Start-Sleep -Seconds 2
        }
        Invoke-Wsl "service redis-server start" -Root
        # Misma política que redis.conf: solo se desalojan claves con TTL.
        Invoke-Wsl "redis-cli CONFIG SET maxmemory $MaxMemory >/dev/null && redis-cli CONFIG SET maxmemory-policy volatile-lru >/dev/null && redis-cli ping"
    }
    "stop" {
        Invoke-Wsl "service redis-server stop" -Root
        if (Test-Path $PidFile) {
            Stop-Process -Id (Get-Content $PidFile) -Force -ErrorAction SilentlyContinue
            Remove-Item $PidFile
        }
    }
    "status" { Invoke-Wsl "redis-cli ping" }
    "keys" {
        Invoke-Wsl 'redis-cli --scan --pattern "distrimarket:*" | sort | while read k; do echo "$k  (ttl: $(redis-cli ttl "$k") s)"; done'
    }
    "monitor" {
        Write-Host "Ctrl+C para salir. Cada linea es una operacion que llega a Redis." -ForegroundColor Cyan
        Invoke-Wsl "redis-cli MONITOR"
    }
    "flush" {
        $ok = Read-Host "Se borraran todas las claves distrimarket:* de este Redis. Escriba SI para continuar"
        if ($ok -eq "SI") {
            Invoke-Wsl 'redis-cli --scan --pattern "distrimarket:*" | xargs -r redis-cli del'
        }
    }
}
