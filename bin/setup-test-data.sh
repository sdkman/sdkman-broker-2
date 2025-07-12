#!/usr/bin/env bash

# SDKMAN Broker Test Data Setup Script
# Sets up MongoDB fixtures for version download testing

set -e

# MongoDB connection details
MONGO_HOST=${MONGO_HOST:-"localhost"}
MONGO_PORT=${MONGO_PORT:-"27017"}
MONGO_DB=${MONGO_DB:-"sdkman"}

echo "Setting up test data in MongoDB at $MONGO_HOST:$MONGO_PORT/$MONGO_DB"

# Clear existing data
mongo --host "$MONGO_HOST:$MONGO_PORT" "$MONGO_DB" --eval "
  db.versions.deleteMany({});
  db.application.deleteMany({});
  print('Cleared existing test data');
"

# Insert version data for java 24.0.1-tem (as expected by test script)
mongo --host "$MONGO_HOST:$MONGO_PORT" "$MONGO_DB" --eval "
  db.versions.insertMany([
    {
      'candidate': 'java',
      'version': '24.0.1-tem',
      'platform': 'LINUX_64',
      'url': 'https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24.0.1%2B9/OpenJDK24U-jdk_x64_linux_hotspot_24.0.1_9.tar.gz',
      'vendor': 'tem',
      'visible': true
    },
    {
      'candidate': 'java',
      'version': '24.0.1-tem',
      'platform': 'LINUX_ARM64',
      'url': 'https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24.0.1%2B9/OpenJDK24U-jdk_aarch64_linux_hotspot_24.0.1_9.tar.gz',
      'vendor': 'tem',
      'visible': true
    },
    {
      'candidate': 'java',
      'version': '24.0.1-tem',
      'platform': 'MAC_OSX',
      'url': 'https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24.0.1%2B9/OpenJDK24U-jdk_x64_mac_hotspot_24.0.1_9.tar.gz',
      'vendor': 'tem',
      'visible': true
    },
    {
      'candidate': 'java',
      'version': '24.0.1-tem',
      'platform': 'MAC_ARM64',
      'url': 'https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24.0.1%2B9/OpenJDK24U-jdk_aarch64_mac_hotspot_24.0.1_9.tar.gz',
      'vendor': 'tem',
      'visible': true
    },
    {
      'candidate': 'java',
      'version': '24.0.1-tem',
      'platform': 'WINDOWS_64',
      'url': 'https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24.0.1%2B9/OpenJDK24U-jdk_x64_windows_hotspot_24.0.1_9.zip',
      'vendor': 'tem',
      'visible': true
    }
  ]);
  print('Inserted java 24.0.1-tem version data for all platforms');
"

# Insert application record for health checks
mongo --host "$MONGO_HOST:$MONGO_PORT" "$MONGO_DB" --eval "
  db.application.insertOne({
    'alive': 'OK',
    'stableCliVersion': '5.19.0',
    'betaCliVersion': 'latest+b8d230b',
    'stableNativeCliVersion': '0.7.4',
    'betaNativeCliVersion': '0.7.4'
  });
  print('Inserted application record');
"

echo "Test data setup completed successfully!"
echo ""
echo "Verify the data with:"
echo "  mongo --host $MONGO_HOST:$MONGO_PORT $MONGO_DB --eval \"db.versions.find({candidate: 'java', version: '24.0.1-tem'}).pretty()\""
echo "  mongo --host $MONGO_HOST:$MONGO_PORT $MONGO_DB --eval \"db.application.find().pretty()\""