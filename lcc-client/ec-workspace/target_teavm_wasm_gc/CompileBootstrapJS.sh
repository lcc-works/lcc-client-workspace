#!/bin/sh
# Convenience wrapper. The bootstrap compile is now a Gradle task
# (compileMainWasmBootstrap) and runs automatically as part of
# makeMainWasmClientBundle, so you do not need to run this first.
cd ../
chmod +x gradlew
./gradlew target_teavm_wasm_gc:compileMainWasmBootstrap
