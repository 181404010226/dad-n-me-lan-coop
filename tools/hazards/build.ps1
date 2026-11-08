param([string]$FFDec=$env:FFDEC_JAR,[string]$Jdk=$env:JAVA_HOME)

. (Join-Path $PSScriptRoot '../environment.ps1')
$Jdk=Resolve-GameJdk $Jdk
if(!$FFDec){throw 'Set FFDEC_JAR to FFDec 26.4.0/ffdec.jar'}
$ErrorActionPreference='Stop'
$project=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$qa=Join-Path $project 'build\hazard-qa'
New-Item -ItemType Directory -Force "$qa\wrapper\frame_1" | Out-Null
$wrapper=(Get-Content -LiteralPath "$project\swf\build\sound-links.as" -Raw)+(Get-Content -LiteralPath "$project\swf\wrapper.as" -Raw)+(Get-Content -LiteralPath "$project\swf\visual-capture.as" -Raw)+(Get-Content -LiteralPath "$PSScriptRoot\fixture.as" -Raw)
$wrapper | Set-Content -Encoding UTF8 -LiteralPath "$qa\wrapper\frame_1\DoAction.as"
& "$Jdk\bin\java.exe" -cp "$project\swf\build;$FFDec" PatchAS2 "$project\swf\empty.swf" "$qa\bridge.swf" "$qa\wrapper" 8
if($LASTEXITCODE -ne 0){throw 'QA bridge compilation failed'}
& "$Jdk\bin\javac.exe" -encoding UTF-8 -cp "$project\app\build\test-classes;$project\app\lib\json.jar" -d "$project\app\build\test-classes" "$PSScriptRoot\FixtureServer.java"
if($LASTEXITCODE -ne 0){throw 'QA server compilation failed'}
