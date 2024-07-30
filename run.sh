#!/usr/bin/env bash

# Specify the correct PATH delimiter based on platform.
WHERE="$(uname -s)"
DELIM=":"

case "$(uname -s)" in
  CYGWIN*|MINGW*|MINGW32*|MSYS*)
    DELIM=";"
  ;;
esac


if javac -d out/production/sqc-bigram-learn/ src/edu/cvtc/bigram/Main.java; then
  java -cp "lib/*${DELIM}out/production/sqc-bigram-learn" edu.cvtc.bigram.Main "$@"
fi

