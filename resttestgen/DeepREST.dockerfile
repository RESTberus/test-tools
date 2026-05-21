# Stage 1: Build Java with Gradle
FROM gradle:8.14-jdk17 AS java-builder

WORKDIR /app

COPY build.gradle* ./
COPY src/ ./src/

RUN gradle clean build -x test

# Stage 2: Python dependencies
FROM ghcr.io/restberus/python:3.14.5-debian-13.5-slim AS python-builder

WORKDIR /app

# Copy requirements first to leverage Docker cache
COPY ./src/main/python/deeprest/requirements.txt ./

RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# Pre-install torch from the CPU-only index to prevent downloading NVIDIA CUDA libraries
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu
RUN pip install --no-cache-dir -r requirements.txt

# Stage 3: Runtime (Stable Baslines 3 Python + RestTestGen Java)
FROM ghcr.io/restberus/python:3.14.5-debian-13.5-slim-jre17 AS runtime

WORKDIR /tool

# Copy built JAR from build stage
COPY --from=java-builder /app/build/libs/*-all.jar ./resttestgen.jar

# Copy venv from python-builder stage
COPY --from=python-builder /opt/venv /opt/venv

# Set environment variables to use the venv
ENV PATH="/opt/venv/bin:$PATH" \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

# Copy Python source from host
COPY ./src/main/python/deeprest /tool

RUN chmod +x entrypoint.sh

RUN mkfifo /tool/j2p /tool/p2j

ENTRYPOINT ["./entrypoint.sh"]