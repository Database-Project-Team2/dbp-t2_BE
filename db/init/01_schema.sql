-- 2조 데이터베이스 생성 스크립트 / ERD v1.5
-- 대상: MySQL 8.4, InnoDB, utf8mb4. 과제 전용 빈 DB에서 실행.
SET NAMES utf8mb4;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
CREATE DATABASE IF NOT EXISTS `employee_project_management` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `employee_project_management`;
CREATE TABLE `EMPLOYEE` (
  `employee_id` INT NOT NULL AUTO_INCREMENT COMMENT '직원 식별자',
  `employee_no` VARCHAR(8) NOT NULL COMMENT '사번: 입사연도 4자리와 순번 4자리',
  `employee_name` VARCHAR(100) NOT NULL COMMENT '직원명',
  `resident_no_encrypted` VARCHAR(255) NOT NULL COMMENT '암호화 주민등록번호',
  `education` VARCHAR(100) NOT NULL COMMENT '최종학력',
  `employment_status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '현재 재직 상태',
  `hire_date` DATE NOT NULL COMMENT '입사일',
  `resignation_date` DATE NULL COMMENT '퇴사일',
  `total_career_months` INT NOT NULL COMMENT '총 경력 개월 수',
  `skill_description` TEXT NULL COMMENT '기술 및 경험 설명',
  `job_category` VARCHAR(30) NOT NULL COMMENT '직군',
  `login_id` VARCHAR(100) NOT NULL COMMENT '로그인 ID',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '비밀번호 해시',
  `system_role` VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE' COMMENT '시스템 권한',
  PRIMARY KEY (`employee_id`),
  CONSTRAINT `uq_employee_no` UNIQUE (`employee_no`),
  CONSTRAINT `uq_employee_login_id` UNIQUE (`login_id`),
  CONSTRAINT `chk_employee_employment_status` CHECK (employment_status IN ('ACTIVE', 'ON_LEAVE', 'RESIGNED')),
  CONSTRAINT `chk_employee_employment_dates` CHECK (resignation_date IS NULL OR resignation_date >= hire_date),
  CONSTRAINT `chk_employee_resignation_status` CHECK ((employment_status = 'RESIGNED' AND resignation_date IS NOT NULL) OR (employment_status IN ('ACTIVE', 'ON_LEAVE') AND resignation_date IS NULL)),
  CONSTRAINT `chk_employee_career` CHECK (total_career_months >= 0),
  CONSTRAINT `chk_employee_job_category` CHECK (job_category IN ('DEVELOPER', 'MARKETING', 'BUSINESS_ADMIN', 'RESEARCH')),
  CONSTRAINT `chk_employee_system_role` CHECK (system_role IN ('EMPLOYEE', 'MANAGEMENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='EMPLOYEE / ERD v1.5';

CREATE TABLE `CLIENT` (
  `client_id` INT NOT NULL AUTO_INCREMENT COMMENT '발주처 식별자',
  `client_name` VARCHAR(150) NOT NULL COMMENT '발주처명',
  `contact_name` VARCHAR(100) NOT NULL COMMENT '대표 담당자명',
  `contact_email` VARCHAR(254) NOT NULL COMMENT '대표 담당자 이메일',
  `contact_phone_number` VARCHAR(30) NULL COMMENT '대표 담당자 전화번호',
  PRIMARY KEY (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CLIENT / ERD v1.5';

CREATE TABLE `PROJECT` (
  `project_id` INT NOT NULL AUTO_INCREMENT COMMENT '프로젝트 식별자',
  `project_name` VARCHAR(200) NOT NULL COMMENT '프로젝트명',
  `client_id` INT NOT NULL COMMENT '발주처 식별자',
  `actual_start_date` DATE NOT NULL COMMENT '실제 착수일',
  `planned_end_date` DATE NOT NULL COMMENT '예정 종료일',
  `actual_end_date` DATE NULL COMMENT '실제 종료일: 진행 중 NULL',
  `evaluation_requested_date` DATE NULL COMMENT '고객평가 의뢰일',
  `evaluation_received_date` DATE NULL COMMENT '고객평가 전체 회수일',
  `evaluation_reported_date` DATE NULL COMMENT '회사 보고일',
  PRIMARY KEY (`project_id`),
  CONSTRAINT `fk_project_client` FOREIGN KEY (`client_id`) REFERENCES `CLIENT` (`client_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_project_planned_period` CHECK (planned_end_date >= actual_start_date),
  CONSTRAINT `chk_project_actual_period` CHECK (actual_end_date IS NULL OR actual_end_date >= actual_start_date),
  CONSTRAINT `chk_project_evaluation_received` CHECK (evaluation_received_date IS NULL OR (evaluation_requested_date IS NOT NULL AND evaluation_received_date >= evaluation_requested_date)),
  CONSTRAINT `chk_project_evaluation_reported` CHECK (evaluation_reported_date IS NULL OR (evaluation_received_date IS NOT NULL AND evaluation_reported_date >= evaluation_received_date))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PROJECT / ERD v1.5';

CREATE TABLE `PROJECT_MEMBER` (
  `member_id` INT NOT NULL AUTO_INCREMENT COMMENT '프로젝트 구성원 식별자',
  `project_id` INT NOT NULL COMMENT '프로젝트 식별자',
  `employee_id` INT NOT NULL COMMENT '직원 식별자',
  `project_role` VARCHAR(30) NOT NULL COMMENT '프로젝트 수행 역할: 최초 등록 후 고정',
  `assignment_start_date` DATE NULL COMMENT '실제 투입 시작일: 투입 전 NULL',
  `assignment_end_date` DATE NULL COMMENT '실제 투입 종료일: 참여 중 NULL',
  PRIMARY KEY (`member_id`),
  CONSTRAINT `uq_project_member` UNIQUE (`project_id`, `employee_id`),
  CONSTRAINT `fk_member_project` FOREIGN KEY (`project_id`) REFERENCES `PROJECT` (`project_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_member_employee` FOREIGN KEY (`employee_id`) REFERENCES `EMPLOYEE` (`employee_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_member_project_role` CHECK (project_role IN ('PM', 'PL', 'ANALYST', 'DESIGNER', 'PROGRAMMER', 'TESTER')),
  CONSTRAINT `chk_member_assignment_period` CHECK (assignment_end_date IS NULL OR (assignment_start_date IS NOT NULL AND assignment_end_date >= assignment_start_date))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PROJECT_MEMBER / ERD v1.5';

CREATE TABLE `EVALUATION` (
  `evaluation_id` INT NOT NULL AUTO_INCREMENT COMMENT '평가 식별자',
  `target_member_id` INT NOT NULL COMMENT '피평가 구성원 식별자',
  `evaluation_type` VARCHAR(20) NOT NULL COMMENT '평가유형',
  `evaluator_member_id` INT NULL COMMENT '내부 평가자: 고객평가는 NULL',
  `registered_by_member_id` INT NOT NULL COMMENT '시스템 등록자 구성원 식별자',
  `evaluated_date` DATE NOT NULL COMMENT '실제 평가일',
  `work_performance_score` INT NOT NULL COMMENT '업무수행 점수: 1~5',
  `work_performance_comment` TEXT NOT NULL COMMENT '업무수행 평가내용',
  `communication_score` INT NOT NULL COMMENT '커뮤니케이션 점수: 1~5',
  `communication_comment` TEXT NOT NULL COMMENT '커뮤니케이션 평가내용',
  PRIMARY KEY (`evaluation_id`),
  CONSTRAINT `uq_internal_evaluation` UNIQUE (`target_member_id`, `evaluation_type`, `evaluator_member_id`),
  CONSTRAINT `fk_evaluation_target` FOREIGN KEY (`target_member_id`) REFERENCES `PROJECT_MEMBER` (`member_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_evaluation_evaluator` FOREIGN KEY (`evaluator_member_id`) REFERENCES `PROJECT_MEMBER` (`member_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_evaluation_registrant` FOREIGN KEY (`registered_by_member_id`) REFERENCES `PROJECT_MEMBER` (`member_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_evaluation_type` CHECK (evaluation_type IN ('CLIENT', 'PM', 'PEER')),
  CONSTRAINT `chk_evaluation_evaluator` CHECK ((evaluation_type = 'CLIENT' AND evaluator_member_id IS NULL) OR (evaluation_type IN ('PM', 'PEER') AND evaluator_member_id IS NOT NULL)),
  CONSTRAINT `chk_evaluation_no_self` CHECK (evaluator_member_id IS NULL OR evaluator_member_id <> target_member_id),
  CONSTRAINT `chk_evaluation_registrant` CHECK (evaluation_type = 'CLIENT' OR registered_by_member_id = evaluator_member_id),
  CONSTRAINT `chk_evaluation_work_performance_score` CHECK (work_performance_score BETWEEN 1 AND 5),
  CONSTRAINT `chk_evaluation_communication_score` CHECK (communication_score BETWEEN 1 AND 5),
  CONSTRAINT `chk_evaluation_work_performance_comment` CHECK (CHAR_LENGTH(TRIM(work_performance_comment)) > 0),
  CONSTRAINT `chk_evaluation_communication_comment` CHECK (CHAR_LENGTH(TRIM(communication_comment)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='EVALUATION / ERD v1.5';
