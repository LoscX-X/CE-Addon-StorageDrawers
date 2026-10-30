param(
    [Parameter(Mandatory)][string]$PaperHome,
    [Parameter(Mandatory)][string]$CraftEngineJar,
    [Parameter(Mandatory)][string]$CoreProtectJar,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$Python = 'python',
    [string]$StorageDrawersSource = (Join-Path $PSScriptRoot 'vendor/StorageDrawers'),
    [string]$MinecraftClientJar = '',
    [string]$MinecraftAssets = '',
    [string]$MinecraftAssetIndex = '34',
    [switch]$SkipPack
)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$sdkRoot = (Resolve-Path -LiteralPath $PaperHome).Path
$extension = if ($IsWindows) { '.exe' } else { '' }
$javac = if ($JavaHome) { Join-Path $JavaHome "bin/javac$extension" } else { (Get-Command javac -ErrorAction Stop).Source }
$jar = if ($JavaHome) { Join-Path $JavaHome "bin/jar$extension" } else { (Get-Command jar -ErrorAction Stop).Source }
if (!$SkipPack) {
    if (!$MinecraftClientJar -or !$MinecraftAssets) { throw 'Resource generation requires -MinecraftClientJar and -MinecraftAssets.' }
    & $Python (Join-Path $projectRoot 'tools/Export-Vanilla-Models.py') $MinecraftClientJar
    if ($LASTEXITCODE -ne 0) { throw 'Vanilla model export failed' }
    $variables = @('STORAGEDRAWERS_PYTHON','STORAGEDRAWERS_ASSETS','STORAGEDRAWERS_ASSET_INDEX')
    $previous = @{}
    foreach ($name in $variables) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
    try {
        $env:STORAGEDRAWERS_PYTHON = $Python
        $env:STORAGEDRAWERS_ASSETS = $MinecraftAssets
        $env:STORAGEDRAWERS_ASSET_INDEX = $MinecraftAssetIndex
        & node (Join-Path $projectRoot 'tools/Convert-Pack.cjs') $StorageDrawersSource
        if ($LASTEXITCODE -ne 0) { throw 'Pack conversion failed' }
    } finally {
        foreach ($name in $variables) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
    }
}
$classes = Join-Path $projectRoot ('build/classes-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$classpath = @((Resolve-Path -LiteralPath $CraftEngineJar).Path)
$classpath += (Resolve-Path -LiteralPath $CoreProtectJar).Path
$ceLibraries = Join-Path (Split-Path -Parent (Resolve-Path -LiteralPath $CraftEngineJar).Path) 'CraftEngine/libs'
$classpath += @(Get-ChildItem -LiteralPath $ceLibraries -Recurse -File -Filter '*.jar' | ForEach-Object FullName)
$platformClasspath=@()
$zip = [IO.Compression.ZipFile]::OpenRead((Join-Path $sdkRoot 'paper.jar'))
try { foreach ($kind in @('libraries','versions')) {
    $reader = [IO.StreamReader]::new($zip.GetEntry("META-INF/$kind.list").Open())
    try { foreach ($line in ($reader.ReadToEnd() -split "`n")) {
        $parts = $line.Trim() -split "`t"
        if ($parts.Count -eq 3) { $platformJar=Join-Path $sdkRoot "$kind/$($parts[2])"; $classpath += $platformJar; $platformClasspath += $platformJar }
    } } finally { $reader.Dispose() }
} } finally { $zip.Dispose() }
$sources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/main/java') -Recurse -File -Filter '*.java' | ForEach-Object FullName)
$compilerArgs = Join-Path $projectRoot ('build/javac-' + [IO.Path]::GetFileName($classes) + '.args')
$compileOptions = @('-encoding','UTF-8','--release','25','-proc:none','-cp',('"' + (($classpath -join ';').Replace('\','/')) + '"'),'-d',('"' + $classes.Replace('\','/') + '"'))
$compileOptions += @($sources | ForEach-Object { '"' + $_.Replace('\','/') + '"' })
[IO.File]::WriteAllLines($compilerArgs,$compileOptions,[Text.UTF8Encoding]::new($false))
& $javac "@$compilerArgs"
if ($LASTEXITCODE -ne 0) { throw 'StorageDrawers-CE compilation failed' }
$jarFile = Join-Path $projectRoot 'build/CE-Addon-StorageDrawers.jar'
& $jar --create --file $jarFile -C $classes . -C (Join-Path $projectRoot 'src/main/resources') .
if ($LASTEXITCODE -ne 0) { throw 'StorageDrawers-CE packaging failed' }
$apiJar=Join-Path $projectRoot 'build/CE-Addon-StorageDrawers-api.jar'
& $jar --create --file $apiJar -C $classes com/cendreal/storagedrawers/api
if($LASTEXITCODE -ne 0){throw 'API packaging failed'}
$probeClasses=Join-Path $projectRoot ('build/api-probe-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $probeClasses -Force | Out-Null
& $javac -encoding UTF-8 --release 25 -proc:none -cp ((@($apiJar)+$platformClasspath) -join ';') -d $probeClasses (Join-Path $projectRoot 'tools/api-consumer/consumer/ApiContractProbe.java')
if($LASTEXITCODE -ne 0){throw 'Standalone API consumer compilation failed'}
& $jar --create --file (Join-Path $projectRoot 'build/CE-Addon-StorageDrawers-api-probe.jar') -C $probeClasses .
if($LASTEXITCODE -ne 0){throw 'Standalone API consumer packaging failed'}
@{ craftEngine = @{ path=$CraftEngineJar; sha256=(Get-FileHash -LiteralPath $CraftEngineJar -Algorithm SHA256).Hash }; coreProtectCompileOnly=@{path=$CoreProtectJar;sha256=(Get-FileHash -LiteralPath $CoreProtectJar -Algorithm SHA256).Hash}; artifact = @{path=$jarFile;sha256=(Get-FileHash -LiteralPath $jarFile -Algorithm SHA256).Hash}; api=@{path=$apiJar;sha256=(Get-FileHash -LiteralPath $apiJar -Algorithm SHA256).Hash}; classes=$classes } | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $projectRoot 'build/build-receipt.json') -Encoding utf8
Get-Item -LiteralPath $jarFile | Select-Object FullName,Length
