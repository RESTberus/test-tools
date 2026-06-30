FROM isagroup/restgym-restest:latest

WORKDIR /tool

# config.sh/run.sh are the versioned tool logic; the base image supplies the rest of /tool.
COPY config.sh run.sh /tool/
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh /tool/config.sh /tool/run.sh

ENTRYPOINT ["/entrypoint.sh"]
