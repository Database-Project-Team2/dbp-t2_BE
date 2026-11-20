# dbp-t2 BE

프람트 워크스페이스(프로젝트·인력 관리) 백엔드 — 데이터베이스프로젝트 2조

- Java 21 · Spring Boot 3.5.15 · Spring Data JPA (Hibernate) · MySQL 8.4
- 프론트 데모: https://dbp-t2-fe.vercel.app

## 로컬 실행

필요한 것: JDK 21, Docker Desktop

```bash
cp .env.example .env          # 포트·비밀번호, RESIDENT_NO_KEY·JWT_SECRET 채우기
docker compose up -d          # MySQL 기동 + db/init 스키마·시드 자동 적재 (최초 1회)
./gradlew test                # 매핑 검증 테스트 (MySQL이 떠 있어야 함)
./gradlew bootRun             # http://localhost:8080/actuator/health
```

DB를 시드 상태로 되돌리기: `scripts/reset-db.sh` (서버를 띄운 채로 테이블만 다시 만든다) 또는 `docker compose down -v && docker compose up -d`

시드 비밀번호는 모두 `demo1234`. 주민번호 암호문은 `RESIDENT_NO_KEY`로 만들어져 있어 키가 다르면 마스킹 값이 `******-*******`로 보인다. 키를 바꾸면 `./gradlew seedSecrets`로 시드를 다시 만든다(db/CHANGELOG.md).

> 로컬에 MySQL이 이미 3306을 쓰고 있으면 `.env`의 `DB_PORT`를 3307 등으로 바꾼다.

## API·문서

- 기본 경로 `/api/v1`, 명세는 프론트 저장소 `docs/API_SPEC.md`. Swagger: 로컬 `http://localhost:8080/swagger-ui.html`, 배포 `https://220-66-233-75.nip.io/dbp-api/swagger-ui.html` (로그인 응답의 `access_token`을 Authorize에 입력)
- 테스트는 docker compose MySQL을 테스트마다 시드로 되돌린다(`IntegrationTest`). 기준일은 2026-11-20으로 고정.

## 배포(BlueRack VM)

- 위치 `~/dbp`, `deploy/docker-compose.prod.yml`(프로젝트 이름 `dbp-t2`: app 127.0.0.1:8081, MySQL 내부망). Caddy가 `https://220-66-233-75.nip.io/dbp-api/*`를 8081로 넘긴다.
- `deploy/.env`에 DB 비밀번호와 `RESIDENT_NO_KEY`(시드 암호문을 만든 키), `JWT_SECRET`을 둔다.
- 갱신: 소스를 `~/dbp`에 올린 뒤 `cd ~/dbp/deploy && sudo docker compose -f docker-compose.prod.yml up -d --build`
- **시연용 DB 초기화(한 줄)**: `cd ~/dbp && sudo scripts/reset-db.sh dbp-t2-mysql-1`

## 구조

```
db/init/            01_schema.sql, 02_seed.sql  ← 스키마의 기준(과제 제출물과 동일)
src/main/java/com/dbpt2/server/
  employee/         EMPLOYEE (직원 + 계정 + 권한)
  client/           CLIENT
  project/          PROJECT
  member/           PROJECT_MEMBER
  evaluation/       EVALUATION
```

## 정해둔 규칙

| 규칙 | 이유 |
|---|---|
| `ddl-auto: validate` — JPA가 테이블을 만들거나 바꾸지 않는다 | 스키마는 SQL 스크립트가 기준. 엔티티가 어긋나면 서버가 뜨지 않아 바로 드러난다 |
| 이름 변환 없음(`PhysicalNamingStrategyStandardImpl`) + 모든 `@Table`/`@Column`에 이름 명시 | 기본 전략은 `EMPLOYEE`를 `employee`로 바꾸는데, 리눅스 MySQL(docker·배포 서버)은 대소문자를 구분해 테이블을 못 찾는다 |
| enum·TEXT 컬럼에 `columnDefinition` 명시 | Hibernate 6은 MySQL에서 enum을 네이티브 ENUM으로 기대해 VARCHAR 컬럼과 validate가 실패한다 |
| ID는 `Integer` | 스키마가 `INT`. `Long`이면 BIGINT를 기대해 validate 실패 |
| 연관관계는 `@ManyToOne(LAZY)` 단방향만 | EVALUATION이 PROJECT_MEMBER를 세 번 참조해 양방향은 복잡도만 늘어난다 |
| 엔티티에 setter 없음, 변경 불가 컬럼은 `updatable = false` | 사번·로그인 ID·프로젝트 역할 고정 규칙을 매핑 단계에서도 보장 |

## DB가 막지 못해 서버에서 검증할 것

같은 프로젝트 소속 여부 · 역할 변경/재투입 차단 · 투입기간 ⊂ 프로젝트 기간 · 평가 시점 · PM 자격 · **고객평가 대상당 1건** · 접근 권한
(ERD 설계 시트 "서버 구현 경계" 참고)
