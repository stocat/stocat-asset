REGISTRY := stocat
TAG := 0.0.1
OTEL_AGENT_VERSION := 2.8.0
GRADLE := ../gradlew
MODULE := auth-api
IMAGE := $(REGISTRY)/$(MODULE):$(TAG)

.PHONY: help boot docker push build
help:
	@echo "Targets: boot, docker, push, build"

boot:
	$(GRADLE) :$(MODULE):bootJar

docker: boot
	docker build \
		--build-arg OTEL_AGENT_VERSION=$(OTEL_AGENT_VERSION) \
		-t $(IMAGE) \
		-f Dockerfile .

push: docker
	docker push $(IMAGE)

build: push
