$sbomPaths = @(
    "C:\development\WorkTime2Track\shared\build\generated\compose\resourceGenerator\assembledResources\jvmMain\composeResources\worktime2track.shared.generated.resources\files\sbom.json"
)

$matches = foreach ($sbomPath in $sbomPaths)
{
    if (-not (Test-Path -LiteralPath $sbomPath -PathType Leaf))
    {
        Write-Host "Path $( $sbomPath ) not found"
        continue
    }

    try
    {
        $sbom = Get-Content -LiteralPath $sbomPath -Raw | ConvertFrom-Json -ErrorAction Stop
    }
    catch
    {
        throw "Unable to parse SBOM '$sbomPath': $( $_.Exception.Message )"
    }

    foreach ($component in @($sbom.components))
    {
        $licenses = @($component.licenses | Where-Object { $null -ne $_ })
        if ($licenses.Count -le 1)
        {
            continue
        }

        $declaredLicenses = $licenses | ForEach-Object {
            if ($_.license.id)
            {
                $_.license.id
            }
            elseif ($_.license.name)
            {
                $_.license.name
            }
            elseif ($_.expression)
            {
                $_.expression
            }
        }

        [pscustomobject]@{
            Name = $component.name
            Version = $component.version
            Licenses = $declaredLicenses -join ", "
        }
    }
}

$matches | Format-Table -Property Name, Version, Licenses -AutoSize