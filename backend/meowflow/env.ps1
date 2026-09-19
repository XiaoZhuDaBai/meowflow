# MeowFlow 构建环境探测（JDK / Maven）
# 用途：被 start-all.ps1 / build.ps1 等脚本 dot-source 复用，避免把 JDK 与 Maven
#       的安装路径硬编码到某台机器上。
#
# 用法：
#   . "$PSScriptRoot\env.ps1"
#   # 之后即可使用 $JAVA_HOME 与 $MVN

$ErrorActionPreference = "Stop"

# JDK：优先环境变量 JAVA_HOME，其次探测常见的 JDK 安装目录。
# 项目要求 JDK 17+（pom 里 source/target 为 17），因此候选里只接受 17 及以上，
# 并在多个版本中挑最新的一个。
function Get-JavaMajorVersion([string]$javaHome) {
    $javaExe = Join-Path $javaHome "bin\java.exe"
    if (-not (Test-Path $javaExe)) { return 0 }
    # java -version 把版本信息写到 stderr。Windows PowerShell 5.1 在
    # $ErrorActionPreference = "Stop" 下会因此抛终止性 RemoteException，
    # 即使外面套 try/catch 也只会拿到异常、拿不到版本号（探测会全部返回 0）。
    # 这里临时把偏好改成 Continue。
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        # 输出形如：openjdk version "21.0.11" 2026-04-21 LTS  /  java version "17.0.19"
        $firstLine = (& $javaExe -version 2>&1 | Select-Object -First 1) -join ''
        if ($firstLine -match '"(\d+)') {
            $major = [int]$matches[1]
            if ($major -eq 1 -and $firstLine -match '"1\.(\d+)') { $major = [int]$matches[1] }  # 1.8 这种老格式
            return $major
        }
    } catch {
        return 0
    } finally {
        $ErrorActionPreference = $previous
    }
    return 0
}

if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $jdkCandidates = @(
        "$env:USERPROFILE\.jdks\*",
        "C:\Program Files\Java\jdk*",
        "C:\Program Files\Eclipse Adoptium\jdk*",
        "C:\Program Files\Microsoft\jdk*"
    )
    $found = $null
    $foundMajor = 0
    foreach ($c in $jdkCandidates) {
        $dirs = Get-Item $c -ErrorAction SilentlyContinue | Where-Object { $_.PSIsContainer }
        foreach ($dir in $dirs) {
            $major = Get-JavaMajorVersion $dir.FullName
            if ($major -ge 17 -and $major -gt $foundMajor) {
                $found = $dir.FullName
                $foundMajor = $major
            }
        }
    }
    if (-not $found) {
        Write-Error "未找到 JDK 17+。请设置 JAVA_HOME 环境变量。"
        exit 1
    }
    $JAVA_HOME = $found
} else {
    $JAVA_HOME = $env:JAVA_HOME
}

# Maven：优先 PATH，其次 MAVEN_HOME，最后探测常见安装位置
$MVN = $null
$mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
if ($mvnCmd) {
    $MVN = $mvnCmd.Source
} elseif ($env:MAVEN_HOME -and (Test-Path (Join-Path $env:MAVEN_HOME "bin\mvn.cmd"))) {
    $MVN = Join-Path $env:MAVEN_HOME "bin\mvn.cmd"
} else {
    $mvnCandidates = @(
        "C:\Program Files\apache-maven*\bin\mvn.cmd",
        "D:\*\apache-maven*\bin\mvn.cmd",
        "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd",
        "C:\Program Files\JetBrains\*\plugins\maven\lib\maven3\bin\mvn.cmd",
        "D:\IntelliJ IDEA*\plugins\maven\lib\maven3\bin\mvn.cmd"
    )
    foreach ($c in $mvnCandidates) {
        $hit = Get-Item $c -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($hit) { $MVN = $hit.FullName; break }
    }
}
if (-not $MVN) {
    Write-Error "未找到 Maven。请将 mvn 加入 PATH，或设置 MAVEN_HOME 环境变量。"
    exit 1
}

# 让子进程（mvn / java）拿到同一个 JDK
$env:JAVA_HOME = $JAVA_HOME
