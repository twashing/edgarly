FROM pandeiro/lein:latest

COPY . /app

RUN lein deps
ENTRYPOINT ["lein", "trampoline", "run", "--config", "dev-resources/config.conf"]