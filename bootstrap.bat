@if "%DEBUG%" == "" @echo off
@rem Install kotlin-logging
mkdir kotlin-logging
cd kotlin-logging
git init
git remote add origin https://github.com/MicroUtils/kotlin-logging.git
git fetch origin 7af97dbd4c79a3bf92cb4eacd4ca8abc0a6e217c
git reset --hard FETCH_HEAD
gradlew.bat publishToMavenLocal -Dorg.gradle.jvmargs=-XX:MaxMetaspaceSize=512m --no-daemon --stacktrace
cd ..
