# edgarly

Platform project for Edgar

## Usage


### Running from the REPL

Alternately, run:

    $ lein repl
    nREPL server started on port 52137 on host 127.0.0.1
    user => (go)

This will allow you to launch the app from the Clojure REPL. You can then make
changes and run `(reset)` to reload the app or `(stop)` to shutdown the app.

In addition, the functions `(context)` and `(print-context)` are available to
print out the current trapperkeeper application context. Both of these take an
optional array of keys as a parameter, which is used to retrieve a nested
subset of the context map.

## Notes

You can connect to TWS, with a VNC viewer. I have TightVNC.

```
cd ~/Downloads/tvnjviewer-2.8.3-bin-gnugpl/
java -jar tightvnc-jviewer.jar
```

Initial build of base docker images
```
docker build --force-rm -f Dockerfile.tws.base -t twashing/edgarly-tws-base:latest .
docker build --force-rm -f Dockerfile.app.base -t twashing/edgarly-app-base:latest .
```

Bringing up docker-compose 
```
# Basic
docker-compose up 

# Force a rebuild of containers
docker-compose up --force-recreate --build
```


# TODO

- make new project - ibgateway
- logging
- configs
- core.spec?
- tests


## Lifecycle functions

- return channels
- expose scanner results in system map
- expose request ids in system map (filter by request type)
- check that system is started
- check that connection is valid


## KStreams

- explicitly create topics, based on metadata
- use Transit Serde to read / write


## Questions

? How to use a custom Transit serde


[ok] How can we split from 1 partition topic, to an N partition topic
  [N] We cannot. Use Global KTables
  - http://stackoverflow.com/questions/42478842/kafka-streams-api-i-am-joining-two-kstreams-of-empmodel/42492618#42492618
  - http://stackoverflow.com/questions/41796207/dynamically-connecting-a-kafka-input-stream-to-multiple-output-streams/41799272#41799272

[ok] Do topics get auto created when we create a KStream or KTable topic
  [N] Topics
  [Y] State Stores

[ok] What are the lifecycle transformations between: scanner , scanner-start , scanner-stop - Aggregation (Windowed)

  - join scanner-start and scanner-stop
    - if only scanner-start: Y
    - if only scanner-stop: N
    - if scanner-start & scanner-stop: N


## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

