# Offline build script for the MMCR x EMI compatibility patch.
#
# This machine cannot reach maven.neoforged.net, so Gradle/NeoGradle is not usable here.
# Instead we compile with javac against:
#   1) the already de-obfuscated Minecraft + NeoForge merged jar from the NeoGradle cache
#   2) the NeoForge/FML/Mixin libraries of the local instance
#   3) the MMCR and JEI jars of the local instance
#
# Usage (from the repository root):
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\build.ps1
# Optional:
#   -Instance "E:\PCL2\.minecraft\versions\o-cek&pt 2"   instance that provides MMCR/JEI jars
#   -Stage    "D:\mmcremi_build"                         ASCII scratch directory

[CmdletBinding()]
param(
    [string]$Instance = 'E:\PCL2\.minecraft\versions\o-cek&pt 2',
    [string]$Stage = 'D:\mmcremi_build'
)

$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent $PSScriptRoot
$javaHome = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$javac = Join-Path $javaHome 'bin\javac.exe'
$jarTool = Join-Path $javaHome 'bin\jar.exe'
$javap = Join-Path $javaHome 'bin\javap.exe'

# De-obfuscated (official mappings) Minecraft + NeoForge merged jar produced by NeoGradle.
$deobf = 'C:\Users\PC\.gradle\caches\neoformruntime\intermediate_results\binaryWithNeoForge_d6fda8c10c8f483212138b528a48afa7326d53f3_output.jar'
$libs = 'E:\PCL2\.minecraft\libraries'

$modsDir = Join-Path $Instance 'mods'
if (-not (Test-Path -LiteralPath $modsDir)) { throw "Instance mods directory not found: $modsDir" }

$mmcrJar = Get-ChildItem $modsDir -Filter 'mmcr-neoforge-*.jar' |
    Sort-Object Name -Descending | Select-Object -First 1
$jeiJar = Get-ChildItem $modsDir -Filter '*jei-1.21.1-neoforge-*.jar' |
    Sort-Object Name -Descending | Select-Object -First 1
$emiJar = Get-ChildItem $modsDir -Filter 'emi-*.jar' |
    Sort-Object Name -Descending | Select-Object -First 1

if (-not $mmcrJar) { throw "MMCR jar not found in $modsDir" }
if (-not $jeiJar) { throw "JEI jar not found in $modsDir (JEI API is required to compile)" }
if (-not $emiJar) { throw "EMI jar not found in $modsDir (EMI is required to compile the JEMI mixins)" }

$classpath = @(
    $deobf,
    (Join-Path $libs 'net\neoforged\fancymodloader\loader\4.0.45\loader-4.0.45.jar'),
    (Join-Path $libs 'net\neoforged\bus\8.0.5\bus-8.0.5.jar'),
    (Join-Path $libs 'com\mojang\logging\1.7.12\logging-1.7.12.jar'),
    (Join-Path $libs 'org\slf4j\slf4j-api\2.0.17\slf4j-api-2.0.17.jar'),
    (Join-Path $libs 'org\spongepowered\mixin\0.8.5\mixin-0.8.5.jar'),
    # Provides net.neoforged.api.distmarker.Dist used by @OnlyIn annotations on vanilla classes.
    (Join-Path $libs 'net\neoforged\mergetool\2.0.7\mergetool-2.0.7-api.jar'),
    $mmcrJar.FullName,
    $jeiJar.FullName,
    $emiJar.FullName
)

$missing = $classpath | Where-Object { -not (Test-Path -LiteralPath $_) }
if ($missing) { throw ("Missing compile dependency:`n" + ($missing -join "`n")) }

Write-Host '=== 1/5 prepare ASCII build directory ==='
if (Test-Path $Stage) { Remove-Item -Recurse -Force $Stage }
New-Item -ItemType Directory -Force -Path (Join-Path $Stage 'classes') | Out-Null

Write-Host '=== 2/5 mirror sources into the ASCII stage (strip BOM, reject broken encoding) ==='
$srcRoot = Join-Path $repo 'src\main\java'
$stageSrc = Join-Path $Stage 'src'
New-Item -ItemType Directory -Force -Path $stageSrc | Out-Null
Copy-Item -Path (Join-Path $srcRoot '*') -Destination $stageSrc -Recurse -Force

$sources = Get-ChildItem $stageSrc -Recurse -Filter *.java | Sort-Object FullName
if (-not $sources) { throw "No .java sources found: $srcRoot" }

$utf8 = New-Object System.Text.UTF8Encoding($false)
foreach ($s in $sources) {
    $bytes = [System.IO.File]::ReadAllBytes($s.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        [System.IO.File]::WriteAllBytes($s.FullName, $bytes[3..($bytes.Length - 1)])
        Write-Host ("    stripped UTF-8 BOM: " + $s.Name)
    }
    $text = [System.IO.File]::ReadAllText($s.FullName, [System.Text.Encoding]::UTF8)
    if ($text.Contains([char]0xFFFD)) { throw "Source file has broken encoding (U+FFFD): $($s.Name)" }
}
$sources | ForEach-Object { $_.FullName } | Set-Content -Path (Join-Path $Stage 'sources.txt') -Encoding ascii
Write-Host ("    sources: " + $sources.Count)

Write-Host '=== 3/5 javac ==='
$cpArg = ($classpath -join ';')
& $javac -encoding UTF-8 -proc:none -Xlint:-options -d (Join-Path $Stage 'classes') -cp $cpArg "@$(Join-Path $Stage 'sources.txt')"
if ($LASTEXITCODE -ne 0) { throw "javac failed (exit $LASTEXITCODE)" }

Write-Host '=== 4/5 copy resources and package ==='
$resRoot = Join-Path $repo 'src\main\resources'
Copy-Item -Path (Join-Path $resRoot '*') -Destination (Join-Path $Stage 'classes') -Recurse -Force

$buildDir = Join-Path $repo 'build'
New-Item -ItemType Directory -Force -Path $buildDir | Out-Null
$outJar = Join-Path $buildDir 'mmcr_emi_compat-1.2.0.jar'
if (Test-Path $outJar) { Remove-Item -Force $outJar }
& $jarTool --create --file $outJar -C (Join-Path $Stage 'classes') .
if ($LASTEXITCODE -ne 0) { throw "jar packaging failed (exit $LASTEXITCODE)" }

Write-Host '=== 5/5 verify ==='
$entries = & $jarTool --list --file $outJar
$required = @(
    'META-INF/neoforge.mods.toml',
    'mmcr-emi-wordmark.png',
    'mmcremi.mixins.json',
    'mmcremi/MmcrEmiCompat.class',
    'mmcremi/emi/MmcrEmiInputBridge.class',
    'mmcremi/emi/MouseUserInput.class',
    'mmcremi/mixin/MachineStructureCategoryMixin.class',
    'mmcremi/mixin/MachineRecipeCategoryMixin.class',
    'mmcremi/mixin/JemiPluginMixin.class',
    'mmcremi/mixin/JemiRecipeMixin.class',
    'mmcremi/mixin/JemiRecipeExtrasBuilderMixin.class',
    'mmcremi/mixin/RecipeScreenMixin.class'
)
foreach ($r in $required) {
    if ($entries -notcontains $r) { throw "jar is missing $r" }
}
$iconToml = Join-Path $Stage 'classes\META-INF\neoforge.mods.toml'
if (-not (Select-String -Path $iconToml -Pattern 'logoFile="mmcr-emi-wordmark.png"' -Quiet)) {
    throw 'neoforge.mods.toml is missing logoFile="mmcr-emi-wordmark.png"'
}
Write-Host '    mod icon OK (logoFile -> mmcr-emi-wordmark.png)'
Write-Host '    jar layout OK'

# Static check of the two injection points: createRecipeExtras must contain an
# invokeinterface on IRecipeExtrasBuilder.getRecipeSlots() with the expected descriptor.
$needle = 'IRecipeExtrasBuilder.getRecipeSlots:()Lmezz/jei/api/gui/ingredient/IRecipeSlotDrawablesView;'
$targets = @(
    @{ Class = 'cn.howxu.mmcr.compat.jei.MachineStructureCategory'; Method = 'createRecipeExtras' },
    @{ Class = 'cn.howxu.mmcr.compat.jei.MachineRecipeCategory'; Method = 'createRecipeExtras' }
)
foreach ($t in $targets) {
    $bytecode = & $javap -c -p -cp $mmcrJar.FullName $t.Class
    $inMethod = $false
    $found = $false
    foreach ($line in $bytecode) {
        if ($line -match ('^\s+(public|protected|private).*\s' + [regex]::Escape($t.Method) + '\(')) {
            $inMethod = $true
            continue
        }
        if ($inMethod -and $line -match '^\s+(public|protected|private|static).*\(') { break }
        if ($inMethod -and $line.Contains($needle)) { $found = $true; break }
    }
    if (-not $found) { throw ("Injection point check failed: " + $t.Class + "#" + $t.Method + " has no getRecipeSlots() call") }
    Write-Host ("    injection point OK: " + $t.Class + "#" + $t.Method)
}

# Static check of the EMI/JEMI side injection targets.
$jemiPlugin = (& $javap -c -p -cp $emiJar.FullName dev.emi.emi.jemi.JemiPlugin) -join "`n"
if ($jemiPlugin -notmatch 'IRecipeCategory\.getRecipeType') { throw 'JemiPlugin#register: no IRecipeCategory.getRecipeType() call found' }
if ($jemiPlugin -notmatch 'java/util/Set\.contains') { throw 'JemiPlugin#register: no Set.contains() call found' }
Write-Host '    injection point OK: dev.emi.emi.jemi.JemiPlugin#register (category rescue)'

$jemiRecipe = (& $javap -p -cp $emiJar.FullName dev.emi.emi.jemi.JemiRecipe) -join "`n"
if ($jemiRecipe -notmatch 'addWidgets\(dev\.emi\.emi\.api\.widget\.WidgetHolder\)') { throw 'JemiRecipe#addWidgets not found' }
$extrasBuilder = (& $javap -p -cp $emiJar.FullName dev.emi.emi.jemi.impl.extras.JemiRecipeExtrasBuilder) -join "`n"
if ($extrasBuilder -notmatch 'addInputHandler\(mezz\.jei\.api\.gui\.inputs\.IJeiInputHandler\)') { throw 'JemiRecipeExtrasBuilder#addInputHandler not found' }
Write-Host '    injection point OK: JemiRecipe#addWidgets + JemiRecipeExtrasBuilder#addInputHandler (input capture)'

$recipeScreen = (& $javap -p -cp $emiJar.FullName dev.emi.emi.screen.RecipeScreen) -join "`n"
foreach ($m in @('mouseClicked', 'mouseReleased', 'mouseDragged', 'mouseScrolled')) {
    if ($recipeScreen -notmatch ($m + '\(double')) { throw ("RecipeScreen#" + $m + " not found") }
}
Write-Host '    injection point OK: RecipeScreen mouse click/release/drag/scroll (input bridge)'

$size = [Math]::Round((Get-Item $outJar).Length / 1KB, 1)
Write-Host ''
Write-Host ("Build finished: " + $outJar + " (" + $size + " KB)")

