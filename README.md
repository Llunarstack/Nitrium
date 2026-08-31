# Nitrium

A Fabric performance mod for Minecraft **1.21.11**.

## Requirements

- Java 21+
- Fabric Loader 0.19.3+
- Fabric API

## Setup

```bash
./gradlew build
```

## Run in development

```bash
./gradlew runClient
./gradlew runServer
```

## Project layout

```
src/main/java/dev/nitrium/          # Common/server-side optimizations
src/client/java/dev/nitrium/client/ # Client-side optimizations
```
