@echo off
setlocal
if defined JAVA_HOME (
  set "JAVACMD=%JAVA_HOME%\bin\java"
) else (
  set "JAVACMD=java"
)
set "DIRNAME=%~dp0"
set "CLASSPATH=%DIRNAME%gradle\wrapper\gradle-wrapper.jar;%DIRNAME%gradle\wrapper\gradle-wrapper-shared.jar"
"%JAVACMD%" -cp "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
