FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .. .
RUN chmod +x gradlew && ./gradlew bootJar -x test

FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /app/applications/app-service/build/libs/products.jar products.jar
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom"
USER appuser
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /products.jar"]
