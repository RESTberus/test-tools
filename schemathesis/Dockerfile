# venv setup
FROM python:3.14.5-alpine3.23 AS app-builder

WORKDIR /app

COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

RUN apk add --no-cache \
        gcc \
        g++ \
        musl-dev \
        libffi-dev \
        openssl-dev \
        cargo \
        rust

RUN uv venv /opt/venv

COPY pyproject.toml README.md ./

RUN mkdir -p src/schemathesis && \
    touch src/schemathesis/__init__.py

ENV UV_PROJECT_ENVIRONMENT=/opt/venv

RUN uv pip install --no-cache-dir ".[allure]" tracecov==0.20.1

COPY src ./src

RUN uv pip install --no-cache-dir --no-deps --force-reinstall ./

RUN find /opt/venv -type f \( -name '*.pyc' -o -name '*.pyo' \) -delete && \
    find /opt/venv -type d -name __pycache__ -delete

# Runtime
FROM python:3.14.5-alpine3.23 AS runtime

LABEL Name=Schemathesis

WORKDIR /app

COPY --from=app-builder /opt/venv /opt/venv

RUN apk add --no-cache ca-certificates libgcc libstdc++ sqlite-libs

RUN addgroup --gid 1000 -S schemathesis && \
    adduser --uid 1000 -D -S schemathesis -G schemathesis -s /sbin/nologin

COPY --chown=1000:1000 pyproject.toml README.md src ./

RUN printf 'import os\nif os.environ.get("SCHEMATHESIS_COVERAGE", "true").lower() != "false":\n    import tracecov\n    tracecov.schemathesis.install()\n' > /app/hooks.py && \
    chown -R 1000:1000 /app

USER schemathesis

ENV PATH="/opt/venv/bin:$PATH"
ENV VIRTUAL_ENV=/opt/venv

ENV PYTHON_GIL=0
ENV SCHEMATHESIS_COVERAGE=true
ENV SCHEMATHESIS_DOCKER_IMAGE=3.14t-alpine
ENV SCHEMATHESIS_HOOKS=/app/hooks.py

ENTRYPOINT ["schemathesis"]
