FROM twashing/edgarly-app-base:latest


MAINTAINER Timothy Washington

COPY . /app

ENTRYPOINT ["lein", "trampoline", "run", "--config", "dev-resources/config.conf"]