# DeepREST tool image for restberus.
# Vendored from SeUniVr/DeepREST-Docker (upstream /source layout, per-call FIFO
# IPC, DeepReinforcementLearningStrategy) with only the deliberate RANDOM_SEED
# seed patch applied (Python deeprest.py + Java Configuration/Environment/
# ExtendedRandom). Launched the same way as restgym/deeprest-tool: Python (RL
# agent) in the background, Java (RestTestGen) in the foreground.
FROM stablebaselines/rl-baselines3-zoo-cpu

USER root

RUN apt update && apt install -y openjdk-11-jdk && rm -rf /var/lib/apt/lists/*

COPY ./source /source

# Build the RestTestGen framework (shadow all-jar) and pin it to a stable path.
RUN chmod +x /source/resttestgen-framework/gradlew \
 && cd /source/resttestgen-framework \
 && ./gradlew clean build --no-daemon -x test \
 && cp build/libs/*-all.jar resttestgen.jar

# The base image keeps Stable-Baselines3 in its conda env, normally activated by
# the base ENTRYPOINT (/usr/local/bin/_entrypoint.sh). restberus overrides the
# container command (bypassing that ENTRYPOINT), so put the conda interpreter on
# PATH explicitly. Placed after the Gradle build so the Java build keeps using
# the apt-installed JDK.
ENV PATH="/opt/conda/bin:${PATH}"

# Named pipes for the Java<->Python IPC (re-opened per message on both sides).
RUN mkfifo /source/j2p /source/p2j

WORKDIR /source/resttestgen-framework

# Standalone launch, mirroring restgym/deeprest-tool (Python backgrounded, Java
# foreground reading rtg-config.yml). In restberus this CMD is replaced by the
# mounted wrapper (tools/deeprest/wrapper.sh), which stages the spec and runs the
# same Python-background / Java-foreground launch inside a restart loop.
CMD cd /source/deeprest-py && python3 deeprest.py & cd /source/resttestgen-framework && java -jar resttestgen.jar -l WARN
