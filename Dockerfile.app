FROM twashing/ibgateway-app-base:latest
MAINTAINER Timothy Washington


# ENTRYPOINT [ "lein" , "with-profile" , "+app" , "run" , "-m" , "com.interrupt.ibgateway.core/-main" ]