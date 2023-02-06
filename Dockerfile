#Dockerfile
FROM mcr.microsoft.com/java/jre:18-zulu-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]