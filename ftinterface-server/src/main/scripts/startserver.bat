@echo off

set JAVA=%JAVA_HOME%\bin\java

rem up to 12 params
set ONE=%~1
set TWO=%~2
set THREE=%~3
set FOUR=%~4
set FIVE=%~5
set SIX=%~6
set SEVEN=%~7
set EIGHT=%~8
set NINE=%~9
shift
shift
shift
shift
shift
shift
shift
shift
shift
set TEN=%~1
set ELEVEN=%~2
set TWELVE=%~3

%JAVA% -Dconfig=./Application.properties -Djava.util.logging.config.file=./logging.properties -jar ./lib/ftinterface-server-${project.version}.jar %ONE% %TWO% %THREE% %FOUR% %FIVE% %SIX%

