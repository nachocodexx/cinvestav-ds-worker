# Load Balancer
## Dependencies
- sbt
- java
## Build 
Using the following command to build the project and create a docker image.
```
chmod +x build.sh && ./build.sh
```
## Runnig
```
docker run --name worker-0 -d --env PORT=6000 --env HOST=localhost --env QUEUE_SIMULATOR=/app/src/single -p 6000:6000 nachocode/cinvestav-ds-worker 
```
