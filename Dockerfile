FROM openjdk:8-alpine
WORKDIR /app/src
COPY ./target/universal/cinvestav-ds-worker-0.1.zip /app/src/app.zip
COPY ./single.out /app/src/single.out
RUN apk add --no-cache --upgrade bash
#RUN apk add build-base
RUN chmod 775 ./single.out
RUN unzip /app/src/app.zip
ENTRYPOINT ["/app/src/cinvestav-ds-worker-0.1/bin/cinvestav-ds-worker"]

