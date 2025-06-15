#!/usr/bin/env bash

# SDKMAN Broker Version Download Test Script

# test data:
# db.versions.find({candidate: "java", version: "24.0.1-tem"});
# { "_id" : ObjectId("68011c4fa25363da94f8391c"), "candidate" : "java", "version" : "24.0.1-tem", "platform" : "LINUX_64", "url" : "https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24.0.1%2B9/OpenJDK24U-jdk_x64_linux_hotspot_24.0.1_9.tar.gz", "vendor" : "tem", "visible" : true }
# { "_id" : ObjectId("68011c50a25363da94f8391e"), "candidate" : "java", "version" : "24.0.1-tem", "platform" : "LINUX_ARM64", "url" : "https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24.0.1%2B9/OpenJDK24U-jdk_aarch64_linux_hotspot_24.0.1_9.tar.gz", "vendor" : "tem", "visible" : true }
# { "_id" : ObjectId("68011c50a25363da94f83920"), "candidate" : "java", "version" : "24.0.1-tem", "platform" : "MAC_OSX", "url" : "https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24.0.1%2B9/OpenJDK24U-jdk_x64_mac_hotspot_24.0.1_9.tar.gz", "vendor" : "tem", "visible" : true }
# { "_id" : ObjectId("6807b3f1a25363da94f83953"), "candidate" : "java", "version" : "24.0.1-tem", "platform" : "WINDOWS_64", "url" : "https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24.0.1%2B9/OpenJDK24U-jdk_x64_windows_hotspot_24.0.1_9.zip", "vendor" : "tem", "visible" : true }
# { "_id" : ObjectId("68066244a25363da94f8394b"), "candidate" : "java", "version" : "24.0.1-tem", "platform" : "MAC_ARM64", "url" : "https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24.0.1%2B9/OpenJDK24U-jdk_aarch64_mac_hotspot_24.0.1_9.tar.gz", "vendor" : "tem", "visible" : true }

set -e

# Default base URL if not provided
# BASE_URL=${1:-"https://api.sdkman.io/2/broker"}
BASE_URL=${1:-"https://broker.sdkman.io"}

# Helper function to run test and check status code
run_test() {
    local test_name="$1"
    local expected_status="$2"
    local url="$3"

    local http_cmd="http --print=h --ignore-stdin"
    local actual_status=$($http_cmd GET "$url" 2>/dev/null | head -n1 | cut -d' ' -f2)

    if [ "$actual_status" = "$expected_status" ]; then
        echo "PASS: $test_name"
    else
        echo "FAIL: $test_name (expected $expected_status, got $actual_status)"
    fi
}

# Test 1: Redirect to platform-specific binary when exact match exists (MAC_ARM64)
run_test "Platform-specific binary redirect (MAC_ARM64)" "302" "$BASE_URL/download/java/24.0.1-tem/darwinarm64"

# Test 2: Redirect to platform-specific binary when exact match exists (MAC_OSX)
run_test "Platform-specific binary redirect (MAC_OSX)" "302" "$BASE_URL/download/java/24.0.1-tem/darwinx64"

# Test 3: Redirect to platform-specific binary when exact match exists (LINUX_64)
run_test "Platform-specific binary redirect (LINUX_64)" "302" "$BASE_URL/download/java/24.0.1-tem/linuxx64"

# Test 4: Redirect to platform-specific binary when exact match exists (LINUX_ARM64)
run_test "Platform-specific binary redirect (LINUX_ARM64)" "302" "$BASE_URL/download/java/24.0.1-tem/linuxarm64"

# Test 5: Redirect to platform-specific binary when exact match exists (WINDOWS_64)
run_test "Platform-specific binary redirect (WINDOWS_64)" "302" "$BASE_URL/download/java/24.0.1-tem/windowsx64"

# Test 6: Return 400 for invalid platform
run_test "Invalid platform" "400" "$BASE_URL/download/java/24.0.1-tem/invalidplatform"

# Test 7: Return 404 when candidate not found
run_test "Non-existent candidate" "404" "$BASE_URL/download/nonexistent/1.0.0/linuxx64"

# Test 8: Return 404 when version not found
run_test "Non-existent version" "404" "$BASE_URL/download/java/99.0.0/linuxx64"

# Test 9: Return 404 when platform not found and no UNIVERSAL fallback
run_test "No platform match, no UNIVERSAL fallback" "404" "$BASE_URL/download/java/24.0.1-tem/linuxx32"

