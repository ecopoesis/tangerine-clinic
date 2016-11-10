#!/bin/bash

export TANGERINE_CLINIC_FILE_ENV="$1"

./target/universal/stage/bin/tangerine-clinic -Dplay.crypto.secret=s3krit -J-Xms8g -J-Xmx8g