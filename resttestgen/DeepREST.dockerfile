# Stage 1: Build Java with Gradle
FROM gradle:8.14-jdk17 AS java-builder

WORKDIR /app

COPY build.gradle* ./
COPY src/ ./src/

RUN gradle clean build -x test

# Stage 2: Python dependencies
FROM ghcr.io/restberus/python:3.14.5-debian-13.5-slim AS python-builder

WORKDIR /app

# Install uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

# Copy dependencies definitions
COPY ./src/main/python/deeprest/.python-version ./src/main/python/deeprest/pyproject.toml ./src/main/python/deeprest/uv.lock ./

# Sync dependencies into /opt/venv
ENV UV_PROJECT_ENVIRONMENT=/opt/venv
RUN uv sync --frozen --no-install-project --no-dev

# Stage 3: Runtime (Stable Baslines 3 Python + RestTestGen Java)
FROM ghcr.io/restberus/python:3.14.5-debian-13.5-slim-jre17 AS runtime

ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -XX:+UseContainerSupport"

WORKDIR /tool

# Copy built JAR from build stage
COPY --from=java-builder /app/build/libs/*-all.jar ./resttestgen.jar

# Copy venv from python-builder stage
COPY --from=python-builder /opt/venv /opt/venv

# Set environment variables to use the venv
ENV PATH="/opt/venv/bin:$PATH" \
    PYTHON_GIL=0 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# Copy Python source from host
COPY ./src/main/python/deeprest /tool

# Copy config files
COPY rtg-config.yml ./rtg-config.yml
COPY strategy-config.yml ./strategy-config.yml

RUN chmod +x entrypoint.sh

RUN mkfifo /tool/j2p /tool/p2j

ENTRYPOINT ["./entrypoint.sh"]