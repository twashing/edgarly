FROM twashing/edgarly-app-base:latest
MAINTAINER Timothy Washington

COPY . /app
# RUN lein deps

ENTRYPOINT [ "lein" , "with-profile" , "+app" , "run" , "-m" , "com.interrupt.edgarly.core/-main" ]