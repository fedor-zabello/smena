#!/bin/bash
set -a
[ -f .env ] && source .env
set +a
exec ./gradlew run "$@"
