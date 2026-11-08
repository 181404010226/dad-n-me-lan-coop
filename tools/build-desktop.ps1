param([string]$Jdk=$env:JAVA_HOME)

. (Join-Path $PSScriptRoot 'environment.ps1')
$Jdk=Resolve-GameJdk $Jdk
$ErrorActionPreference='Stop'
$repoRoot=Split-Path $PSScriptRoot -Parent
& "$repoRoot\app\test\run.ps1" -Jdk $Jdk
if($LASTEXITCODE -ne 0){throw 'Room tests failed'}
if(!(Test-Path -LiteralPath "$repoRoot\app\assets\game.swf")){throw 'Run tools/prepare.py first'}
$desktopPath=Join-Path $repoRoot 'build\desktop'
New-Item -ItemType Directory -Force -Path $desktopPath | Out-Null
Copy-Item -LiteralPath "$repoRoot\app\assets","$repoRoot\app\lib" -Destination $desktopPath -Recurse -Force
$desktopClasses=Join-Path $repoRoot 'build\desktop-classes'
$packageClasses=Join-Path $desktopClasses 'com\party\rgm36'
New-Item -ItemType Directory -Force -Path $packageClasses | Out-Null
Get-ChildItem "$repoRoot\app\build\test-classes\com\party\rgm36" -Filter '*.class' | Where-Object { $_.Name -eq 'DesktopServer.class' -or $_.Name -like 'RoomServer*.class' -or $_.Name -like 'RoomWorld*.class' } | Copy-Item -Destination $packageClasses -Force
& "$Jdk\bin\jar.exe" cf "$desktopPath\server.jar" -C $desktopClasses com
if($LASTEXITCODE -ne 0){throw 'JAR creation failed'}
if(!(Test-Path -LiteralPath "$desktopPath\runtime\bin\java.exe")){
    & "$Jdk\bin\jlink.exe" --add-modules java.base,java.desktop,java.logging --strip-debug --no-header-files --no-man-pages --compress=2 --output "$desktopPath\runtime"
    if($LASTEXITCODE -ne 0){throw 'Runtime creation failed'}
}
$launcher=@'
param([string]$Jdk=$env:JAVA_HOME)
$javaPath=Join-Path $PSScriptRoot 'runtime\bin\java.exe'
if(!(Test-Path -LiteralPath $javaPath)){$javaPath=Join-Path $Jdk 'bin\java.exe'}
if(!(Test-Path -LiteralPath $javaPath)){$javaPath=(Get-Command java.exe -ErrorAction Stop).Source}
& $javaPath -cp "$PSScriptRoot\server.jar;$PSScriptRoot\lib\json.jar" com.party.rgm36.DesktopServer "$PSScriptRoot\assets" --open
'@
$launcher | Set-Content -Encoding UTF8 -LiteralPath "$desktopPath\start.ps1"
'@echo off','powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1"','pause' | Set-Content -Encoding ASCII -LiteralPath "$desktopPath\Start.cmd"
Write-Output "Desktop package: $desktopPath"
