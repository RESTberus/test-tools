# Original Morest ("api-fuzzer-hw") algorithm, modernized to Python 3.11 and deterministically
# seeded, packaged for restberus. Debian slim (not alpine) so the deps resolve to manylinux
# wheels on both linux/amd64 and linux/arm64. Dependencies are managed with uv + uv.lock.
FROM python:3.9-slim AS builder

COPY --from=ghcr.io/astral-sh/uv:0.10.9 /uv /uvx /bin/

ENV UV_LINK_MODE=copy \
    UV_PROJECT_ENVIRONMENT=/tool/.venv \
    UV_PYTHON_DOWNLOADS=never \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /tool

# Install the locked dependencies only (package=false -> no project build), so this layer is
# cached unless the lock changes. pydblite's legacy sdist is built here with setuptools<73
# (pinned via [tool.uv.extra-build-dependencies] in pyproject.toml).
RUN apt-get update && apt-get install -y --no-install-recommends gcc g++ && rm -rf /var/lib/apt/lists/*

COPY pyproject.toml uv.lock .python-version ./
RUN uv sync --frozen --no-dev

# Runtime stage
FROM python:3.9-slim AS runtime

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    VIRTUAL_ENV=/tool/.venv \
    PATH="/tool/.venv/bin:$PATH"

WORKDIR /tool

COPY --from=builder /tool/.venv /tool/.venv
COPY . /tool/

# restberus overrides the container command with `/bin/sh /app/run.sh` (the wrapper); these
# defaults only apply when running the image standalone.
ENV TARGET_URL=http://localhost:8000 \
    TIME_BUDGET_SECONDS=600 \
    RANDOM_SEED=42 \
    SPEC_PATH=/app/spec.json

CMD ["python3", "entrypoint.py"]
