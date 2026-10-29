FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw clean package -DskipTests

# Separa o jar em app.jar + lib/ (formato exigido pelo cache de classes abaixo)
RUN cp target/*.jar app.jar && java -Djarmode=tools -jar app.jar extract --destination extraido

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/extraido/ ./

# Cache de classes (CDS): uma subida de treino, sem banco, grava as classes carregadas
# em app.jsa e as próximas subidas leem dali em vez de carregar tudo de novo.
# O treino precisa rodar na mesma JVM da imagem final.
RUN java -XX:ArchiveClassesAtExit=app.jsa \
        -Dspring.context.exit=onRefresh \
        -Dspring.profiles.active=prod \
        -DDBUrl=jdbc:postgresql://127.0.0.1:1/treino -DDBUsername=treino -DDBPassword=treino \
        -Dspring.jpa.hibernate.ddl-auto=none \
        -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
        -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
        -jar app.jar

EXPOSE 10000

# A instância do Render tem pouca CPU: só o compilador JIT rápido (C1) e o GC serial
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
