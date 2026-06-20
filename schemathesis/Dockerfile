FROM ghcr.io/restberus/python:3.14.5-debian-13.5-slim

LABEL Name=Schemathesis

WORKDIR /app

RUN groupadd --gid 1000 --system schemathesis && \
    useradd --uid 1000 --system schemathesis -g schemathesis -s /sbin/nologin

COPY --chown=1000:1000 pyproject.toml README.md src ./

RUN pip install --upgrade pip && \
    pip install --no-cache-dir ./

RUN chown -R 1000:1000 /app

USER schemathesis

ENV SCHEMATHESIS_DOCKER_IMAGE=3.14.5-debian-13.5-slim
ENV PYTHON_GIL=0

ENTRYPOINT ["schemathesis"]
