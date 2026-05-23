FROM python:3.11.15-alpine3.23 AS builder
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

ENV PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_DEFAULT_TIMEOUT=100 \
    UV_LINK_MODE=copy \
    UV_PROJECT_ENVIRONMENT=/app/.venv

# Change workdir
WORKDIR /app

# Install dependencies and compile into bytecode
RUN --mount=type=cache,target=/root/.cache/uv \
    --mount=type=bind,source=uv.lock,target=uv.lock \
    --mount=type=bind,source=pyproject.toml,target=pyproject.toml \
    --mount=type=bind,source=.python-version,target=.python-version \
    uv sync --frozen --no-install-project --no-dev --compile-bytecode


FROM python:3.11.15-alpine3.23 AS runner

# Set workdir
WORKDIR /app

# Copy the virtual env from the builder
COPY --from=builder /app/.venv /app/.venv

# Copy the project files
ADD . /app

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PATH="/app/.venv/bin:$PATH"

ENV TARGET_URL=http://localhost:8000 \
    TIME_BUDGET_SECONDS=600 \
    PYTHON_GIL=0 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    YAML_PATH=/app/api_spec.yaml

CMD ["python", "main.py", "--target-url", "${TARGET_URL}", "--time-budget-seconds", "${TIME_BUDGET_SECONDS}", "--yaml-path", "${YAML_PATH}"]