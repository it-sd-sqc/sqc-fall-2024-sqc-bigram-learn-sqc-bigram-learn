#!/usr/bin/env bash

# Specify the correct PATH delimiter based on platform.
WHERE="$(uname -s)"
DELIM=":"

case "$(uname -s)" in
  CYGWIN*|MINGW*|MINGW32*|MSYS*)
    DELIM=";"
  ;;
esac

jshell --class-path "lib/*${DELIM}out/production/sqc-bigram-learn"
