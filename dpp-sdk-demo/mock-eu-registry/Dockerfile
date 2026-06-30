FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S app && adduser -S -G app app

WORKDIR /app

COPY mock-eu-registry/target/mock-eu-registry-*-exec.jar /app/app.jar
RUN chown app:app /app/app.jar

USER app

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD sh -c 'wget -qO- "http://127.0.0.1:${MOCK_REGISTRY_PORT:-${DPP_REGISTRY_PORT:-8081}}/health" > /dev/null || exit 1'

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD ["--debug=false"]
