FROM ghcr.io/restberus/python:3.14.5-alpine-3.23 AS builder

COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

ENV PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_DEFAULT_TIMEOUT=100 \
    UV_LINK_MODE=copy

WORKDIR /app

RUN apk add --no-cache \
        gcc \
        g++ \
        musl-dev \
        libffi-dev \
        openssl-dev \
        cargo \
        rust

RUN uv venv /app/.venv
ENV VIRTUAL_ENV=/app/.venv \
    PATH="/app/.venv/bin:$PATH" \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# Install dependencies
COPY requirements.txt ./
RUN uv pip install --no-cache-dir -r requirements.txt

# Runtime stage
FROM ghcr.io/restberus/python:3.14.5-alpine-3.23 AS runner

WORKDIR /app

COPY --from=builder /app/.venv /app/.venv
COPY src/arat-rl.py src/verify_setup.py ./

ENV PATH="/app/.venv/bin:$PATH" \
    VIRTUAL_ENV=/app/.venv \
    PYTHON_GIL=0 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

ENTRYPOINT ["python", "arat-rl.py"]
