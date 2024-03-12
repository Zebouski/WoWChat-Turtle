FROM maven:3.8.6-jdk-8 AS dependencies
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve

FROM maven:3.8.6-jdk-8 as builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY --from=dependencies /root/.m2 /root/.m2
RUN mvn clean package -DfinalName=wowchat

FROM openjdk:8-jre
WORKDIR /app
COPY ./src/main/resources/wowchat.conf /app/
COPY --from=builder /app/target/wowchat.jar /app

ENTRYPOINT ["java", "-jar", "wowchat.jar", "wowchat.conf"]
