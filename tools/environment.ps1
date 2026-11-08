function Resolve-GameJdk([string]$Value) {
    if ($Value -and (Test-Path -LiteralPath (Join-Path $Value 'bin/javac.exe'))) { return [IO.Path]::GetFullPath($Value) }
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) { return $env:JAVA_HOME }
    $compiler=Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($compiler) { return Split-Path (Split-Path $compiler.Source -Parent) -Parent }
    throw 'Install JDK 17 and set JAVA_HOME, or pass -Jdk <directory>.'
}
function Resolve-GameSdk([string]$Value) {
    foreach ($candidate in @($Value,$env:ANDROID_SDK_ROOT,$env:ANDROID_HOME)) {
        if ($candidate -and (Test-Path -LiteralPath (Join-Path $candidate 'platforms/android-35/android.jar'))) { return $candidate }
    }
    throw 'Install Android platform 35 / build-tools 35.0.1 and set ANDROID_SDK_ROOT, or pass -Sdk <directory>.'
}
