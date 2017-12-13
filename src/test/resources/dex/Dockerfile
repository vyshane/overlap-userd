# Dex docker image for integration tests

FROM quay.io/coreos/dex:v2.8.1

COPY config.yaml /
RUN touch /dex.db

EXPOSE 5556
EXPOSE 5557

CMD ["serve", "/config.yaml"]