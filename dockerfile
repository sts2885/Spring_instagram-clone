#개발환경이 openjdk 11임
#https://medium.com/@AlexanderObregon/using-gradle-to-build-and-deploy-docker-containers-d4738cada039
#FROM openjdk:11-jre-slim
FROM openjdk:11-jre-slim-buster

COPY ./build/libs/Instagram-0.0.1-SNAPSHOT.jar /app/app.jar

CMD ["java", "-jar", "/app/app.jar"]
