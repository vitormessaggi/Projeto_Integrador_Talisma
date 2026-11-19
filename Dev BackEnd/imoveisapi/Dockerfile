FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY ["Dev BackEnd/imoveisapi", "Dev BackEnd/imoveisapi"]
COPY ["Dev BancoDados/DDL", "Dev BancoDados/DDL"]
COPY ["Dev BancoDados/DML", "Dev BancoDados/DML"]
WORKDIR /workspace/Dev BackEnd/imoveisapi
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre
RUN groupadd --gid 10001 talisma && useradd --uid 10001 --gid talisma --create-home talisma
WORKDIR /app
COPY --from=build --chown=talisma:talisma ["/workspace/Dev BackEnd/imoveisapi/target/imoveisapi-0.0.1-SNAPSHOT.jar", "/app/api.jar"]
USER 10001:10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -Djava.awt.headless=true"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/api.jar"]
