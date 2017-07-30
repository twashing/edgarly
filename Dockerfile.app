FROM twashing/ibgateway-app-base:latest
MAINTAINER Timothy Washington

VOLUME /root/.m2/repository

ENTRYPOINT [ "lein" , "with-profile" , "+app" , "run" , "-m" , "com.interrupt.ibgateway.core/-main" ]