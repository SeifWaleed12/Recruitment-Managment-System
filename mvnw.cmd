@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set MAVEN_PROJECTBASEDIR=%DIRNAME%

set MAVEN_WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"

set JAVA_HOME=C:\Users\Seif Waleed\.jdks\ms-21.0.12
set JAVACMD=%JAVA_HOME%\bin\java.exe

if not exist %JAVACMD% (
  set JAVACMD=java
)

%JAVACMD% -jar %MAVEN_WRAPPER_JAR% %*
