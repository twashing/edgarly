# IBGateway

Edgarly microservice for interfacing with the TWS Gateway. From here, the platform makes historical, stock and scanner requests.


## Notes

A) You can connect to TWS, with a VNC viewer (ex: TightVNC).

```
cd ~/Downloads/tvnjviewer-2.8.3-bin-gnugpl/
java -jar tightvnc-jviewer.jar
```

B) You have to do an initial build of base docker images.
```
docker build --no-cache -f Dockerfile.tws.base -t twashing/ibgateway-tws-base:latest -t twashing/ibgateway-tws-base:`git rev-parse HEAD` .
docker build --no-cache -f Dockerfile.tws -t twashing/ibgateway-tws:latest -t twashing/ibgateway-tws:`git rev-parse HEAD` .

docker build --no-cache -f Dockerfile.app.base -t twashing/ibgateway-app-base:latest -t twashing/ibgateway-app-base:`git rev-parse HEAD` .
docker build --no-cache -f Dockerfile.app -t twashing/ibgateway-app:latest -t twashing/ibgateway-app:`git rev-parse HEAD` .

lein with-profile  +app  run -m  com.interrupt.ibgateway.core/-main
```

C) Bringing up docker-compose 
```
# Basic
docker-compose up 

# Force a rebuild of containers
docker-compose up --force-recreate --build
```

## Lifecycle functions

- return channels
- expose scanner results in system map
- expose request ids in system map (filter by request type)
- check that system is started
- check that connection is valid

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
