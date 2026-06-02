This will be where devin.ai does it's thing.

# setlist-bridge

Spring Boot (Kotlin) service that will turn setlist.fm setlists into Spotify playlists.

## Stack

- Spring Boot 4.0.6 (Spring MVC, via `spring-boot-starter-webmvc`)
- Kotlin 2.3.0
- Java 25 (current LTS)
- Gradle (Kotlin DSL) with the Gradle wrapper

## Toolchain

Java is managed with [`asdf`](https://asdf-vm.com/) — see [`.tool-versions`](.tool-versions).

```bash
asdf install
```

Gradle resolves the JDK via its Java toolchain support, so make sure an OpenJDK 25
build is available on your machine (asdf-installed JDKs are auto-detected).

## Build & test

```bash
./gradlew build      # compile + run tests
./gradlew test       # tests only (includes the Spring context smoke test)
./gradlew bootRun    # run the application
```

`SetlistBridgeApplicationTests.contextLoads()` verifies that the Spring
application context starts up correctly.
