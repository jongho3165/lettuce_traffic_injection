FROM eclipse-temurin:17-jdk-jammy

# JAR 파일 이름을 명확히 지정
ARG JAR_FILE=target/redis-traffic-generator-0.0.1-SNAPSHOT.jar

# JAR 파일을 컨테이너 내부로 복사
COPY ${JAR_FILE} app.jar

# Spring Boot 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]
