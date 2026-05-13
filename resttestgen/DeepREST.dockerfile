# Stage 1: Build Java with Gradle
FROM gradle:8.14-jdk17 AS java-builder

WORKDIR /app

COPY build.gradle* ./
COPY src/ ./src/

RUN gradle clean build -x test

# Stage 2: Python dependencies
FROM python:3.12-slim-bookworm AS python-builder

WORKDIR /app

# Copy requirements first to leverage Docker cache
COPY ./src/main/python/deeprest/requirements.txt ./

RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# Pre-install torch from the CPU-only index to prevent downloading NVIDIA CUDA libraries
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu
RUN pip install --no-cache-dir -r requirements.txt

# Stage 3: Runtime (Stable Baslines 3 Python + RestTestGen Java)
FROM python:3.12-slim-bookworm AS runtime

WORKDIR /app

# Copy built JAR from build stage
COPY --from=java-builder /app/build/libs/*-all.jar ./rtg.jar

# Copy venv from python-builder stage
COPY --from=python-builder /opt/venv /opt/venv

# Set environment variables to use the venv
ENV PATH="/opt/venv/bin:$PATH"

# Copy Python source from host
COPY ./src/main/python/deeprest /app

RUN apt-get update && apt-get install -y openjdk-17-jre-headless && rm -rf /var/lib/apt/lists/*

RUN chmod +x entrypoint.sh

RUN mkfifo /app/j2p /app/p2j

ENTRYPOINT ["./entrypoint.sh"]