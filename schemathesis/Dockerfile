# venv setup
FROM ghcr.io/restberus/python:3.14.5-alpine-3.23 AS app-builder

COPY --from=ghcr.io/astral-sh/uv:0.11.15 /uv /uvx /bin/

RUN apk add --no-cache \
        gcc \
        g++ \
        musl-dev \
        libffi-dev \
        openssl-dev \
        cargo \
        rust

WORKDIR /app

COPY pyproject.toml README.md .python-version ./

RUN uv venv /app/.venv
ENV VIRTUAL_ENV=/app/.venv \
    PATH="/app/.venv/bin:$PATH" \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_DEFAULT_TIMEOUT=100 \
    UV_LINK_MODE=copy \
    UV_PROJECT_ENVIRONMENT=/app/.venv

RUN mkdir -p src/schemathesis && \
    touch src/schemathesis/__init__.py

RUN uv pip install --no-cache-dir tracecov==0.20.1

COPY src ./src

RUN uv pip install --no-cache-dir --no-deps --force-reinstall ./

# Runtime
FROM ghcr.io/restberus/python:3.14.5-alpine-3.23 AS runtime

WORKDIR /app

COPY --from=app-builder /app/.venv /app/.venv

RUN apk add --no-cache ca-certificates libgcc libstdc++ sqlite-libs

RUN addgroup --gid 1000 -S schemathesis && \
    adduser --uid 1000 -D -S schemathesis -G schemathesis -s /sbin/nologin

COPY --chown=1000:1000 pyproject.toml README.md src ./

RUN printf 'import os\nif os.environ.get("SCHEMATHESIS_COVERAGE", "true").lower() != "false":\n    import tracecov\n    tracecov.schemathesis.install()\n' > /app/hooks.py && \
    chown -R 1000:1000 /app

USER schemathesis

ENV PATH="/app/.venv/bin:$PATH" \
    VIRTUAL_ENV=/app/.venv \
    PYTHON_GIL=0 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

ENV SCHEMATHESIS_COVERAGE=true \ 
    SCHEMATHESIS_HOOKS=/app/hooks.py

ENTRYPOINT ["schemathesis"]
