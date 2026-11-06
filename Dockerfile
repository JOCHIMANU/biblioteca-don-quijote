# Construcción en dos etapas: se compila con el JDK y se ejecuta solo con el JRE (imagen final pequeña).
FROM eclipse-temurin:17-jdk AS construir
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
# se baja primero lo que no cambia, para que reconstruir tras tocar el código sea rápido
RUN sh mvnw -q -B dependency:go-offline
COPY src src
RUN sh mvnw -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 biblioteca
COPY --from=construir /app/target/biblioteca-*.jar /app/biblioteca.jar
USER biblioteca

# Memoria justa para los planes gratuitos (512 MB) y arranque rápido
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
# El proveedor fija PORT; la app lo lee sola (server.port=${PORT:8080})
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/biblioteca.jar"]
