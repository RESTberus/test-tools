# Stage 1: build the patched RESTest CLI. src/ is isa-group/RESTest with RESTestRandom
# wired into every random source, so RANDOM_SEED makes a run reproducible (see README.md).
# Bytecode is arch-independent, so build once on the native platform.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-11 AS builder

WORKDIR /build

# Warm the dependency cache in its own layer; `package` re-resolves whatever is missing.
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline || true

COPY src ./src
RUN mvn -B -DskipTests package

# Stage 2: the vendor image supplies the rest of /tool (FT/RT/RT-LLM instances, the
# RT-LLM model and its Python deps); the CLI jar and the tool logic are replaced.
FROM isagroup/restgym-restest:latest

WORKDIR /tool

COPY --from=builder /build/target/restest-cli.jar /tool/restest-cli.jar

# config.sh/run.sh are the versioned tool logic; the base image supplies the rest of /tool.
COPY config.sh run.sh /tool/
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh /tool/config.sh /tool/run.sh

ENTRYPOINT ["/entrypoint.sh"]
