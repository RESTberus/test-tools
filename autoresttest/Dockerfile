# AutoRestTest (selab-gatech/AutoRestTest), packaged for restberus. Debian slim (not alpine)
# so numpy/scipy/scikit-learn/gensim resolve to manylinux wheels on both linux/amd64 and
# linux/arm64. Dependencies are managed with uv + uv.lock.
FROM python:3.10-slim AS builder

COPY --from=ghcr.io/astral-sh/uv:0.11.24 /uv /uvx /bin/

ENV UV_LINK_MODE=copy \
    UV_PROJECT_ENVIRONMENT=/tool/.venv \
    UV_PYTHON_DOWNLOADS=never \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /tool

# Build tools in case a transitive dependency has no wheel for the target platform.
RUN apt-get update && apt-get install -y --no-install-recommends gcc g++ && rm -rf /var/lib/apt/lists/*

# Install the locked dependencies only (package = false -> no project build), so this layer
# is cached unless the lock changes.
COPY pyproject.toml uv.lock .python-version ./
RUN uv sync --frozen --no-dev

# Runtime stage
FROM python:3.10-slim AS runtime

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    VIRTUAL_ENV=/tool/.venv \
    PATH="/tool/.venv/bin:$PATH" \
    PYTHONPATH=/tool/src

WORKDIR /tool

COPY . /tool/
COPY --from=builder /tool/.venv /tool/.venv

CMD ["python3", "-m", "autoresttest.autoresttest"]
