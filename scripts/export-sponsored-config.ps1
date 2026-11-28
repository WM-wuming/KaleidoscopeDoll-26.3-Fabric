param(
    [string]$Source = (Join-Path $PSScriptRoot '../build/KaleidoscopeDoll-Sponsored-Config-26.3.zip'),
    [string]$Output = (Join-Path $PSScriptRoot '../build/KaleidoscopeDoll-Sponsored-Config-26.3'),
    [switch]$RestoreCredits,
    [switch]$Renumber,
    [string]$CreditsCsv,
    [string]$CreditsRef = '62303aa58bbd5ee52498f3ad0fbf4a0c7d04c183'
)

$ErrorActionPreference = 'Stop'
$sourceAssets = 'assets/kaleidoscope_doll/'
$sponsorNamespace = 'kaleidoscope_doll_sponsors'
$sponsorAssets = "assets/$sponsorNamespace/"
$originalReference = 'kaleidoscope_doll:block/doll/'
$sponsorReference = "${sponsorNamespace}:block/doll/"

function Read-JsonEntry($Entry) {
    $reader = [IO.StreamReader]::new($Entry.Open())
    try { return ,($reader.ReadToEnd() | ConvertFrom-Json -AsHashtable -Depth 100 -NoEnumerate) }
    finally { $reader.Dispose() }
}

function Write-JsonFile([string]$Path, $Data) {
    [IO.File]::WriteAllText($Path, (ConvertTo-Json -InputObject $Data -Depth 100 -Compress))
}

function Read-GitFile([string]$Path) {
    $lines = git -C $PSScriptRoot show "${CreditsRef}:$Path"
    if ($LASTEXITCODE -ne 0) { throw "Could not read $Path from upstream commit $CreditsRef." }
    return ($lines -join "`n")
}

function Get-SponsorCredits($OriginalIds, $Ids) {
    # The upstream commit may use a different Java package layout.
    $sourcePaths = @(git -C $PSScriptRoot ls-tree --full-tree -r --name-only $CreditsRef -- src/main/java/ |
        Where-Object { $_ -cmatch '/kaleidoscopedoll/event/ModRegisterEvent\.java$' })
    if ($LASTEXITCODE -ne 0 -or $sourcePaths.Count -ne 1) {
        throw "Could not locate a unique ModRegisterEvent.java in upstream commit $CreditsRef."
    }
    $source = Read-GitFile $sourcePaths[0]
    $tooltipKeys = @{}
    foreach ($match in [regex]::Matches($source, 'registerSpecialTooltips\("(doll_\d+)", "([^"]+)"\)')) {
        $tooltipKeys.Add($match.Groups[1].Value, "tooltip.kaleidoscope_doll.doll.$($match.Groups[2].Value)")
    }
    if ($OriginalIds.Count -ne 634 -or @($OriginalIds | Sort-Object -Unique).Count -ne 634 -or
        $Ids.Count -ne 634 -or @($Ids | Sort-Object -Unique).Count -ne 634) {
        throw 'Expected 634 unique sponsor model IDs.'
    }

    $locales = [ordered]@{}
    foreach ($locale in 'zh_cn', 'en_us', 'ja_jp') {
        $original = Read-GitFile "src/main/resources/${sourceAssets}lang/$locale.json" | ConvertFrom-Json -AsHashtable
        $localized = [ordered]@{}
        for ($i = 0; $i -lt $Ids.Count; $i++) {
            if ($OriginalIds[$i] -cnotmatch "^java:${sponsorNamespace}:(doll_\d+)$" -or !$tooltipKeys.ContainsKey($Matches[1])) {
                throw "No upstream tooltip key for $($OriginalIds[$i])."
            }
            $id = $Ids[$i]
            $credit = $original[$tooltipKeys[$Matches[1]]]
            if (![string]::IsNullOrWhiteSpace($credit)) {
                $localized.Add($id, $credit)
            } elseif ($locale -ne 'ja_jp') {
                throw "No upstream $locale credit for $id."
            }
        }
        # The client already falls back to en_us for missing translations.
        $locales.Add($locale, $localized)
    }
    return $locales
}

function Write-CreditsCsv($OriginalIds, $Ids, $Credits) {
    if (!$CreditsCsv) { return }
    for ($i = 0; $i -lt $Ids.Count; $i++) {
        $id = $Ids[$i]
        [pscustomobject]@{
            'Model ID' = $id
            'Previous model ID' = $OriginalIds[$i]
            'zh_cn' = $Credits.zh_cn[$id]
            'en_us' = $Credits.en_us[$id]
            'ja_jp' = $Credits.ja_jp[$id]
        }
    }
}

$inputPath = (Resolve-Path -LiteralPath $Source).Path
$outputPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Output)
if (Test-Path -LiteralPath $outputPath) { throw "The output already exists: $outputPath" }
$sourceZip = [IO.Compression.ZipFile]::OpenRead($inputPath)
try {
    $metadata = Read-JsonEntry $sourceZip.GetEntry('pack.mcmeta')
    $metadata.pack.description = 'Kaleidoscope Doll: Sponsored Custom Doll Config (26.3)'
    $assetRoot = $sponsorAssets
    $modelEntries = @($sourceZip.Entries | Where-Object { $_.FullName -cmatch "^${assetRoot}models/block/doll/doll_\d+\.json$" })
    if (!$modelEntries.Count) {
        $assetRoot = $sourceAssets
        $modelEntries = @($sourceZip.Entries | Where-Object { $_.FullName -cmatch "^${assetRoot}models/block/doll/doll_\d+\.json$" })
    }
    if ($modelEntries.Count -ne 634) { throw 'Expected 634 sponsor model files.' }
    $modelEntries = @($modelEntries | Sort-Object { [int]([IO.Path]::GetFileNameWithoutExtension($_.Name) -replace '^doll_') })
    $originalIds = @($modelEntries | ForEach-Object { "java:${sponsorNamespace}:$([IO.Path]::GetFileNameWithoutExtension($_.Name))" })
    $ids = if ($Renumber) { @(1..$modelEntries.Count | ForEach-Object { "java:${sponsorNamespace}:doll_$_" }) } else { $originalIds }
    $textureEntries = @($sourceZip.Entries | Where-Object { $_.FullName -cmatch "^${assetRoot}textures/block/doll/[a-z0-9_.-]+\.png(?:\.mcmeta)?$" })
    $credits = [ordered]@{}
    if ($RestoreCredits -or $Renumber -or $CreditsCsv) {
        $credits = Get-SponsorCredits $originalIds $ids
    } else {
        foreach ($locale in 'zh_cn', 'en_us', 'ja_jp') {
            $entry = $sourceZip.GetEntry("${sponsorAssets}custom_dolls/lang/$locale.json")
            if ($entry) { $credits.Add($locale, (Read-JsonEntry $entry)) }
        }
        if (!$credits.Count) { $credits = Get-SponsorCredits $originalIds $ids }
    }
    $models = [ordered]@{}
    $nameMap = @{}
    for ($i = 0; $i -lt $modelEntries.Count; $i++) {
        $nameMap.Add(($originalIds[$i] -split ':')[-1], ($ids[$i] -split ':')[-1])
    }
    foreach ($entry in $modelEntries) {
        $model = Read-JsonEntry $entry
        $name = $nameMap[[IO.Path]::GetFileNameWithoutExtension($entry.Name)]
        if ($model.parent -cmatch '^(?:kaleidoscope_doll|kaleidoscope_doll_sponsors):block/doll/(doll_\d+)$' -and $nameMap.ContainsKey($Matches[1])) {
            $model.parent = "$sponsorReference$($nameMap[$Matches[1]])"
        }
        foreach ($key in @($model.textures.Keys)) {
            $reference = $model.textures[$key]
            if (!$reference -or !$reference.StartsWith($originalReference, [StringComparison]::Ordinal)) { continue }
            $texturePath = $reference.Substring($reference.IndexOf(':') + 1)
            if (!$sourceZip.GetEntry("${assetRoot}textures/$texturePath.png")) {
                throw "Missing source texture $reference for $name"
            }
            $model.textures[$key] = "${sponsorNamespace}:$texturePath"
        }
        $models.Add("$name.json", $model)
    }

    $stage = "$outputPath.$([guid]::NewGuid()).tmp"
    $stagedFiles = [Collections.Generic.List[string]]::new()
    try {
        foreach ($kind in 'models', 'textures', 'lang') { [void][IO.Directory]::CreateDirectory("$stage/custom_dolls/$kind") }
        $stagedFiles.Add("$stage/pack.mcmeta")
        Write-JsonFile "$stage/pack.mcmeta" $metadata
        foreach ($entry in $models.GetEnumerator()) {
            $file = "$stage/custom_dolls/models/$($entry.Key)"
            $stagedFiles.Add($file)
            Write-JsonFile $file $entry.Value
        }
        foreach ($entry in $textureEntries) {
            $file = "$stage/custom_dolls/textures/$($entry.Name)"
            $stagedFiles.Add($file)
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $file)
        }
        foreach ($entry in $credits.GetEnumerator()) {
            $file = "$stage/custom_dolls/lang/$($entry.Key).json"
            $stagedFiles.Add($file)
            Write-JsonFile $file $entry.Value
        }
        [IO.Directory]::Move($stage, $outputPath)
    } finally {
        if ([IO.Directory]::Exists($stage)) {
            foreach ($file in $stagedFiles) { [IO.File]::Delete($file) }
            foreach ($kind in 'models', 'textures', 'lang', '') {
                $directory = "$stage/custom_dolls/$kind"
                if ([IO.Directory]::Exists($directory)) { [IO.Directory]::Delete($directory) }
            }
            [IO.Directory]::Delete($stage)
        }
    }
} finally { $sourceZip.Dispose() }

if ($CreditsCsv) { Write-CreditsCsv $originalIds $ids $credits | Export-Csv -LiteralPath $CreditsCsv -NoTypeInformation -Encoding utf8BOM }
Get-Item -LiteralPath $outputPath | Select-Object FullName
