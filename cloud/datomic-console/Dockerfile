FROM java:openjdk-8
MAINTAINER Timothy Washington <twashing@gmail.com>


ENV DATOMIC_HOST=localhost
RUN echo 'alias ll="ls -la"' >> ~/.bashrc
COPY datomic-pro-0.9.5561.62.zip /tmp/datomic-pro-0.9.5561.62.zip

RUN unzip /tmp/datomic-pro-0.9.5561.62.zip && rm /tmp/datomic-pro-0.9.5561.62.zip

WORKDIR datomic-pro-0.9.5561.62


RUN cp config/samples/dev-transactor-template.properties config/dev-transactor.properties

RUN sed "s/host=localhost/host=0.0.0.0/" -i config/dev-transactor.properties

RUN mkdir /data
RUN sed "s/# data-dir=data/data-dir=\/data/" -i config/dev-transactor.properties
VOLUME /data

RUN mkdir /log
RUN sed "s/# log-dir=log/log-dir=\/log/" -i config/dev-transactor.properties
VOLUME /log



ADD start.sh ./
RUN chmod +x start.sh

EXPOSE 8080

ENTRYPOINT ["./start.sh"]

# ENTRYPOINT [ "/bin/bash" ]

