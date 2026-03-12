FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY . .
# Fix line endings and permissions for gradlew
RUN apk add --no-cache dos2unix && dos2unix gradlew
RUN chmod +x gradlew
# Build the application
RUN ./gradlew bootJar -x test --no-daemon
# Remove the plain jar if it exists, so we only have the fat jar
RUN find build/libs -name "*-plain.jar" -delete || true
# Rename the remaining fat jar to app.jar
RUN mv build/libs/*.jar build/libs/app.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
