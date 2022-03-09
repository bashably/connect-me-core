# needed to run spring boot jar
FROM openjdk:11.0-jdk-slim

# copy jar into image
COPY /target/*.jar app.jar

# run spring boot jar
ENTRYPOINT ["java","-jar" ,"/app.jar"]
