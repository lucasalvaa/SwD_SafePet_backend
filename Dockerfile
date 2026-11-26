FROM gcr.io/distroless/java21-debian13:nonroot

WORKDIR /app

COPY --chown=nonroot:nonroot target/backend.jar /app/backend.jar

EXPOSE 8080

# L'entrypoint della base image è già "java -jar"
CMD ["/app/backend.jar"]
