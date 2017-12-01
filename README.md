# Edgarly

Edgarly microservice for interfacing with the TWS Gateway. From here, the platform makes historical, stock and scanner requests.


## Notes

A) You can connect to TWS, with a VNC viewer (ex: TightVNC).

```
cd ~/Downloads/tvnjviewer-2.8.3-bin-gnugpl/
java -jar tightvnc-jviewer.jar
```

B) You have to do an initial build of base docker images.
```
docker build --no-cache -f Dockerfile.tws.base -t twashing/edgarly-tws-base:latest -t twashing/edgarly-tws-base:`git rev-parse HEAD` .
docker build --no-cache -f Dockerfile.tws -t twashing/edgarly-tws:latest -t twashing/edgarly-tws:`git rev-parse HEAD` .

docker build --no-cache -f Dockerfile.app.base -t twashing/edgarly-app-base:latest -t twashing/edgarly-app-base:`git rev-parse HEAD` .
docker build --no-cache -f Dockerfile.app -t twashing/edgarly-app:latest -t twashing/edgarly-app:`git rev-parse HEAD` .

lein with-profile +app run -m com.interrupt.edgarly.core/-main
```

C) Bringing up docker-compose 
```
# Basic
docker-compose up 

# Force a rebuild of containers
docker-compose up --force-recreate --build
```

D) Running the app solo
```
lein with-profile +app run -m com.interrupt.edgarly.core/-main
```

## Lifecycle functions

- return channels
- expose scanner results in system map
- expose request ids in system map (filter by request type)
- check that system is started
- check that connection is valid

## Onyx Overview

[Onyx](https://github.com/onyx-platform/onyx) is a stream processing, and distributed computing framework. It takes unbounded streaming input, and routes it to different locations, based on rules that you establish.

It has a distributed architecture, which uses a masterless computation system.

It's configured using an information model for the description and construction of distributed workflows. These are the [core primitives](http://www.onyxplatform.org/docs/user-guide/0.10.x/#concepts ) that the Onyx uses.
- Segment - the unit of data flowing through an Onyx cluster. Segments are the only shape of data that Onyx allows you to emit between functions.
- Task - the smallest unit of work in Onyx, representing an activity of either input, processing, or output
- Workflow - the structural specification of an Onyx program
- Catalog - all inputs, outputs, and functions in a workflow must be described via a catalog
- Flow Conditions - specifies on a segment-by-segment basis which direction data should flow determined by predicate functions
- Function
- Lifecycle - something that describes the lifetime of a task
- Windows
- Plugin
- Sentinel
- Peer
- Virtual Peer
- Job - the collection of a workflow, catalog, flow conditions, lifecycles, and execution parameters

You can get a footing of how to setup and use Onyx, using [these examples](http://www.onyxplatform.org/learn/#learn-onyx).
```
$ git clone git@github.com:onyx-platform/onyx-examples.git
$ cd onyx-examples
$ less README.md
```

## Issues

### Can't reach myMavenRepo.read or myMavenRepo.write, from inside a container

When pulling lein dependencies, inside a running container, I get this error.
```
Could not find artifact com.interrupt:edgarly:jar:0.1.2-SNAPSHOT in clojars (https://clojars.org/repo/)
Could not find artifact com.interrupt:edgarly:jar:0.1.2-SNAPSHOT in myMavenRepo.read (https://mymavenrepo.com/repo/HaEY4usKuLXXnqmXBr0z)
Could not find artifact com.interrupt:edgarly:jar:0.1.2-SNAPSHOT in myMavenRepo.write (https://mymavenrepo.com/repo/xc9d5m3WdTIFAqIiiYkn/)
This could be due to a typo in :dependencies or network issues.
If you are behind a proxy, try setting the 'http_proxy' environment variable.
Error encountered performing task 'run' with profile(s): 'base,system,user,provided,dev,app'
```


### For Onyx-kafka, Troubleshooting simple job not writing to a topic
- https://github.com/onyx-platform/onyx-kafka/issues/47


### For scaling n kafka nodes, howto set docker swarm runtime volume size (/dev/shm)?
- https://stackoverflow.com/questions/47401805/howto-set-docker-swarm-runtime-volume-size-dev-shm
- https://stackoverflow.com/questions/46085748/define-size-for-dev-shm-on-container-engine


#### Try these options
- https://github.com/wurstmeister/kafka-docker
- https://hub.docker.com/r/wurstmeister/kafka/
- https://stackoverflow.com/questions/37428269/build-a-multi-node-kafka-cluster-on-docker-swarm
- https://jeqo.github.io/post/2017-01-15-scale-kafka-containers/
- http://codeblog.dotsandbrackets.com/highly-available-kafka-cluster-docker/
- https://blogs.perficient.com/delivery/blog/2017/05/25/how-to-install-kafka-cluster-in-the-docker-containers/
- https://sematext.com/blog/monitoring-kafka-on-docker-cloud/
- https://www.linkedin.com/pulse/zookeeper-kafka-aws-docker-swarms-oh-my-eric-kolotyluk/


### Once docker swarm successfully runs many kafka nodes
- datomic in existing VPC
- swarm visualizer node
- datomic console
