ALTER TABLE generate_record ADD COLUMN normal_path_coverage TEXT NULL;
ALTER TABLE generate_record ADD COLUMN boundary_coverage TEXT NULL;
ALTER TABLE generate_record ADD COLUMN exception_coverage TEXT NULL;
ALTER TABLE generate_record ADD COLUMN syntax_norm TEXT NULL;
ALTER TABLE generate_record ADD COLUMN suggestion_text LONGTEXT NULL;
ALTER TABLE generate_record ADD COLUMN line_coverage DOUBLE NULL COMMENT '语句覆盖率';
ALTER TABLE generate_record ADD COLUMN branch_coverage DOUBLE NULL COMMENT '分支覆盖率';
ALTER TABLE generate_record ADD COLUMN coverage_status VARCHAR(50) NULL COMMENT '覆盖率统计状态';
ALTER TABLE generate_record ADD COLUMN coverage_message TEXT NULL COMMENT '覆盖率统计说明';

ALTER TABLE generate_record
    MODIFY COLUMN suggestion_text LONGTEXT NULL;
