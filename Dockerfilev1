#Create an image from a base image that already has java 24 runtime
#eclipse-temurin Java image + your Spring Boot JAR = your application's Docker image
FROM eclipse-temurin:24-jre

#inside the container, use /app as the working directory
WORKDIR /app

#Take local built Spring boot JAR and places inside the docker image as /app/app.jar
COPY target/*.jar app.jar

# Documents that the application listens on port 8080
EXPOSE 8080

#This mean when docker start this container, it should execute the command java -jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]