# Local Consumption Guide

Use this guide when another Maven project on the same machine needs to consume this SDK before it is published to a remote repository.

## 1. Install The SDK Locally

From this repository:

```bash
.\mvnw.cmd clean install
```

On Windows:

- Install a JDK that provides Java 17 or newer.
- Either set `JAVA_HOME` correctly or ensure `java` is available on `PATH`.
- Keep the hidden `.mvn\wrapper` directory when copying the repository. If `maven-wrapper.jar` is missing, `mvnw.cmd` now attempts to download it from Maven Central.

On Linux or macOS:

```bash
./mvnw clean install
```

- Install a JDK that provides Java 17 or newer.
- Either set `JAVA_HOME` correctly or ensure `java` is available on `PATH`.
- Keep the hidden `.mvn/wrapper` directory when copying the repository. If `maven-wrapper.jar` is missing, `mvnw` now attempts to download it from Maven Central.
- Ensure `curl` or `wget` is available if the wrapper JAR must be downloaded.

This compiles the reactor, runs tests, packages both modules, and installs them into the local Maven repository.

Installed artifacts:

- parent/aggregator: `com.example.dppsdk:dpp-datamodel:0.3.0`
- reusable core module: `com.example.dppsdk:dpp-core:0.3.0`
- furniture-specific module: `com.example.dppsdk:dpp4fun:0.3.0`

On Windows, the local Maven repository is usually:

```text
%USERPROFILE%\.m2\repository
```

Typical installed module paths:

```text
%USERPROFILE%\.m2\repository\com\example\dppsdk\dpp-core\0.3.0
%USERPROFILE%\.m2\repository\com\example\dppsdk\dpp4fun\0.3.0
```

## 2. Add The Main Dependency

Most consumers should depend on `dpp4fun`, which already pulls in `dpp-core`:

```xml
<dependency>
    <groupId>com.example.dppsdk</groupId>
    <artifactId>dpp4fun</artifactId>
    <version>0.3.0</version>
</dependency>
```

If a project only needs the reusable core types, it can depend on `dpp-core` directly:

```xml
<dependency>
    <groupId>com.example.dppsdk</groupId>
    <artifactId>dpp-core</artifactId>
    <version>0.3.0</version>
</dependency>
```

## 3. Optional Test Fixtures

If the consuming project's tests need module test fixtures, add the module test JAR.

For `dpp4fun` fixtures:

```xml
<dependency>
    <groupId>com.example.dppsdk</groupId>
    <artifactId>dpp4fun</artifactId>
    <version>0.3.0</version>
    <classifier>tests</classifier>
    <scope>test</scope>
</dependency>
```

For `dpp-core` fixtures:

```xml
<dependency>
    <groupId>com.example.dppsdk</groupId>
    <artifactId>dpp-core</artifactId>
    <version>0.3.0</version>
    <classifier>tests</classifier>
    <scope>test</scope>
</dependency>
```

## 4. Use The SDK

For construction, validation, mapping, and JSON examples, see `SDK_USAGE.md`.
