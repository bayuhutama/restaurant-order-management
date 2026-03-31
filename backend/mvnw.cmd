@echo off
setlocal

set JAVA_HOME=C:\Program Files\Java\jdk-17.0.17

set MAVEN_VERSION=3.9.6
set MAVEN_HOME=%USERPROFILE%\.mvn-wrapper\apache-maven-%MAVEN_VERSION%

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo [mvnw] Apache Maven %MAVEN_VERSION% not found. Downloading...
    set "ZIP=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip"
    powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip'"
    if not exist "%USERPROFILE%\.mvn-wrapper" mkdir "%USERPROFILE%\.mvn-wrapper"
    powershell -NoProfile -Command "Expand-Archive -Path '%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip' -DestinationPath '%USERPROFILE%\.mvn-wrapper' -Force"
    del "%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip"
    echo [mvnw] Maven downloaded to %MAVEN_HOME%
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
