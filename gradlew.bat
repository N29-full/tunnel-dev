@ECHO OFF
SET DIR=%~dp0
SET WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar
IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO gradle-wrapper.jar missing; please run Gradle once on your machine or import in IntelliJ.
  EXIT /B 1
)
java -cp "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
