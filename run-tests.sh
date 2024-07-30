#!/usr/bin/env bash

# Specify the correct PATH delimiter based on platform.
WHERE="$(uname -s)"
DELIM=":"

case "$(uname -s)" in
  CYGWIN*|MINGW*|MINGW32*|MSYS*)
    DELIM=";"
  ;;
esac


if javac -d out/production/sqc-bigram-learn/ src/edu/cvtc/bigram/Main.java && javac -d out/test/sqc-bigram-learn/ -cp "lib/*${DELIM}out/production/sqc-bigram-learn" test/*.java; then
  java -jar ./lib/junit-platform-console-standalone-1.10.2.jar execute --class-path "lib/slf4j-api-2.0.9.jar${DELIM}lib/slf4j-nop-2.0.9.jar${DELIM}lib/sqlite-jdbc-3.44.1.0.jar${DELIM}out/production/sqc-bigram-learn${DELIM}out/test/sqc-bigram-learn" --scan-classpath --details flat --disable-banner 
fi

