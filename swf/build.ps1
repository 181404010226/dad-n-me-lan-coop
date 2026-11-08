param([string]$FFDec=$env:FFDEC_JAR,[string]$Jdk=$env:JAVA_HOME,[string]$Python='python')

. (Join-Path $PSScriptRoot '../tools/environment.ps1')
$Jdk=Resolve-GameJdk $Jdk
if(!$FFDec){throw 'Set FFDEC_JAR to FFDec 26.4.0/ffdec.jar'}
$ErrorActionPreference='Stop'
$swfRoot=$PSScriptRoot
$outputRoot=Join-Path $swfRoot 'build'
New-Item -ItemType Directory -Force $outputRoot | Out-Null
function Run([string]$exe,[string[]]$arguments){ & $exe @arguments; if($LASTEXITCODE -ne 0){throw "Failed: $exe"} }
$original=Join-Path $swfRoot 'original/game.swf'
if(!(Test-Path -LiteralPath $original)){Run $Python @('-X','utf8',"$swfRoot\..\tools\fetch-original.py")}
$exports=Join-Path $swfRoot 'exports'
if(!(Test-Path -LiteralPath (Join-Path $exports 'scripts/frame_1/DoAction.as'))){Run "$Jdk\bin\java.exe" @('-jar',$FFDec,'-export','script',$exports,$original)}
Run $Python @('-X','utf8',"$swfRoot\make-patch.py")
Run "$Jdk\bin\javac.exe" @('-encoding','UTF-8','-cp',$FFDec,'-d',$outputRoot,"$swfRoot\PatchAS2.java","$swfRoot\Palette.java","$swfRoot\Localize.java","$swfRoot\Nonviolent.java","$swfRoot\VerifyNonviolent.java","$swfRoot\Presentation.java")
Run "$Jdk\bin\java.exe" @('-cp',"$outputRoot;$FFDec",'PatchAS2',"$swfRoot\original\game.swf","$outputRoot\code.swf","$swfRoot\patches")
Run "$Jdk\bin\java.exe" @('-cp',"$outputRoot;$FFDec",'Localize',"$outputRoot\code.swf","$outputRoot\localized-code.swf","$swfRoot\..\art\imagen")
Run "$Jdk\bin\java.exe" @('-cp',"$outputRoot;$FFDec",'Nonviolent',"$outputRoot\localized-code.swf","$outputRoot\clean-code.swf","$swfRoot\..\art\nonviolent")
Run "$Jdk\bin\java.exe" @('-cp',"$outputRoot;$FFDec",'VerifyNonviolent',"$outputRoot\localized-code.swf","$outputRoot\clean-code.swf","$outputRoot\cosmetic-audit.json")
Run "$Jdk\bin\java.exe" @('-cp',"$outputRoot;$FFDec",'Palette',"$outputRoot\clean-code.swf","$outputRoot\palette-game.swf")
Run "$Jdk\bin\java.exe" @('-cp',"$outputRoot;$FFDec",'Presentation',"$outputRoot\palette-game.swf","$outputRoot\game.swf","$outputRoot\view.swf","$swfRoot\visual-renderer.as")
New-Item -ItemType Directory -Force "$outputRoot\wrapper\frame_1" | Out-Null
((Get-Content -LiteralPath "$outputRoot\sound-links.as" -Raw)+(Get-Content -LiteralPath "$swfRoot\wrapper.as" -Raw)+(Get-Content -LiteralPath "$swfRoot\visual-capture.as" -Raw)) | Set-Content -Encoding UTF8 -LiteralPath "$outputRoot\wrapper\frame_1\DoAction.as"
Run "$Jdk\bin\java.exe" @('-cp',"$outputRoot;$FFDec",'PatchAS2',"$swfRoot\empty.swf","$outputRoot\bridge.swf","$outputRoot\wrapper",'8')
$assets=Join-Path (Split-Path $swfRoot -Parent) 'app\assets'
Copy-Item -LiteralPath "$outputRoot\game.swf" -Destination "$assets\game.swf" -Force
Copy-Item -LiteralPath "$outputRoot\game.swf" -Destination "$assets\duo.swf" -Force
Copy-Item -LiteralPath "$outputRoot\bridge.swf" -Destination "$assets\bridge.swf" -Force
Copy-Item -LiteralPath "$outputRoot\view.swf" -Destination "$assets\view.swf" -Force
Write-Output 'AVM1 patched game and AVM1 bridge built.'
