# CCA Forum Project Overview

This file is the project-specific README.
The original class instructions remain in `README.md`.

## Project Description

CCA Forum is a Java Swing desktop app that lets users:

- create accounts and log in securely
- browse categories and threads
- create posts and comments
- view post details and discussion replies
- optionally use AI-assisted reply generation (configurable)

The app uses a MySQL-compatible database (TiDB) for persistent storage.

## Tech Stack

- Java 17
- Java Swing (desktop UI)
- Maven
- MySQL Connector/J
- jBCrypt (password hashing)

## Run Locally

1. Copy and configure properties:

   - duplicate `forum.properties.example` as `forum.properties`
   - fill `db.url`, `db.user`, and `db.password`

2. Build:

   - `mvn clean package`

3. Run:

   - `java -jar target/forum-app.jar`

> Run from the project root so `forum.properties` is found.

## Security / What Is Committed

For security, this repository does **not** commit generated app binaries or real secrets.

- Do not commit built `.jar` / `.exe` files (build artifacts stay local).
- Do not commit `forum.properties` with real credentials.
- Commit only source code/config templates, including `forum.properties.example`.

## Optional AI Settings

AI is off by default. To enable:

- set `ai.enabled=true`
- provide `ai.api_key`
- confirm provider/model fields in `forum.properties`

## Packaging (Windows)

You can package an `.exe` installer with:

- `scripts/package-exe.bat`

This requires:

- Maven on PATH
- `JAVA_HOME` pointing to a JDK that includes `jpackage`

