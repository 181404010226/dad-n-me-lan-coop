param([string]$Sdk=$env:ANDROID_SDK_ROOT,[string]$Jdk=$env:JAVA_HOME,[string]$SigningKey='')

. (Join-Path $PSScriptRoot '../tools/environment.ps1')
$Jdk=Resolve-GameJdk $Jdk
$Sdk=Resolve-GameSdk $Sdk
$ErrorActionPreference='Stop'
$projectPath=$PSScriptRoot
if(!$SigningKey){$SigningKey=Join-Path $projectPath 'build\debug.keystore'}
# Windows aapt2 cannot reliably handle non-ASCII paths. Map only this checkout temporarily.
if($projectPath -match '[^\x00-\x7F]'){
    $mappedDrive=$null
    foreach($candidate in @('Z:','Y:','X:','V:','U:','T:','S:')){if(!(Test-Path "$candidate\")){& subst.exe $candidate $projectPath;if($LASTEXITCODE -eq 0){$mappedDrive=$candidate;break}}}
    if(!$mappedDrive){throw 'No free drive letter for Android resource compilation'}
    try{ & "$mappedDrive\build.ps1" -Sdk $Sdk -Jdk $Jdk -SigningKey $SigningKey }finally{ & subst.exe $mappedDrive /D }
    Write-Output "APK available: $projectPath\build\unified\rgm36-duo.apk"
    return
}
$rolePath=Join-Path $projectPath 'build\unified'
$buildPath=Join-Path $rolePath ('compile-'+[guid]::NewGuid().ToString('N').Substring(0,8))
if(!(Test-Path -LiteralPath "$projectPath\assets\game.swf")){throw 'Run tools/prepare.py with your original SWF before building.'}
$env:JAVA_HOME=$Jdk
$env:PATH="$Jdk\bin;$env:PATH"
$bt=Join-Path $Sdk 'build-tools\35.0.1'
$platform=Join-Path $Sdk 'platforms\android-35\android.jar'
$manifestText=Get-Content -LiteralPath "$projectPath\AndroidManifest.xml" -Raw
New-Item -ItemType Directory -Force -Path $buildPath | Out-Null
$manifestPath=Join-Path $buildPath 'AndroidManifest.xml'
$manifestText | Set-Content -LiteralPath $manifestPath -Encoding UTF8
function Run([string]$program,[string[]]$arguments){ & $program @arguments; if($LASTEXITCODE -ne 0){throw "Failed: $program"} }
New-Item -ItemType Directory -Force -Path "$buildPath\gen","$buildPath\classes","$buildPath\dex" | Out-Null
$assetPath=Join-Path $buildPath 'assets'
$assetRoot=Join-Path $projectPath 'assets'
Get-ChildItem -LiteralPath $assetRoot -Recurse -File | ForEach-Object {
 $relative=$_.FullName.Substring($assetRoot.Length).TrimStart('\')
 if($relative -notin @('index.html','app.js','qr.js','game.swf')){
  $target=Join-Path $assetPath $relative
  New-Item -ItemType Directory -Force -Path (Split-Path $target -Parent) | Out-Null
  Copy-Item -LiteralPath $_.FullName -Destination $target
 }
}
Run "$bt\aapt2.exe" @('compile','--dir',"$projectPath\res",'-o',"$buildPath\resources.zip")
Run "$bt\aapt2.exe" @('link','-I',$platform,'--manifest',$manifestPath,'--custom-package','com.party.rgm36','--java',"$buildPath\gen",'-A',$assetPath,'-o',"$buildPath\base.apk","$buildPath\resources.zip")
$javaFiles=@(Get-ChildItem "$projectPath\src","$buildPath\gen" -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
Run "$Jdk\bin\javac.exe" (@('-encoding','UTF-8','--release','8','-classpath',$platform,'-d',"$buildPath\classes")+$javaFiles)
Run "$Jdk\bin\jar.exe" @('cf',"$buildPath\classes.jar",'-C',"$buildPath\classes",'.')
Run "$bt\d8.bat" @('--lib',$platform,'--min-api','26','--output',"$buildPath\dex","$buildPath\classes.jar")
Copy-Item -LiteralPath "$buildPath\base.apk" -Destination "$buildPath\with-dex.apk" -Force
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[System.IO.Compression.ZipFile]::Open("$buildPath\with-dex.apk",[System.IO.Compression.ZipArchiveMode]::Update)
try{
    # Windows aapt2 writes nested asset entry names with backslashes; AssetManager requires '/'.
    foreach($entry in @($zip.Entries)){
        if($entry.FullName.Contains('\')){
            $name=$entry.FullName.Replace('\','/')
            $memory=[System.IO.MemoryStream]::new()
            $inputStream=$entry.Open()
            try{$inputStream.CopyTo($memory)}finally{$inputStream.Dispose()}
            $newEntry=$zip.CreateEntry($name,[System.IO.Compression.CompressionLevel]::Optimal)
            $outputStream=$newEntry.Open()
            try{$memory.Position=0;$memory.CopyTo($outputStream)}finally{$outputStream.Dispose();$memory.Dispose()}
            $entry.Delete()
        }
    }
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$buildPath\dex\classes.dex",'classes.dex') | Out-Null
}finally{$zip.Dispose()}
Run "$bt\zipalign.exe" @('-f','4',"$buildPath\with-dex.apk","$buildPath\aligned.apk")
$key=$SigningKey
if(!(Test-Path -LiteralPath $key)){Run "$Jdk\bin\keytool.exe" @('-genkeypair','-keystore',$key,'-storepass','android','-keypass','android','-alias','androiddebugkey','-dname','CN=Classic Duo Local Development','-keyalg','RSA','-keysize','2048','-validity','10000')}
Run "$bt\apksigner.bat" @('sign','--ks',$key,'--ks-pass','pass:android','--key-pass','pass:android','--out',"$buildPath\rgm36-duo.apk","$buildPath\aligned.apk")
Run "$bt\apksigner.bat" @('verify',"$buildPath\rgm36-duo.apk")
Copy-Item -LiteralPath "$buildPath\rgm36-duo.apk" -Destination "$rolePath\rgm36-duo.apk" -Force
Write-Output "APK built: $rolePath\rgm36-duo.apk"
