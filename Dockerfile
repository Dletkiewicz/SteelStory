FROM openjdk:17-oracle
COPY target/*.jar api.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","api.jar"]