param(
    [string]$Source = (Join-Path $PSScriptRoot '../build/KaleidoscopeDoll-Sponsored-Custom-Dolls-26.3.zip'),
    [string]$Output = (Join-Path $PSScriptRoot '../build/KaleidoscopeDoll-Sponsored-Config-26.3.zip'),
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
$creditsDirectory = "${sponsorAssets}custom_dolls/lang"

function Read-JsonEntry($Entry) {
    $reader = [IO.StreamReader]::new($Entry.Open())
    try { return ,($reader.ReadToEnd() | ConvertFrom-Json -AsHashtable -Depth 100 -NoEnumerate) }
    finally { $reader.Dispose() }
}

function Write-JsonEntry($Archive, [string]$Path, $Data) {
    $writer = [IO.StreamWriter]::new($Archive.CreateEntry($Path).Open())
    try { $writer.WriteLine((ConvertTo-Json -InputObject $Data -Depth 100 -Compress)) }
    finally { $writer.Dispose() }
}

function Copy-Entry($Archive, [string]$Path, $Entry) {
    $reader = $Entry.Open()
    try {
        $writer = $Archive.CreateEntry($Path).Open()
        try { $reader.CopyTo($writer) }
        finally { $writer.Dispose() }
    } finally { $reader.Dispose() }
}

function Read-GitFile([string]$Path) {
    $lines = git -C $PSScriptRoot show "${CreditsRef}:$Path"
    if ($LASTEXITCODE -ne 0) { throw "Could not read $Path from upstream commit $CreditsRef." }
    return ($lines -join "`n")
}

function Get-SponsorCredits($OriginalIds, $Ids) {
    $source = Read-GitFile 'src/main/java/com/github/ysbbbbbb/kaleidoscopedoll/event/ModRegisterEvent.java'
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

function Write-SponsorCredits($Archive, $Credits) {
    foreach ($entry in $Credits.GetEnumerator()) {
        Write-JsonEntry $Archive "$creditsDirectory/$($entry.Key).json" $entry.Value
    }
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

if (($RestoreCredits -or $Renumber) -and !$PSBoundParameters.ContainsKey('Source')) { $Source = $Output }
$inputPath = (Resolve-Path -LiteralPath $Source).Path
$outputPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Output)
if ($RestoreCredits -or $Renumber) {
    $temporaryPath = "$outputPath.$([guid]::NewGuid()).tmp"
    try {
        $sourceZip = [IO.Compression.ZipFile]::OpenRead($inputPath)
        try {
            $currentIds = Read-JsonEntry $sourceZip.GetEntry('custom_dolls.json')
            $ids = if ($Renumber) { 1..$currentIds.Count | ForEach-Object { "java:${sponsorNamespace}:doll_$_" } } else { $currentIds }
            $items = [ordered]@{}
            $originalIds = @(
                for ($i = 0; $i -lt $currentIds.Count; $i++) {
                    if ($currentIds[$i] -cnotmatch "^java:${sponsorNamespace}:(doll_\d+)$") {
                        throw "Invalid sponsor model ID: $($currentIds[$i])"
                    }
                    $entry = $sourceZip.GetEntry("${sponsorAssets}items/$($Matches[1]).json")
                    $item = Read-JsonEntry $entry
                    if ($item.model.model -cnotmatch ('^' + [regex]::Escape($sponsorReference) + '(doll_\d+)$')) {
                        throw "Unexpected item model: $($currentIds[$i])"
                    }
                    # Internal model paths retain the upstream numbers for future credit restoration.
                    "java:${sponsorNamespace}:$($Matches[1])"
                    $items.Add(("${sponsorAssets}items/" + ($ids[$i] -split ':')[-1] + '.json'), $entry)
                }
            )
            $credits = Get-SponsorCredits $originalIds $ids
            $outputZip = [IO.Compression.ZipFile]::Open($temporaryPath, [IO.Compression.ZipArchiveMode]::Create)
            try {
                foreach ($entry in $sourceZip.Entries) {
                    if ($entry.FullName -cin @('custom_dolls.json', "$creditsDirectory/zh_cn.json", "$creditsDirectory/en_us.json", "$creditsDirectory/ja_jp.json") -or
                        $entry.FullName.StartsWith("${sponsorAssets}items/", [StringComparison]::Ordinal)) {
                        continue
                    }
                    Copy-Entry $outputZip $entry.FullName $entry
                }
                Write-JsonEntry $outputZip 'custom_dolls.json' $ids
                foreach ($entry in $items.GetEnumerator()) { Copy-Entry $outputZip $entry.Key $entry.Value }
                Write-SponsorCredits $outputZip $credits
            } finally { $outputZip.Dispose() }
        } finally { $sourceZip.Dispose() }
        [IO.File]::Move($temporaryPath, $outputPath, $true)
    } finally { [IO.File]::Delete($temporaryPath) }
    if ($CreditsCsv) { Write-CreditsCsv $originalIds $ids $credits | Export-Csv -LiteralPath $CreditsCsv -NoTypeInformation -Encoding utf8BOM }
    Get-Item -LiteralPath $outputPath | Select-Object FullName,Length
    return
}

if ([string]::Equals($inputPath, $outputPath, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'The config output must not overwrite the source pack.'
}
$sourceZip = [IO.Compression.ZipFile]::OpenRead($inputPath)
try {
    $metadata = Read-JsonEntry $sourceZip.GetEntry('pack.mcmeta')
    $metadata.pack.description = 'Kaleidoscope Doll: Sponsored Custom Doll Config (26.3)'
    $originalIds = Read-JsonEntry $sourceZip.GetEntry("data/$sponsorNamespace/custom_dolls.json")
    $ids = @(1..$originalIds.Count | ForEach-Object { "java:${sponsorNamespace}:doll_$_" })
    $credits = Get-SponsorCredits $originalIds $ids

    $models = [ordered]@{}
    $items = [ordered]@{}
    $textures = @($sourceZip.Entries | Where-Object { $_.FullName -match "^${sourceAssets}textures/block/doll/.+\.png(?:\.mcmeta)?$" })
    for ($i = 0; $i -lt $originalIds.Count; $i++) {
        $id = $originalIds[$i]
        if ($id -notmatch "^java:${sponsorNamespace}:(doll_\d+)$") { throw "Invalid sponsor model ID: $id" }
        $doll = $Matches[1]
        $itemPath = "${sponsorAssets}items/$doll.json"
        $item = Read-JsonEntry $sourceZip.GetEntry($itemPath)
        if ($item.model.model -cne "$originalReference$doll") { throw "Unexpected item model: $id" }
        $item.model.model = "$sponsorReference$doll"
        $items.Add(("${sponsorAssets}items/" + ($ids[$i] -split ':')[-1] + '.json'), $item)

        $modelPath = "models/block/doll/$doll.json"
        $model = Read-JsonEntry $sourceZip.GetEntry("$sourceAssets$modelPath")
        foreach ($key in @($model.textures.Keys)) {
            $reference = $model.textures[$key]
            if (!$reference.StartsWith($originalReference, [StringComparison]::Ordinal)) { continue }
            $texturePath = $reference.Substring($reference.IndexOf(':') + 1)
            if ($null -eq $sourceZip.GetEntry("${sourceAssets}textures/$texturePath.png")) {
                throw "Missing source texture $reference for $id"
            }
            $model.textures[$key] = "${sponsorNamespace}:$texturePath"
        }
        $models.Add("$sponsorAssets$modelPath", $model)
    }

    $temporaryPath = "$outputPath.$([guid]::NewGuid()).tmp"
    try {
        $outputZip = [IO.Compression.ZipFile]::Open($temporaryPath, [IO.Compression.ZipArchiveMode]::Create)
        try {
            Write-JsonEntry $outputZip 'pack.mcmeta' $metadata
            Write-JsonEntry $outputZip 'custom_dolls.json' $ids
            foreach ($entry in $items.GetEnumerator()) { Write-JsonEntry $outputZip $entry.Key $entry.Value }
            foreach ($entry in $models.GetEnumerator()) { Write-JsonEntry $outputZip $entry.Key $entry.Value }
            foreach ($entry in $textures) {
                Copy-Entry $outputZip ($sponsorAssets + $entry.FullName.Substring($sourceAssets.Length)) $entry
            }
            Write-SponsorCredits $outputZip $credits
        } finally { $outputZip.Dispose() }
        [IO.File]::Move($temporaryPath, $outputPath, $true)
    } finally { [IO.File]::Delete($temporaryPath) }
} finally { $sourceZip.Dispose() }

if ($CreditsCsv) { Write-CreditsCsv $originalIds $ids $credits | Export-Csv -LiteralPath $CreditsCsv -NoTypeInformation -Encoding utf8BOM }
Get-Item -LiteralPath $outputPath | Select-Object FullName,Length
