param([string]$Jdk=$env:JAVA_HOME)

. (Join-Path $PSScriptRoot '../../tools/environment.ps1')
$Jdk=Resolve-GameJdk $Jdk
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
New-Item -ItemType Directory -Force -Path "$taskRoot\build\test-classes" | Out-Null
$core=@('AutoProtocol','RoomWorld','RoomServer','LanDiscovery','AutoPairing') | ForEach-Object { "$taskRoot\src\com\party\rgm36\$_.java" }
$tests=Get-ChildItem -LiteralPath "$taskRoot\test" -Filter '*.java' | ForEach-Object { $_.FullName }
& "$Jdk\bin\javac.exe" -encoding UTF-8 -cp "$taskRoot\lib\json.jar" -d "$taskRoot\build\test-classes" @core @tests
if($LASTEXITCODE -ne 0){throw 'Java compilation failed'}
& "$Jdk\bin\java.exe" -cp "$taskRoot\build\test-classes;$taskRoot\lib\json.jar" com.party.rgm36.RoomTest
if($LASTEXITCODE -ne 0){throw 'Room tests failed'}

& "$Jdk\bin\java.exe" -cp "$taskRoot\build\test-classes;$taskRoot\lib\json.jar" com.party.rgm36.AutoPairTest
if($LASTEXITCODE -ne 0){throw 'Automatic pairing tests failed'}

& "$Jdk\bin\java.exe" -cp "$taskRoot\build\test-classes;$taskRoot\lib\json.jar" com.party.rgm36.UnifiedPairTest
if($LASTEXITCODE -ne 0){throw 'Unified APK election tests failed'}

& "$Jdk\bin\java.exe" -cp "$taskRoot\build\test-classes;$taskRoot\lib\json.jar" com.party.rgm36.AuthorityTest
if($LASTEXITCODE -ne 0){throw 'Authority protocol tests failed'}
