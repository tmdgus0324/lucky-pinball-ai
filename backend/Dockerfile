# Render 등 플랫폼의 Java 자동 감지에 기대지 않고 Java 17을 명시적으로 고정하기 위한
# 최소 멀티스테이지 빌드. 빌드 스테이지에서 bootJar를 만들고, 실행 스테이지는 JRE만 담아
# 이미지를 가볍게 유지한다.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
