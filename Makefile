REGISTRY := stocat
TAG := 0.0.1
KIND_CLUSTER := stocat-local
OTEL_AGENT_VERSION := 2.8.0

SERVICES := auth-api trade-api trade-websocket-api asset-scraper exchange-rate-crawler

.PHONY: help
help:
	@echo "Available targets:"
	@echo "  boot-<service>       Build Gradle bootJar for a service"
	@echo "  docker-<service>     Build Docker image for a service"
	@echo "  push-<service>       Push Docker image to the registry"
	@echo "  kind-load-<service>  Local build + kind load (no push)"
	@echo "  helm-local-<service> Local kind load + Helm deploy"
	@echo "  boot-all / docker-all / push-all / kind-load-all"

.PHONY: $(addprefix boot-,$(SERVICES))
$(addprefix boot-,$(SERVICES)): boot-%:
	$(if $(filter $*, $(SERVICES)),,$(error Unknown service '$*'))
	$(MAKE) -C $* boot \
		REGISTRY=$(REGISTRY) \
		TAG=$(TAG) \
		KIND_CLUSTER=$(KIND_CLUSTER) \
		OTEL_AGENT_VERSION=$(OTEL_AGENT_VERSION)

.PHONY: $(addprefix docker-,$(SERVICES))
$(addprefix docker-,$(SERVICES)): docker-%:
	$(if $(filter $*, $(SERVICES)),,$(error Unknown service '$*'))
	$(MAKE) -C $* docker \
		REGISTRY=$(REGISTRY) \
		TAG=$(TAG) \
		KIND_CLUSTER=$(KIND_CLUSTER) \
		OTEL_AGENT_VERSION=$(OTEL_AGENT_VERSION)

.PHONY: $(addprefix kind-load-,$(SERVICES))
$(addprefix kind-load-,$(SERVICES)): kind-load-%:
	$(if $(filter $*, $(SERVICES)),,$(error Unknown service '$*'))
	$(MAKE) -C $*/localhost kind-load \
		REGISTRY=$(REGISTRY) \
		TAG=$(TAG) \
		KIND_CLUSTER=$(KIND_CLUSTER) \
		OTEL_AGENT_VERSION=$(OTEL_AGENT_VERSION)

.PHONY: $(addprefix helm-local-,$(SERVICES))
$(addprefix helm-local-,$(SERVICES)): helm-local-%:
	$(if $(filter $*, $(SERVICES)),,$(error Unknown service '$*'))
	$(MAKE) -C $*/localhost helm-deploy \
		REGISTRY=$(REGISTRY) \
		TAG=$(TAG) \
		KIND_CLUSTER=$(KIND_CLUSTER) \
		OTEL_AGENT_VERSION=$(OTEL_AGENT_VERSION)

.PHONY: $(addprefix push-,$(SERVICES))
$(addprefix push-,$(SERVICES)): push-%:
	$(if $(filter $*, $(SERVICES)),,$(error Unknown service '$*'))
	$(MAKE) -C $* push \
		REGISTRY=$(REGISTRY) \
		TAG=$(TAG) \
		KIND_CLUSTER=$(KIND_CLUSTER) \
		OTEL_AGENT_VERSION=$(OTEL_AGENT_VERSION)

.PHONY: boot-all docker-all push-all kind-load-all helm-local-all
boot-all: $(addprefix boot-,$(SERVICES))

docker-all: $(addprefix docker-,$(SERVICES))

push-all: $(addprefix push-,$(SERVICES))

kind-load-all: $(addprefix kind-load-,$(SERVICES))

helm-local-all: $(addprefix helm-local-,$(SERVICES))
