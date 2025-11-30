# Trade API

## 로컬 개발

- Kind + Helm까지 자동 실행:
  ```bash
  make -C localhost all   # 또는 기본 target이라면 make -C localhost
  ```
  (이미지만 올릴 경우 `make -C localhost kind-load`)
- 이미지 태그: `stocat/trade-api:local`, 클러스터: `stocat-local`, 네임스페이스: `stocat`.

## Docker 빌드/푸시

```bash
make boot    # gradle :trade-api:bootJar
make docker  # docker build -t stocat/trade-api:0.0.1
make push    # docker push stocat/trade-api:0.0.1
```
- 다른 태그를 쓰려면 `make TAG=1.0.0 push`처럼 실행.

## Helm 배포

- 로컬: `make -C localhost` 혹은 루트에서 `make helm-local-trade-api`.
- 운영/스테이징: `cd ../helm && make deploy SERVICE=trade-api ENV=prod`.
  환경별 values 파일(`services/trade-api/<env>.values.yaml`, `helm/<env>.values.yaml`)이 자동으로 합쳐집니다.
