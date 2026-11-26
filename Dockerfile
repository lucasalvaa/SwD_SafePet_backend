FROM gcr.io/distroless/java21-debian13:nonroot@sha256:9a868974dd82da359adf8c659d9f9a8d5249b138a733def74adbf00f64975db3

WORKDIR /app

COPY --chown=nonroot:nonroot target/backend.jar /app/backend.jar

EXPOSE 8080

# L'entrypoint della base image è già "java -jar"
CMD ["/app/backend.jar"]
