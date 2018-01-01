FROM twashing/edgarly-app-base:latest
MAINTAINER Timothy Washington


ENTRYPOINT [ "lein" , "with-profile" , "app" , "run" , "-m" , "com.interrupt.edgarly.core/-main" ]