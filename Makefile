# Default to version specified in build.sbt
ifeq ($(VERSION),)
  version_override =
else
  version_override = VERSION=$(VERSION)
endif

docker:
	$(version_override) sbt docker:publishLocal

dockerpush:
	$(version_override) sbt docker:publish

test:
	sbt test

clean:
	sbt clean
