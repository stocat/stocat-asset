# Auth API

## 로컬 개발

1. `localhost`용 Makefile을 사용해 Gradle 빌드→이미지→kind→Helm 배포를 자동화합니다.
   ```bash
   make -C localhost all   # 또는 기본 target이라면 make -C localhost
   ```
   - Docker 이미지: `stocat/auth-api:local`
   - Kind 클러스터: `stocat-local`, 네임스페이스: `stocat`
2. 이미지만 빌드하거나 kind에 올리고 싶으면 `make -C localhost docker`/`make -C localhost kind-load`를 사용하세요.

## 원격용 Docker 이미지 빌드/배포

1. Gradle 빌드 및 Docker 이미지:
   ```bash
   make boot   # gradle :auth-api:bootJar
   make docker # docker build -t stocat/auth-api:0.0.1
   ```
2. Docker Hub 푸시:
   ```bash
   make push   # docker push stocat/auth-api:0.0.1
   ```
   - 태그를 바꾸려면 `make TAG=0.0.2 push`처럼 명령어에서 덮어쓰세요.

## Helm 배포

- 로컬(k1nd) 배포: `make -C localhost` 또는 루트에서 `make helm-local-auth-api`.
- 운영/스테이징: `cd ../helm && make deploy SERVICE=auth-api ENV=prod`.
  필요한 values 파일(`services/auth-api/values.yaml`, `services/auth-api/<env>.values.yaml`, `helm/<env>.values.yaml`)이 자동으로 포함됩니다.
