# NOTE:
# JavaEnv.bash の _JAVA_ENV_CANDIDATES と JavaEnv.ps1 の ValidateSet の内容を揃えること
_JAVA_ENV_CANDIDATES=("liberica:25" "temurin:25" "liberica:21" "temurin:21")

function JavaEnv()
{
    local jvm=${1}

    # Git Bash / MinGW 判定：uname が MINGW* で始まる
    if [[ "$(uname -s)" == MINGW* ]]; then
        cs="cs.bat"
    else
        cs="cs"
    fi

    if _java_home=$($cs java-home --jvm ${jvm}); then
        export JAVA_HOME=${_java_home}
        export _JAVA_ENV_JVM=${jvm}

        if which xclip > /dev/null 2>&1; then
            echo $JAVA_HOME | xclip -selection clipboard
            echo "JAVA_HOME copied to clipboard!"
        fi
    fi
}

function _JavaEnv_completion()
{
    # 補間対象の文字列に ':' が含まれるため
    # COMP* の単語区切りから除外
    COMP_WORDBREAKS="${COMP_WORDBREAKS//:}"

    if [[ ${COMP_CWORD} -eq 1 ]]; then
        COMPREPLY=( $(compgen -W "${_JAVA_ENV_CANDIDATES[*]}" -- "${COMP_WORDS[COMP_CWORD]}") )
    else
        COMPREPLY=()
    fi
}

complete -F _JavaEnv_completion JavaEnv

function _JavaEnv_prompt()
{
    if [[ -n ${_JAVA_ENV_JVM} ]]; then
        printf "[${_JAVA_ENV_JVM}] "
    fi
}

export PS1='`_JavaEnv_prompt`'${PS1}
