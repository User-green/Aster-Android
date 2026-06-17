#!/usr/bin/env bash
#
# Aster Communications Inc.
#
# Copyright (c) 2026 Aster Communications Inc.
#
# This file is part of this project.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
crate_dir="$(dirname "$script_dir")"
repo_root="$(cd "$crate_dir/../.." && pwd)"
android_jni_dir="${ANDROID_JNI_DIR:-$repo_root/core-crypto/src/main/jniLibs}"

cd "$crate_dir"

if ! command -v cargo-ndk >/dev/null 2>&1; then
    echo "cargo-ndk is required. Install with: cargo install cargo-ndk --locked" >&2
    exit 1
fi

mkdir -p "$android_jni_dir"

cargo ndk \
    -t arm64-v8a \
    -t armeabi-v7a \
    -t x86_64 \
    -o "$android_jni_dir" \
    build --release --locked

echo "built libaster_crypto_ffi.so into $android_jni_dir"
