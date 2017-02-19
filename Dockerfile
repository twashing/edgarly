FROM ubuntu:16.04


COPY install-base.sh /usr/local/bin/
COPY resources/IBControllerStart-paper.sh /root/
COPY resources/IBControllerStart-real.sh /root/

RUN chmod ug+x /usr/local/bin/install-base.sh
# RUN /usr/local/bin/install-base.sh &> install-base.out
