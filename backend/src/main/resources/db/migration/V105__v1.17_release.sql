-- permission
INSERT INTO user_group_permission (id, group_id, permission_id, module_id)
VALUES ('36c05551-5195-4cb8-98d4-737f15ffe0bb', 'project_admin', 'PROJECT_VERSION:READ+DELETE', 'PROJECT_VERSION');
INSERT INTO user_group_permission (id, group_id, permission_id, module_id)
VALUES ('4783870f-c29c-4b00-9797-be618b4464a2', 'project_admin', 'PROJECT_VERSION:READ+ENABLE', 'PROJECT_VERSION');
INSERT INTO user_group_permission (id, group_id, permission_id, module_id)
VALUES ('7396b1f2-2ed4-4582-bbd8-8d721dac96fa', 'project_admin', 'PROJECT_VERSION:READ+CREATE', 'PROJECT_VERSION');
INSERT INTO user_group_permission (id, group_id, permission_id, module_id)
VALUES ('75a35739-832d-4edf-8bba-f19e46d9a8df', 'project_admin', 'PROJECT_VERSION:READ', 'PROJECT_VERSION');
INSERT INTO user_group_permission (id, group_id, permission_id, module_id)
VALUES ('8d0ba6b9-938c-4e94-b60f-df791b36f56c', 'project_admin', 'PROJECT_VERSION:READ+EDIT', 'PROJECT_VERSION');

-- version
CREATE TABLE IF NOT EXISTS `project_version`
(
    `id`           varchar(50) NOT NULL,
    `project_id`   varchar(50)  DEFAULT NULL,
    `name`         varchar(100) DEFAULT NULL,
    `description`  varchar(200) DEFAULT NULL,
    `status`       varchar(20)  DEFAULT NULL,
    `latest`       tinyint(1)   DEFAULT NULL,
    `publish_time` bigint(13)   DEFAULT NULL,
    `start_time`   bigint(13)   DEFAULT NULL,
    `end_time`     bigint(13)   DEFAULT NULL,
    `create_time`  bigint(13)   DEFAULT NULL,
    `create_user`  varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `project_version_project_id_index` (`project_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

ALTER TABLE project
    ADD version_enable TINYINT(1) DEFAULT 1 NULL;


INSERT INTO project_version (id, name, description, status, latest, publish_time, start_time, end_time, create_time,
                             create_user, project_id)
SELECT UUID(),
       'v1.0.0',
       '系统默认版本',
       'open',
       TRUE,
       UNIX_TIMESTAMP() * 1000,
       UNIX_TIMESTAMP() * 1000,
       UNIX_TIMESTAMP() * 1000,
       UNIX_TIMESTAMP() * 1000,
       'admin',
       id
FROM project;

-- api definition
ALTER TABLE api_definition
    ADD version_id VARCHAR(50) NULL;

ALTER TABLE api_definition
    ADD ref_id VARCHAR(50) NULL;

ALTER TABLE api_definition_exec_result
    ADD version_id VARCHAR(50) NULL;

CREATE INDEX api_definition_exec_result_version_id_index
    ON api_definition_exec_result (version_id);

CREATE INDEX api_definition_ref_id_index
    ON api_definition (ref_id);

CREATE INDEX api_definition_version_id_index
    ON api_definition (version_id);

UPDATE api_definition
SET ref_id = id;

UPDATE api_definition
    INNER JOIN project_version ON project_version.project_id = api_definition.project_id
SET version_id = project_version.id;

-- api test case
ALTER TABLE api_test_case
    ADD version_id VARCHAR(50) NULL;

CREATE INDEX api_test_case_version_id_index
    ON api_test_case (version_id);

UPDATE api_test_case
    INNER JOIN project_version ON project_version.project_id = api_test_case.project_id
SET version_id = project_version.id;


UPDATE api_definition_exec_result
    JOIN api_test_case ON resource_id = api_test_case.id
SET api_definition_exec_result.version_id = api_test_case.version_id;

UPDATE api_definition_exec_result
    JOIN api_definition ON resource_id = api_definition.id
SET api_definition_exec_result.version_id = api_definition.version_id;

-- load_test
ALTER TABLE load_test
    ADD version_id VARCHAR(50) NULL;

ALTER TABLE load_test
    ADD ref_id VARCHAR(50) NULL;

ALTER TABLE load_test_report
    ADD version_id VARCHAR(50) NULL;

CREATE INDEX load_test_report_version_id_index
    ON load_test_report (version_id);

CREATE INDEX load_test_ref_id_index
    ON load_test (ref_id);

CREATE INDEX load_test_version_id_index
    ON load_test (version_id);

UPDATE load_test
SET ref_id = id;

UPDATE load_test
    INNER JOIN project_version ON project_version.project_id = load_test.project_id
SET version_id = project_version.id;

UPDATE load_test_report
    INNER JOIN project_version ON project_version.project_id = load_test_report.project_id
SET version_id = project_version.id;

-- api scenario
ALTER TABLE api_scenario
    ADD version_id VARCHAR(50) NULL;

ALTER TABLE api_scenario
    ADD ref_id VARCHAR(255) NULL;

ALTER TABLE api_scenario_report
    ADD version_id VARCHAR(50) NULL;

CREATE INDEX api_scenario_report_version_id_index
    ON api_scenario_report (version_id);

CREATE INDEX api_scenario_ref_id_index
    ON api_scenario (ref_id);

CREATE INDEX api_scenario_version_id_index
    ON api_scenario (version_id);

UPDATE api_scenario
SET ref_id = id;

UPDATE api_scenario
    INNER JOIN project_version ON project_version.project_id = api_scenario.project_id
SET version_id = project_version.id;

UPDATE api_scenario_report
    INNER JOIN project_version ON project_version.project_id = api_scenario_report.project_id
SET version_id = project_version.id;