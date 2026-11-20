# db/init 변경 기록

스키마(01_schema.sql)·시드(02_seed.sql)를 바꿀 때 이유를 남긴다. 프론트 `src/data/seed.json`도 같은 값으로 맞춘다.

## 2026-11-20 — 백엔드 연동 준비 (feat/integration)

- **비밀번호 해시 재생성**: 기존 `$2b$10$abcdefghijklmnopqrstuu…`는 `demo1234`와 일치하지 않았다(BCrypt 검사 실패). `./gradlew seedSecrets`로 `demo1234`의 BCrypt 해시를 새로 만들어 10명 모두에 적용했다. 비밀번호는 README·프론트 안내와 같은 `demo1234`.
- **주민번호 암호문 재생성**: 기존 값은 형식만 맞춘 자리표시자였다. 서버의 AES-256-GCM(`RESIDENT_NO_KEY`)으로 가상 주민번호 10개를 암호화해 교체했다. 번호는 프론트 `residentNumber.ts` 검사를 통과하는 가상 값이며 `src/test/java/.../tools/SeedSecrets.java`에 있다. 키가 다르면 응답의 마스킹 값이 `******-*******`로 표시된다.
- **학력 값 교체**: `대학교 졸업` → `대졸(학사)`. 프론트 학력 선택지(고졸/전문대졸(초대졸)/대졸(학사)/석사/박사)와 맞춘다. 서버 가입·프로필 수정 검사도 이 5개만 허용한다.
