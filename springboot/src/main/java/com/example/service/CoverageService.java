package com.example.service;

import com.example.entity.CoverageResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CoverageService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String STATUS_TEST_FAILED = "TEST_FAILED";

    private static final Pattern PYTHON_CODE_BLOCK = Pattern.compile("(?is)```\\s*(?:python|py)\\s*\\R?(.*?)```");
    private static final Pattern ANY_CODE_BLOCK = Pattern.compile("(?is)```\\s*\\R?(.*?)```");
    private static final Pattern TEST_FUNCTION_PATTERN = Pattern.compile("(?m)^\\s*def\\s+test_\\w*\\s*\\(");
    private static final Pattern TEST_CLASS_PATTERN = Pattern.compile("(?m)^\\s*class\\s+Test\\w*\\s*(?:\\(|:)");
    private static final Pattern ASSERT_LINE_PATTERN = Pattern.compile("(?m)^\\s*assert\\s+.+");
    private static final Pattern IMPORT_UNITTEST_PATTERN = Pattern.compile("(?m)^\\s*import\\s+unittest\\b");
    private static final Pattern IMPORT_PYTEST_PATTERN = Pattern.compile("(?m)^\\s*import\\s+pytest\\b");
    private static final Pattern UNITTEST_ERROR_SUMMARY_PATTERN = Pattern.compile("(?i)errors=(\\d+)");
    private static final Pattern UNITTEST_FAILURE_SUMMARY_PATTERN = Pattern.compile("(?i)failures=(\\d+)");

    @Value("${coverage.python-command:python}")
    private String pythonCommand;

    @Value("${coverage.timeout-seconds:10}")
    private long timeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CoverageResult runPythonCoverage(String functionCode, String generatedText) {
        String targetCode = extractPythonSourceCode(functionCode);
        if (targetCode.isEmpty()) {
            return skipped("函数代码为空，未执行 coverage.py 覆盖率统计");
        }

        String testCode = extractPythonTestCode(generatedText);
        if (testCode.isEmpty()) {
            return skipped("生成结果不是可执行 Python 测试代码");
        }

        Path tempDir = null;
        try {
            Path baseDir = Paths.get(System.getProperty("java.io.tmpdir"), "llm-test-coverage");
            Files.createDirectories(baseDir);
            tempDir = baseDir.resolve(UUID.randomUUID().toString());
            Files.createDirectories(tempDir);

            Files.writeString(tempDir.resolve("target_module.py"), targetCode + System.lineSeparator(), StandardCharsets.UTF_8);
            Files.writeString(tempDir.resolve("test_target.py"), buildTestFileContent(testCode), StandardCharsets.UTF_8);

            CommandResult runResult = runCommand(tempDir, buildPythonCommand("-m", "coverage", "run", "--branch", "test_target.py"));
            if (runResult.timedOut()) {
                return failed("覆盖率统计超时");
            }

            String finalStatus = STATUS_SUCCESS;
            String finalMessage = "覆盖率统计成功";
            if (runResult.exitCode() != 0) {
                String runOutput = firstNonBlank(runResult.stderr(), runResult.stdout(), "未知错误");
                if (!hasCoverageData(tempDir)) {
                    return failed("coverage run 执行失败，未生成覆盖率数据：" + runOutput);
                }
                if (isFatalTestExecutionFailure(runOutput)) {
                    return failed("测试代码无法执行，未保存覆盖率：" + runOutput);
                }
                finalStatus = STATUS_TEST_FAILED;
                finalMessage = buildTestFailedMessage(runOutput);
            }

            CommandResult jsonResult = runCommand(tempDir, buildPythonCommand("-m", "coverage", "json", "-o", "coverage.json"));
            if (jsonResult.timedOut()) {
                return failed("覆盖率统计超时");
            }
            if (jsonResult.exitCode() != 0) {
                return failed("coverage json 执行失败：" + firstNonBlank(jsonResult.stderr(), jsonResult.stdout(), "未知错误"));
            }

            return parseCoverageJson(tempDir.resolve("coverage.json"), finalStatus, finalMessage);
        } catch (Exception e) {
            return failed("覆盖率统计失败：" + nullToEmpty(e.getMessage()));
        } finally {
            deleteDirectoryQuietly(tempDir);
        }
    }

    private String extractPythonSourceCode(String functionCode) {
        String text = nullToEmpty(functionCode).trim();
        if (text.isEmpty()) {
            return "";
        }

        Matcher matcher = PYTHON_CODE_BLOCK.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return text
                .replaceAll("(?is)^```\\s*(?:python|py)?\\s*", "")
                .replaceAll("(?is)```\\s*$", "")
                .trim();
    }

    private String extractPythonTestCode(String generatedText) {
        String text = nullToEmpty(generatedText).trim();
        if (text.isEmpty()) {
            return "";
        }

        Matcher pythonBlockMatcher = PYTHON_CODE_BLOCK.matcher(text);
        while (pythonBlockMatcher.find()) {
            String candidate = pythonBlockMatcher.group(1).trim();
            if (looksLikeExecutablePythonTest(candidate)) {
                return candidate;
            }
        }

        Matcher anyBlockMatcher = ANY_CODE_BLOCK.matcher(text);
        while (anyBlockMatcher.find()) {
            String candidate = removeOptionalFenceLanguage(anyBlockMatcher.group(1).trim());
            if (looksLikeExecutablePythonTest(candidate)) {
                return candidate;
            }
        }

        String inlineCandidate = extractInlinePythonTest(text);
        if (looksLikeExecutablePythonTest(inlineCandidate)) {
            return inlineCandidate;
        }

        return "";
    }

    private String removeOptionalFenceLanguage(String text) {
        String normalized = nullToEmpty(text).replace("\r\n", "\n").replace("\r", "\n").trim();
        int firstLineEnd = normalized.indexOf('\n');
        if (firstLineEnd <= 0) {
            return normalized;
        }

        String firstLine = normalized.substring(0, firstLineEnd).trim();
        if (firstLine.matches("(?i)^[a-z0-9_+.-]{1,30}$")) {
            return normalized.substring(firstLineEnd + 1).trim();
        }
        return normalized;
    }

    private String extractInlinePythonTest(String text) {
        String normalized = nullToEmpty(text).replace("\r\n", "\n").replace("\r", "\n");
        int start = earliestIndex(normalized,
                "\nimport unittest",
                "import unittest",
                "\nfrom unittest",
                "from unittest",
                "\nimport pytest",
                "import pytest",
                "\ndef test_",
                "def test_",
                "\nclass Test",
                "class Test",
                "\nassert ",
                "assert ");
        if (start < 0) {
            return "";
        }

        return normalized.substring(start).trim();
    }

    private int earliestIndex(String text, String... needles) {
        int result = -1;
        for (String needle : needles) {
            int index = text.indexOf(needle);
            if (index >= 0 && (result < 0 || index < result)) {
                result = index;
            }
        }
        return result;
    }

    private boolean looksLikeExecutablePythonTest(String code) {
        String text = nullToEmpty(code);
        if (text.trim().isEmpty()) {
            return false;
        }

        String lower = text.toLowerCase();
        return IMPORT_UNITTEST_PATTERN.matcher(text).find()
                || lower.contains("unittest.")
                || IMPORT_PYTEST_PATTERN.matcher(text).find()
                || lower.contains("pytest.")
                || TEST_FUNCTION_PATTERN.matcher(text).find()
                || TEST_CLASS_PATTERN.matcher(text).find()
                || ASSERT_LINE_PATTERN.matcher(text).find();
    }

    private String buildTestFileContent(String testCode) {
        String normalized = nullToEmpty(testCode).trim();
        StringBuilder builder = new StringBuilder();
        builder.append("from target_module import *").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append(normalized).append(System.lineSeparator());

        String lower = normalized.toLowerCase();
        boolean hasMainGuard = lower.contains("__main__");
        if (!hasMainGuard && (lower.contains("unittest") || TEST_CLASS_PATTERN.matcher(normalized).find())) {
            builder.append(System.lineSeparator())
                    .append("if __name__ == \"__main__\":").append(System.lineSeparator())
                    .append("    unittest.main()").append(System.lineSeparator());
        } else if (!hasMainGuard && (lower.contains("pytest") || TEST_FUNCTION_PATTERN.matcher(normalized).find())) {
            if (!IMPORT_PYTEST_PATTERN.matcher(normalized).find()) {
                builder.append(System.lineSeparator()).append("import pytest").append(System.lineSeparator());
            }
            builder.append(System.lineSeparator())
                    .append("if __name__ == \"__main__\":").append(System.lineSeparator())
                    .append("    raise SystemExit(pytest.main([__file__]))").append(System.lineSeparator());
        }

        return builder.toString();
    }

    private List<String> buildPythonCommand(String... args) {
        List<String> command = new ArrayList<>(splitCommand(nullToEmpty(pythonCommand).trim()));
        if (command.isEmpty()) {
            command.add("python");
        }
        command.addAll(List.of(args));
        return command;
    }

    private List<String> splitCommand(String commandText) {
        List<String> result = new ArrayList<>();
        if (commandText == null || commandText.isBlank()) {
            return result;
        }

        Matcher matcher = Pattern.compile("\"([^\"]+)\"|'([^']+)'|(\\S+)").matcher(commandText);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                result.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                result.add(matcher.group(2));
            } else {
                result.add(matcher.group(3));
            }
        }
        return result;
    }

    private CommandResult runCommand(Path workDir, List<String> command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workDir.toFile());
        Path stdoutPath = Files.createTempFile(workDir, "coverage-stdout-", ".log");
        Path stderrPath = Files.createTempFile(workDir, "coverage-stderr-", ".log");
        processBuilder.redirectOutput(stdoutPath.toFile());
        processBuilder.redirectError(stderrPath.toFile());

        Process process = processBuilder.start();
        boolean completed = process.waitFor(Math.max(timeoutSeconds, 1), TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            process.waitFor(2, TimeUnit.SECONDS);
            return new CommandResult(-1, "", "", true);
        }

        String stdout = Files.readString(stdoutPath, StandardCharsets.UTF_8);
        String stderr = Files.readString(stderrPath, StandardCharsets.UTF_8);
        return new CommandResult(process.exitValue(), truncate(stdout), truncate(stderr), false);
    }

    private boolean hasCoverageData(Path tempDir) throws IOException {
        Path coverageDataPath = tempDir.resolve(".coverage");
        return Files.exists(coverageDataPath) && Files.size(coverageDataPath) > 0;
    }

    private boolean isFatalTestExecutionFailure(String output) {
        Integer unittestErrors = firstMatchedInt(UNITTEST_ERROR_SUMMARY_PATTERN, output);
        if (unittestErrors != null) {
            return unittestErrors > 0;
        }
        if (isAssertionFailure(output)) {
            return false;
        }

        String lower = nullToEmpty(output).toLowerCase();
        return lower.contains("syntaxerror")
                || lower.contains("indentationerror")
                || lower.contains("taberror")
                || lower.contains("importerror")
                || lower.contains("modulenotfounderror")
                || lower.contains("nameerror")
                || lower.contains("attributeerror")
                || lower.contains("typeerror");
    }

    private boolean isAssertionFailure(String output) {
        String text = nullToEmpty(output);
        Integer failures = firstMatchedInt(UNITTEST_FAILURE_SUMMARY_PATTERN, text);
        return text.contains("AssertionError")
                || text.contains("FAIL:")
                || (failures != null && failures > 0);
    }

    private String buildTestFailedMessage(String runOutput) {
        String summary = summarizeRunFailure(runOutput);
        if (summary.isEmpty()) {
            return "测试代码执行存在断言失败，但已成功统计覆盖率";
        }
        return "测试代码执行存在断言失败，但已成功统计覆盖率。失败信息：" + summary;
    }

    private String summarizeRunFailure(String output) {
        String text = nullToEmpty(output).replace("\r", "").trim();
        if (text.isEmpty()) {
            return "";
        }

        Integer failures = firstMatchedInt(UNITTEST_FAILURE_SUMMARY_PATTERN, text);
        if (failures != null && failures > 0) {
            return truncate(firstNonBlank(lastLineContaining(text, "FAILED ("), firstLineContaining(text, "AssertionError"), text));
        }
        return truncate(firstNonBlank(firstLineContaining(text, "AssertionError"), lastLineContaining(text, "failed"), text));
    }

    private Integer firstMatchedInt(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(nullToEmpty(text));
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String firstLineContaining(String text, String needle) {
        for (String line : nullToEmpty(text).split("\\n")) {
            if (line.contains(needle)) {
                return line.trim();
            }
        }
        return "";
    }

    private String lastLineContaining(String text, String needle) {
        String result = "";
        for (String line : nullToEmpty(text).split("\\n")) {
            if (line.contains(needle)) {
                result = line.trim();
            }
        }
        return result;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return nullToEmpty(value).trim().isEmpty() ? defaultValue : value;
    }

    private CoverageResult parseCoverageJson(Path coverageJsonPath, String status, String baseMessage) throws IOException {
        if (!Files.exists(coverageJsonPath)) {
            return failed("coverage.json 未生成");
        }

        JsonNode root = objectMapper.readTree(Files.readString(coverageJsonPath, StandardCharsets.UTF_8));
        JsonNode totals = root.path("totals");
        if (totals.isMissingNode() || !totals.hasNonNull("percent_covered")) {
            return failed("coverage.json 未包含 totals.percent_covered");
        }

        Double lineCoverage = roundCoverage(totals.path("percent_covered").asDouble());
        int numBranches = totals.path("num_branches").asInt(0);
        Double branchCoverage = null;
        String message = defaultIfBlank(baseMessage, "覆盖率统计成功");
        if (numBranches > 0) {
            int coveredBranches = totals.path("covered_branches").asInt(0);
            branchCoverage = roundCoverage(coveredBranches * 100.0 / numBranches);
        } else {
            message = message + "，目标代码未检测到可统计分支";
        }

        CoverageResult result = new CoverageResult();
        result.setSuccess(true);
        result.setStatus(defaultIfBlank(status, STATUS_SUCCESS));
        result.setLineCoverage(lineCoverage);
        result.setBranchCoverage(branchCoverage);
        result.setMessage(message);
        return result;
    }

    private Double roundCoverage(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private CoverageResult skipped(String message) {
        CoverageResult result = new CoverageResult();
        result.setSuccess(false);
        result.setStatus(STATUS_SKIPPED);
        result.setMessage(message);
        return result;
    }

    private CoverageResult failed(String message) {
        CoverageResult result = new CoverageResult();
        result.setSuccess(false);
        result.setStatus(STATUS_FAILED);
        result.setMessage(truncate(message));
        return result;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String text = nullToEmpty(value).trim();
            if (!text.isEmpty()) {
                return truncate(text);
            }
        }
        return "";
    }

    private String truncate(String value) {
        String text = nullToEmpty(value).trim();
        int maxLength = 4000;
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private void deleteDirectoryQuietly(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }

        try {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record CommandResult(int exitCode, String stdout, String stderr, boolean timedOut) {
    }
}
