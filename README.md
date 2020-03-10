# userd

User management microservice written in Scala. Provides a gRPC interface.

## Building

To build a Docker image:

```
make docker
```

You can specify an image tag:

```
VERSION=x.y.z make docker
```

## Running Tests

```
make test
```
