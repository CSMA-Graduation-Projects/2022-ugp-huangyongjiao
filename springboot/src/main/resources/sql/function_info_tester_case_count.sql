CREATE TABLE IF NOT EXISTS function_info (
    id INT NOT NULL AUTO_INCREMENT,
    function_name VARCHAR(255) DEFAULT NULL,
    class_name VARCHAR(255) DEFAULT NULL,
    language VARCHAR(50) DEFAULT NULL,
    code_text TEXT,
    input_desc TEXT,
    output_desc TEXT,
    remark VARCHAR(255) DEFAULT NULL,
    tester_case_count INT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    creator_id INT DEFAULT NULL,
    creator_role VARCHAR(20) DEFAULT NULL,
    manager_id INT DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE function_info ADD COLUMN function_name VARCHAR(255) NULL;
ALTER TABLE function_info ADD COLUMN class_name VARCHAR(255) NULL;
ALTER TABLE function_info ADD COLUMN language VARCHAR(50) NULL;
ALTER TABLE function_info ADD COLUMN code_text TEXT NULL;
ALTER TABLE function_info ADD COLUMN input_desc TEXT NULL;
ALTER TABLE function_info ADD COLUMN output_desc TEXT NULL;
ALTER TABLE function_info ADD COLUMN remark VARCHAR(255) NULL;
ALTER TABLE function_info ADD COLUMN tester_case_count INT NULL;
ALTER TABLE function_info ADD COLUMN create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE function_info ADD COLUMN creator_id INT NULL;
ALTER TABLE function_info ADD COLUMN creator_role VARCHAR(20) NULL;
ALTER TABLE function_info ADD COLUMN manager_id INT NULL;
