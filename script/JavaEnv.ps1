
function JavaEnv
{
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        # NOTE:
        # JavaEnv.bash の _JAVA_ENV_CANDIDATES と JavaEnv.ps1 の ValidateSet の内容を揃えること
        [ValidateSet("liberica:25", "temurin:25", "liberica:21", "temurin:21")]
        [string]$Jvm
    )

    $JAVA_HOME = (cs java-home --jvm $Jvm).Trim()

    if (![string]::IsNullOrWhiteSpace($JAVA_HOME)) {
        $global:_JAVA_ENV_JVM = $Jvm
        $env:JAVA_HOME = $JAVA_HOME

        Set-Clipboard -Value $env:JAVA_HOME
        Write-Output "JAVA_HOME copied to clipboard!"
    }
}

if (-not $global:_JAVA_ENV_ORIGINAL_PROMPT) {
    $global:_JAVA_ENV_ORIGINAL_PROMPT = (Get-Command prompt).ScriptBlock
}

function global:prompt {
    [CmdletBinding()]

    $Prefix = ""
    if ($global:_JAVA_ENV_JVM) {
        $Prefix = "[" + $global:_JAVA_ENV_JVM + "] "
    }

    return $Prefix + (& $global:_JAVA_ENV_ORIGINAL_PROMPT)
}
