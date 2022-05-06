# Running
In order to run and test this service (consisting of multiple services such as
databases), `docker-compose` is used.

## Deployment (local installation)
```shell
# 0) make sure you are inside the root directory of the project tree
# 1) build project without running tests (because the successfull execution of tests would require a running testing environment)
mvn package -DskipTests
# 2) build docker images (packaged .war file, databases, etc)
docker-compose build
# 3) run created images inside network of containers (unfold all systems) using docker-compose
docker-compose up -d
```

## Testing

```shell
## EXECUTE ONCE IN ORDER TO ESTABLISH TESTING ENVIRONMENT
# in order to build the environment
docker-compose -f docker-compose-environment.yml build
# in order to setup environment systems (will run in background)
docker-compose -f docker-compose-environment.yml up -d

# ... wait a few seconds for database setup to complete ...

## EXECUTE IN ORDER TO RUN UNIT TESTS
# in order to build and run unit tests using maven
mvn test

## EXECUTE ONCE WHEN YOU ARE FINISHED
# in order to shut down environment systems
docker-compose -f docker-compose-environment.yml down
# you dont need to restart the testing environment every time.
```

# Documentation
The Documentation is stored in directory `documentation` as a **LogSeq Graph**.

In order to open this LogSeq Graph, download LogSeq [here](http://www.logseq.com) and open the directory with it.

## LogSeq Introduction
LogSeq works just like an advanced Wiki: It supports interlinked pages written in Markdown. It allows you 
to structure and present information. You can view the connection between different pages as a graph 
and add new pages to it.

