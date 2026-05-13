CREATE TABLE IF NOT EXISTS generate_record (
    id INT NOT NULL AUTO_INCREMENT,
    function_id INT DEFAULT NULL,
    function_name VARCHAR(255) DEFAULT NULL,
    source_type VARCHAR(50) DEFAULT 'function',
    prompt_text LONGTEXT,
    result_text LONGTEXT,
    model_name VARCHAR(100) DEFAULT NULL,
    prompt_tokens INT DEFAULT NULL,
    completion_tokens INT DEFAULT NULL,
    total_tokens INT DEFAULT NULL,
    latency_ms BIGINT DEFAULT NULL,
    strategy VARCHAR(50) DEFAULT NULL,
    run_index INT DEFAULT NULL,
    previous_record_id INT DEFAULT NULL,
    creator_id INT DEFAULT NULL,
    creator_role VARCHAR(20) DEFAULT NULL,
    manager_id INT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    evaluation_score INT DEFAULT NULL,
    tester_case_count INT DEFAULT NULL,
    llm_case_count INT DEFAULT NULL,
    tester_evaluation_score INT DEFAULT NULL,
    line_coverage DOUBLE DEFAULT NULL COMMENT '语句覆盖率',
    branch_coverage DOUBLE DEFAULT NULL COMMENT '分支覆盖率',
    coverage_status VARCHAR(50) DEFAULT NULL COMMENT '覆盖率统计状态',
    coverage_message TEXT DEFAULT NULL COMMENT '覆盖率统计说明',
    public_compare_result LONGTEXT COMMENT '公开测试基准覆盖分析结果',
    public_covered_count INT DEFAULT 0 COMMENT '已覆盖公开断言数量',
    public_partial_count INT DEFAULT 0 COMMENT '部分覆盖公开断言数量',
    public_missing_count INT DEFAULT 0 COMMENT '缺失公开断言数量',
    public_extra_result LONGTEXT COMMENT '生成用例增量价值分析结果',
    generated_case_count INT DEFAULT 0 COMMENT '大模型生成用例数量',
    public_matched_case_count INT DEFAULT 0 COMMENT '与公开测试基准匹配的生成用例数量',
    public_extra_case_count INT DEFAULT 0 COMMENT '公开测试基准外新增有效测试点数量',
    public_invalid_case_count INT DEFAULT 0 COMMENT '无效或重复测试点数量',
    public_extra_rate DECIMAL(6,2) DEFAULT 0 COMMENT '增量补充率',
    public_expand_rate DECIMAL(6,2) DEFAULT 0 COMMENT '覆盖扩展率',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE generate_record ADD COLUMN function_id INT NULL;
ALTER TABLE generate_record ADD COLUMN function_name VARCHAR(255) NULL;
ALTER TABLE generate_record ADD COLUMN source_type VARCHAR(50) NULL DEFAULT 'function';
ALTER TABLE generate_record ADD COLUMN prompt_text LONGTEXT NULL;
ALTER TABLE generate_record ADD COLUMN result_text LONGTEXT NULL;
ALTER TABLE generate_record ADD COLUMN model_name VARCHAR(100) NULL;
ALTER TABLE generate_record ADD COLUMN prompt_tokens INT NULL;
ALTER TABLE generate_record ADD COLUMN completion_tokens INT NULL;
ALTER TABLE generate_record ADD COLUMN total_tokens INT NULL;
ALTER TABLE generate_record ADD COLUMN latency_ms BIGINT NULL;
ALTER TABLE generate_record ADD COLUMN strategy VARCHAR(50) NULL;
ALTER TABLE generate_record ADD COLUMN run_index INT NULL;
ALTER TABLE generate_record ADD COLUMN previous_record_id INT NULL;
ALTER TABLE generate_record ADD COLUMN creator_id INT NULL;
ALTER TABLE generate_record ADD COLUMN creator_role VARCHAR(20) NULL;
ALTER TABLE generate_record ADD COLUMN manager_id INT NULL;
ALTER TABLE generate_record ADD COLUMN create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE generate_record ADD COLUMN evaluation_score INT NULL;
ALTER TABLE generate_record ADD COLUMN tester_case_count INT NULL;
ALTER TABLE generate_record ADD COLUMN llm_case_count INT NULL;
ALTER TABLE generate_record ADD COLUMN tester_evaluation_score INT NULL;
ALTER TABLE generate_record ADD COLUMN line_coverage DOUBLE NULL COMMENT '语句覆盖率';
ALTER TABLE generate_record ADD COLUMN branch_coverage DOUBLE NULL COMMENT '分支覆盖率';
ALTER TABLE generate_record ADD COLUMN coverage_status VARCHAR(50) NULL COMMENT '覆盖率统计状态';
ALTER TABLE generate_record ADD COLUMN coverage_message TEXT NULL COMMENT '覆盖率统计说明';

ALTER TABLE generate_record
    MODIFY COLUMN prompt_text LONGTEXT NULL,
    MODIFY COLUMN result_text LONGTEXT NULL;
