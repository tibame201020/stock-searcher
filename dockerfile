FROM maven:3.9.2-amazoncorretto-17 as builder
COPY . /buildDir
WORKDIR /buildDir
RUN mvn clean package -Dmaven.test.skip=true
FROM maven:3.9.2-amazoncorretto-17
COPY --from=builder /buildDir/target/stock-searcher-0.0.1-SNAPSHOT.jar app/springboot.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "springboot.jar"]
