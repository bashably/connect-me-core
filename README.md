# Running
In order to run and test this service (consisting of multiple services such as
databases), `docker-compose` is used.

## Deployment
```shell
# 1) clone project tree
git clone https://github.com/...
# 2) build project without running tests
mvn package -DskipTests
# 3) build docker script
docker-compose build
# 4) run everything (unfold all systems) using docker-compose
docker-compose up -d
```

## Testing

```shell
## EXECUTE IN ORDER TO ESTABLISH TESTING ENVIRONMENT
# in order to build the environment
docker-compose -f docker-compose-environment.yml build
# in order to setup environment systems (will run in background)
docker-compose -f docker-compose-environment.yml up -d

## EXECUTE IN ORDER TO RUN UNIT TESTS
# in order to build and run unit tests using maven
mvn test

## EXECUTE WHEN YOU ARE FINISHED
# in order to shut down environment systems
docker-compose -f docker-compose-environment.yml down
```

