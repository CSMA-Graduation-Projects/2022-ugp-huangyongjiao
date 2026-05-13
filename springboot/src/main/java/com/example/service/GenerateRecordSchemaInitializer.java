package com.example.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GenerateRecordSchemaInitializer {

    private static final String GENERATE_RECORD_TABLE = """
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
                normal_path_coverage TEXT,
                boundary_coverage TEXT,
                exception_coverage TEXT,
                syntax_norm TEXT,
                suggestion_text LONGTEXT,
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final String FUNCTION_INFO_TABLE = """
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final List<ColumnDefinition> GENERATE_RECORD_COLUMNS = List.of(
            new ColumnDefinition("function_id",
                    "ALTER TABLE generate_record ADD COLUMN function_id INT NULL"),
            new ColumnDefinition("function_name",
                    "ALTER TABLE generate_record ADD COLUMN function_name VARCHAR(255) NULL"),
            new ColumnDefinition("source_type",
                    "ALTER TABLE generate_record ADD COLUMN source_type VARCHAR(50) NULL DEFAULT 'function'"),
            new ColumnDefinition("prompt_text",
                    "ALTER TABLE generate_record ADD COLUMN prompt_text LONGTEXT NULL",
                    "longtext",
                    "ALTER TABLE generate_record MODIFY COLUMN prompt_text LONGTEXT NULL"),
            new ColumnDefinition("result_text",
                    "ALTER TABLE generate_record ADD COLUMN result_text LONGTEXT NULL",
                    "longtext",
                    "ALTER TABLE generate_record MODIFY COLUMN result_text LONGTEXT NULL"),
            new ColumnDefinition("model_name",
                    "ALTER TABLE generate_record ADD COLUMN model_name VARCHAR(100) NULL"),
            new ColumnDefinition("prompt_tokens",
                    "ALTER TABLE generate_record ADD COLUMN prompt_tokens INT NULL"),
            new ColumnDefinition("completion_tokens",
                    "ALTER TABLE generate_record ADD COLUMN completion_tokens INT NULL"),
            new ColumnDefinition("total_tokens",
                    "ALTER TABLE generate_record ADD COLUMN total_tokens INT NULL"),
            new ColumnDefinition("latency_ms",
                    "ALTER TABLE generate_record ADD COLUMN latency_ms BIGINT NULL"),
            new ColumnDefinition("strategy",
                    "ALTER TABLE generate_record ADD COLUMN strategy VARCHAR(50) NULL"),
            new ColumnDefinition("run_index",
                    "ALTER TABLE generate_record ADD COLUMN run_index INT NULL"),
            new ColumnDefinition("previous_record_id",
                    "ALTER TABLE generate_record ADD COLUMN previous_record_id INT NULL"),
            new ColumnDefinition("creator_id",
                    "ALTER TABLE generate_record ADD COLUMN creator_id INT NULL"),
            new ColumnDefinition("creator_role",
                    "ALTER TABLE generate_record ADD COLUMN creator_role VARCHAR(20) NULL"),
            new ColumnDefinition("manager_id",
                    "ALTER TABLE generate_record ADD COLUMN manager_id INT NULL"),
            new ColumnDefinition("create_time",
                    "ALTER TABLE generate_record ADD COLUMN create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP"),
            new ColumnDefinition("evaluation_score",
                    "ALTER TABLE generate_record ADD COLUMN evaluation_score INT NULL"),
            new ColumnDefinition("normal_path_coverage",
                    "ALTER TABLE generate_record ADD COLUMN normal_path_coverage TEXT NULL"),
            new ColumnDefinition("boundary_coverage",
                    "ALTER TABLE generate_record ADD COLUMN boundary_coverage TEXT NULL"),
            new ColumnDefinition("exception_coverage",
                    "ALTER TABLE generate_record ADD COLUMN exception_coverage TEXT NULL"),
            new ColumnDefinition("syntax_norm",
                    "ALTER TABLE generate_record ADD COLUMN syntax_norm TEXT NULL"),
            new ColumnDefinition("suggestion_text",
                    "ALTER TABLE generate_record ADD COLUMN suggestion_text LONGTEXT NULL",
                    "longtext",
                    "ALTER TABLE generate_record MODIFY COLUMN suggestion_text LONGTEXT NULL"),
            new ColumnDefinition("tester_case_count",
                    "ALTER TABLE generate_record ADD COLUMN tester_case_count INT NULL"),
            new ColumnDefinition("llm_case_count",
                    "ALTER TABLE generate_record ADD COLUMN llm_case_count INT NULL"),
            new ColumnDefinition("tester_evaluation_score",
                    "ALTER TABLE generate_record ADD COLUMN tester_evaluation_score INT NULL"),
            new ColumnDefinition("line_coverage",
                    "ALTER TABLE generate_record ADD COLUMN line_coverage DOUBLE NULL COMMENT '语句覆盖率'"),
            new ColumnDefinition("branch_coverage",
                    "ALTER TABLE generate_record ADD COLUMN branch_coverage DOUBLE NULL COMMENT '分支覆盖率'"),
            new ColumnDefinition("coverage_status",
                    "ALTER TABLE generate_record ADD COLUMN coverage_status VARCHAR(50) NULL COMMENT '覆盖率统计状态'"),
            new ColumnDefinition("coverage_message",
                    "ALTER TABLE generate_record ADD COLUMN coverage_message TEXT NULL COMMENT '覆盖率统计说明'"),
            new ColumnDefinition("public_compare_result",
                    "ALTER TABLE generate_record ADD COLUMN public_compare_result LONGTEXT NULL COMMENT '公开测试基准覆盖分析结果'",
                    "longtext",
                    "ALTER TABLE generate_record MODIFY COLUMN public_compare_result LONGTEXT NULL COMMENT '公开测试基准覆盖分析结果'"),
            new ColumnDefinition("public_covered_count",
                    "ALTER TABLE generate_record ADD COLUMN public_covered_count INT DEFAULT 0 COMMENT '已覆盖公开断言数量'"),
            new ColumnDefinition("public_partial_count",
                    "ALTER TABLE generate_record ADD COLUMN public_partial_count INT DEFAULT 0 COMMENT '部分覆盖公开断言数量'"),
            new ColumnDefinition("public_missing_count",
                    "ALTER TABLE generate_record ADD COLUMN public_missing_count INT DEFAULT 0 COMMENT '缺失公开断言数量'"),
            new ColumnDefinition("public_extra_result",
                    "ALTER TABLE generate_record ADD COLUMN public_extra_result LONGTEXT NULL COMMENT '生成用例增量价值分析结果'",
                    "longtext",
                    "ALTER TABLE generate_record MODIFY COLUMN public_extra_result LONGTEXT NULL COMMENT '生成用例增量价值分析结果'"),
            new ColumnDefinition("generated_case_count",
                    "ALTER TABLE generate_record ADD COLUMN generated_case_count INT DEFAULT 0 COMMENT '大模型生成用例数量'"),
            new ColumnDefinition("public_matched_case_count",
                    "ALTER TABLE generate_record ADD COLUMN public_matched_case_count INT DEFAULT 0 COMMENT '与公开测试基准匹配的生成用例数量'"),
            new ColumnDefinition("public_extra_case_count",
                    "ALTER TABLE generate_record ADD COLUMN public_extra_case_count INT DEFAULT 0 COMMENT '公开测试基准外新增有效测试点数量'"),
            new ColumnDefinition("public_invalid_case_count",
                    "ALTER TABLE generate_record ADD COLUMN public_invalid_case_count INT DEFAULT 0 COMMENT '无效或重复测试点数量'"),
            new ColumnDefinition("public_extra_rate",
                    "ALTER TABLE generate_record ADD COLUMN public_extra_rate DECIMAL(6,2) DEFAULT 0 COMMENT '增量补充率'"),
            new ColumnDefinition("public_expand_rate",
                    "ALTER TABLE generate_record ADD COLUMN public_expand_rate DECIMAL(6,2) DEFAULT 0 COMMENT '覆盖扩展率'")
    );

    private static final List<ColumnDefinition> FUNCTION_INFO_COLUMNS = List.of(
            new ColumnDefinition("function_name",
                    "ALTER TABLE function_info ADD COLUMN function_name VARCHAR(255) NULL"),
            new ColumnDefinition("class_name",
                    "ALTER TABLE function_info ADD COLUMN class_name VARCHAR(255) NULL"),
            new ColumnDefinition("language",
                    "ALTER TABLE function_info ADD COLUMN language VARCHAR(50) NULL"),
            new ColumnDefinition("code_text",
                    "ALTER TABLE function_info ADD COLUMN code_text TEXT NULL"),
            new ColumnDefinition("input_desc",
                    "ALTER TABLE function_info ADD COLUMN input_desc TEXT NULL"),
            new ColumnDefinition("output_desc",
                    "ALTER TABLE function_info ADD COLUMN output_desc TEXT NULL"),
            new ColumnDefinition("remark",
                    "ALTER TABLE function_info ADD COLUMN remark VARCHAR(255) NULL"),
            new ColumnDefinition("tester_case_count",
                    "ALTER TABLE function_info ADD COLUMN tester_case_count INT NULL"),
            new ColumnDefinition("create_time",
                    "ALTER TABLE function_info ADD COLUMN create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP"),
            new ColumnDefinition("creator_id",
                    "ALTER TABLE function_info ADD COLUMN creator_id INT NULL"),
            new ColumnDefinition("creator_role",
                    "ALTER TABLE function_info ADD COLUMN creator_role VARCHAR(20) NULL"),
            new ColumnDefinition("manager_id",
                    "ALTER TABLE function_info ADD COLUMN manager_id INT NULL")
    );

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureSchema() {
        jdbcTemplate.execute(FUNCTION_INFO_TABLE);
        jdbcTemplate.execute(GENERATE_RECORD_TABLE);

        ensureColumns("function_info", FUNCTION_INFO_COLUMNS);
        ensureColumns("generate_record", GENERATE_RECORD_COLUMNS);
    }

    private void ensureColumns(String tableName, List<ColumnDefinition> definitions) {
        for (ColumnDefinition definition : definitions) {
            ensureColumn(tableName, definition.name(), definition.addDdl());
            if (definition.expectedDataType() != null && definition.modifyDdl() != null) {
                ensureColumnType(tableName, definition.name(), definition.expectedDataType(), definition.modifyDdl());
            }
        }
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                columnName
        );

        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.execute(ddl);
    }

    private void ensureColumnType(String tableName, String columnName, String expectedDataType, String ddl) {
        String dataType = jdbcTemplate.query(
                "SELECT data_type FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                rs -> rs.next() ? rs.getString(1) : null,
                tableName,
                columnName
        );

        if (dataType == null || expectedDataType.equalsIgnoreCase(dataType)) {
            return;
        }

        jdbcTemplate.execute(ddl);
    }

    private record ColumnDefinition(String name, String addDdl, String expectedDataType, String modifyDdl) {
        private ColumnDefinition(String name, String addDdl) {
            this(name, addDdl, null, null);
        }
    }
}
