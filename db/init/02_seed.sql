-- 2조 테스트 데이터 적재 스크립트 / ERD v1.5
-- 01_schema.sql 실행 후 빈 테이블에 한 번만 실행. 모든 데이터는 가상 데이터.
SET NAMES utf8mb4;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
USE `employee_project_management`;
START TRANSACTION;

INSERT INTO `EMPLOYEE` (`employee_id`, `employee_no`, `employee_name`, `resident_no_encrypted`, `education`, `employment_status`, `hire_date`, `resignation_date`, `total_career_months`, `skill_description`, `job_category`, `login_id`, `password_hash`, `system_role`)
VALUES
  (1, '20240001', '김민준', 'aesgcm:IH+a/5JowxifAWFtPYmWcizy12jdwUpgEW+lMJeC7ys9+5lGxL/BVhnV', '대졸(학사)', 'ACTIVE', '2024-01-15', NULL, 27, 'Java, SQL 및 프로젝트 협업 경험', 'DEVELOPER', 'demo01', '$2a$10$Y3buPkOU/L9pzNf/NAs9kewmS6KafROfMunjx/cjU1pGJefdSg7U2', 'MANAGEMENT'),
  (2, '20240002', '이서연', 'aesgcm:B816gqtDhwCJ5w9n32cQNn9z9shvJn2GFoUa51FNHMpSqiDUwO7rvcqs', '대졸(학사)', 'ACTIVE', '2024-01-15', NULL, 30, 'Java, SQL 및 프로젝트 협업 경험', 'DEVELOPER', 'demo02', '$2a$10$Y3buPkOU/L9pzNf/NAs9kewmS6KafROfMunjx/cjU1pGJefdSg7U2', 'EMPLOYEE'),
  (3, '20240003', '박지훈', 'aesgcm:r0PtHrzhGowKkIm0ZmvN3cIwQfQWJ+eKzP7oxPrOwOnPX66Nwt6uEMMI', '대졸(학사)', 'ACTIVE', '2024-01-15', NULL, 33, 'Java, SQL 및 프로젝트 협업 경험', 'DEVELOPER', 'demo03', '$2a$10$Y3buPkOU/L9pzNf/NAs9kewmS6KafROfMunjx/cjU1pGJefdSg7U2', 'EMPLOYEE'),
  (4, '20240004', '최수빈', 'aesgcm:HokHNm4nma/RUszFWsg/2Taqn6Ec+VUf7mPXP4TDLAUd2p53czPHhPPk', '대졸(학사)', 'ACTIVE', '2024-01-15', NULL, 36, 'Java, SQL 및 프로젝트 협업 경험', 'DEVELOPER', 'demo04', '$2a$10$Y3buPkOU/L9pzNf/NAs9kewmS6KafROfMunjx/cjU1pGJefdSg7U2', 'EMPLOYEE'),
  (5, '20240005', '정도윤', 'aesgcm:q849NjEWRtVr/zzdWMCVIiKVz3gPGDbnUNv4nxjvTi2QxQFUTvXN1+Vw', '대졸(학사)', 'ACTIVE', '2024-01-15', NULL, 39, 'Java, SQL 및 프로젝트 협업 경험', 'DEVELOPER', 'demo05', '$2a$10$Y3buPkOU/L9pzNf/NAs9kewmS6KafROfMunjx/cjU1pGJefdSg7U2', 'EMPLOYEE'),
  (6, '20250001', '강하린', 'aesgcm:KsZ1uWDuTiC4IMXjmYoyOrlUZ1hm9xeEmK4UK9mlA8HkZdlNAqBHL6GQ', '대졸(학사)', 'ACTIVE', '2025-03-03', NULL, 42, 'Java, SQL 및 프로젝트 협업 경험', 'DEVELOPER', 'demo06', '$2a$10$Y3buPkOU/L9pzNf/NAs9kewmS6KafROfMunjx/cjU1pGJefdSg7U2', 'EMPLOYEE'),
  (7, '20250002', '윤서준', 'aesgcm:pQhXE6GROCV0Tf/lRUM2qnuGFQt+CZ3BRi27CiCe0IK9zzuOu09n96yS', '대졸(학사)', 'ACTIVE', '2025-03-03', NULL, 45, 'Java, SQL 및 프로젝트 협업 경험', 'DEVELOPER', 'demo07', '$2a$10$Y3buPkOU/L9pzNf/NAs9kewmS6KafROfMunjx/cjU1pGJefdSg7U2', 'EMPLOYEE'),
  (8, '20250003', '장예린', 'aesgcm:4qBQ2F0/eMflKBEwhX1zbabIJtsOFq2E8K9Cye0kAYRYQeiKVDVmGBLw', '대졸(학사)', 'ACTIVE', '2025-03-03', NULL, 48, 'Java, SQL 및 프로젝트 협업 경험', 'MARKETING', 'demo08', '$2a$10$Y3buPkOU/L9pzNf/NAs9kewmS6KafROfMunjx/cjU1pGJefdSg7U2', 'EMPLOYEE'),
  (9, '20250004', '임현우', 'aesgcm:2dlUvFKSXu91Zh2HBOC/WAajTDSmsViIucqlv5NEoyDRwpLPMhH3zCGb', '대졸(학사)', 'ON_LEAVE', '2025-03-03', NULL, 51, NULL, 'BUSINESS_ADMIN', 'demo09', '$2a$10$Y3buPkOU/L9pzNf/NAs9kewmS6KafROfMunjx/cjU1pGJefdSg7U2', 'MANAGEMENT'),
  (10, '20250005', '한지민', 'aesgcm:Pgt3XzkA6OXVWFWqInWT41/GXoUg+wEAc83pAXAx/s0kzKBjEoEtQRV0', '대졸(학사)', 'RESIGNED', '2025-03-03', '2026-10-01', 54, 'Java, SQL 및 프로젝트 협업 경험', 'RESEARCH', 'demo10', '$2a$10$Y3buPkOU/L9pzNf/NAs9kewmS6KafROfMunjx/cjU1pGJefdSg7U2', 'EMPLOYEE');

INSERT INTO `CLIENT` (`client_id`, `client_name`, `contact_name`, `contact_email`, `contact_phone_number`)
VALUES
  (1, '가상 한빛유통', '김담당', 'hanbit@example.com', '02-000-0001'),
  (2, '가상 미래연구소', '이담당', 'research@example.com', NULL),
  (3, '가상 새봄교육', '박담당', 'saebom@example.com', '02-000-0003');

INSERT INTO `PROJECT` (`project_id`, `project_name`, `client_id`, `actual_start_date`, `planned_end_date`, `actual_end_date`, `evaluation_requested_date`, `evaluation_received_date`, `evaluation_reported_date`)
VALUES
  (1, '유통 재고관리 시스템', 1, '2026-01-05', '2026-03-31', '2026-03-31', '2026-04-01', '2026-04-03', '2026-04-06'),
  (2, '연구 데이터 관리 시스템', 2, '2026-04-01', '2026-06-30', '2026-07-03', '2026-07-04', NULL, NULL),
  (3, '교육 콘텐츠 관리 시스템', 3, '2026-08-03', '2026-09-30', NULL, NULL, NULL, NULL),
  (4, '유통 분석 대시보드', 1, '2026-07-06', '2026-09-30', '2026-09-30', NULL, NULL, NULL);

INSERT INTO `PROJECT_MEMBER` (`member_id`, `project_id`, `employee_id`, `project_role`, `assignment_start_date`, `assignment_end_date`)
VALUES
  (1, 1, 1, 'PM', '2026-01-05', '2026-03-31'),
  (2, 1, 2, 'PL', '2026-01-05', '2026-03-31'),
  (3, 1, 3, 'PROGRAMMER', '2026-01-05', '2026-03-31'),
  (4, 1, 4, 'TESTER', '2026-01-05', '2026-03-31'),
  (5, 1, 8, 'ANALYST', '2026-01-05', '2026-03-31'),
  (6, 2, 5, 'PM', '2026-04-01', '2026-07-03'),
  (7, 2, 6, 'DESIGNER', '2026-04-01', '2026-07-03'),
  (8, 2, 7, 'PROGRAMMER', '2026-04-01', '2026-07-03'),
  (9, 2, 8, 'ANALYST', '2026-04-01', '2026-07-03'),
  (10, 2, 10, 'TESTER', '2026-04-01', '2026-07-03'),
  (11, 3, 1, 'PM', '2026-08-03', NULL),
  (12, 3, 2, 'PL', '2026-08-03', NULL),
  (13, 3, 6, 'PROGRAMMER', '2026-08-03', '2026-09-15'),
  (14, 3, 8, 'ANALYST', '2026-08-03', NULL),
  (15, 3, 9, 'DESIGNER', NULL, NULL),
  (16, 4, 5, 'PM', '2026-07-06', '2026-09-30'),
  (17, 4, 3, 'DESIGNER', '2026-07-06', '2026-09-30'),
  (18, 4, 4, 'PROGRAMMER', '2026-07-06', '2026-09-30'),
  (19, 4, 7, 'PL', '2026-07-06', '2026-09-30'),
  (20, 4, 10, 'TESTER', '2026-07-06', '2026-09-30');

INSERT INTO `EVALUATION` (`evaluation_id`, `target_member_id`, `evaluation_type`, `evaluator_member_id`, `registered_by_member_id`, `evaluated_date`, `work_performance_score`, `work_performance_comment`, `communication_score`, `communication_comment`)
VALUES
  (1, 1, 'CLIENT', NULL, 1, '2026-04-02', 4, '맡은 업무를 수행하고 결과를 공유했습니다.', 5, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (2, 2, 'CLIENT', NULL, 1, '2026-04-02', 5, '맡은 업무를 수행하고 결과를 공유했습니다.', 3, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (3, 3, 'CLIENT', NULL, 1, '2026-04-02', 3, '맡은 업무를 수행하고 결과를 공유했습니다.', 4, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (4, 4, 'CLIENT', NULL, 1, '2026-04-02', 4, '맡은 업무를 수행하고 결과를 공유했습니다.', 5, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (5, 5, 'CLIENT', NULL, 1, '2026-04-02', 5, '맡은 업무를 수행하고 결과를 공유했습니다.', 3, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (6, 2, 'PM', 1, 1, '2026-03-31', 3, '맡은 업무를 수행하고 결과를 공유했습니다.', 4, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (7, 3, 'PM', 1, 1, '2026-03-31', 4, '맡은 업무를 수행하고 결과를 공유했습니다.', 5, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (8, 4, 'PM', 1, 1, '2026-03-31', 5, '맡은 업무를 수행하고 결과를 공유했습니다.', 3, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (9, 5, 'PM', 1, 1, '2026-03-31', 3, '맡은 업무를 수행하고 결과를 공유했습니다.', 4, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (10, 2, 'PEER', 1, 1, '2026-03-31', 4, '맡은 업무를 수행하고 결과를 공유했습니다.', 5, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (11, 3, 'PEER', 2, 2, '2026-03-31', 5, '맡은 업무를 수행하고 결과를 공유했습니다.', 3, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (12, 4, 'PEER', 3, 3, '2026-03-31', 3, '맡은 업무를 수행하고 결과를 공유했습니다.', 4, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (13, 5, 'PEER', 4, 4, '2026-03-31', 4, '맡은 업무를 수행하고 결과를 공유했습니다.', 5, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (14, 1, 'PEER', 5, 5, '2026-03-31', 5, '맡은 업무를 수행하고 결과를 공유했습니다.', 3, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (15, 6, 'CLIENT', NULL, 6, '2026-07-05', 3, '맡은 업무를 수행하고 결과를 공유했습니다.', 4, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (16, 7, 'CLIENT', NULL, 6, '2026-07-05', 4, '맡은 업무를 수행하고 결과를 공유했습니다.', 5, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (17, 7, 'PM', 6, 6, '2026-07-03', 5, '맡은 업무를 수행하고 결과를 공유했습니다.', 3, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (18, 8, 'PM', 6, 6, '2026-07-03', 3, '맡은 업무를 수행하고 결과를 공유했습니다.', 4, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (19, 9, 'PM', 6, 6, '2026-07-03', 4, '맡은 업무를 수행하고 결과를 공유했습니다.', 5, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (20, 10, 'PM', 6, 6, '2026-07-03', 5, '맡은 업무를 수행하고 결과를 공유했습니다.', 3, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (21, 7, 'PEER', 6, 6, '2026-07-03', 3, '맡은 업무를 수행하고 결과를 공유했습니다.', 4, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (22, 8, 'PEER', 7, 7, '2026-07-03', 4, '맡은 업무를 수행하고 결과를 공유했습니다.', 5, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (23, 9, 'PEER', 8, 8, '2026-07-03', 5, '맡은 업무를 수행하고 결과를 공유했습니다.', 3, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (24, 10, 'PEER', 9, 9, '2026-07-03', 3, '맡은 업무를 수행하고 결과를 공유했습니다.', 4, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (25, 6, 'PEER', 10, 10, '2026-07-03', 4, '맡은 업무를 수행하고 결과를 공유했습니다.', 5, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (26, 17, 'PM', 16, 16, '2026-09-30', 5, '맡은 업무를 수행하고 결과를 공유했습니다.', 3, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (27, 18, 'PM', 16, 16, '2026-09-30', 3, '맡은 업무를 수행하고 결과를 공유했습니다.', 4, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (28, 19, 'PM', 16, 16, '2026-09-30', 4, '맡은 업무를 수행하고 결과를 공유했습니다.', 5, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (29, 20, 'PM', 16, 16, '2026-09-30', 5, '맡은 업무를 수행하고 결과를 공유했습니다.', 3, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (30, 17, 'PEER', 16, 16, '2026-09-30', 3, '맡은 업무를 수행하고 결과를 공유했습니다.', 4, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (31, 18, 'PEER', 17, 17, '2026-09-30', 4, '맡은 업무를 수행하고 결과를 공유했습니다.', 5, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (32, 19, 'PEER', 18, 18, '2026-09-30', 5, '맡은 업무를 수행하고 결과를 공유했습니다.', 3, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (33, 20, 'PEER', 19, 19, '2026-09-30', 3, '맡은 업무를 수행하고 결과를 공유했습니다.', 4, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.'),
  (34, 16, 'PEER', 20, 20, '2026-09-30', 4, '맡은 업무를 수행하고 결과를 공유했습니다.', 5, '일정과 진행 상황을 팀원에게 명확하게 전달했습니다.');

COMMIT;
