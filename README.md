## Requirements  

- Java 21 (openjdk 21.0.2)
- Apache Maven 3.9.6
- Docker version 24.0.7

## Execution steps

1) docker rmi -f edge-sqlite-db:1.0.0
2) mvn clean package
3) docker build -f Dockerfile . -t edge-sqlite-db:1.0.0
4) docker compose -f docker-kafka-compose.yml up
5) docker compose -f docker-compose.yml up
6) docker compose -f docker-client-compose.yaml up
7) docker compose -f docker-second-client-compose.yaml u

## More Details 

Μore details along with evaluation experiments can be found in the MSc dissertation (file: StorageFabricForEdgeDevices.pdf).
