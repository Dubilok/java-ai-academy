package com.javaacademy.platform.ai.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.catalog.entity.Lecture;
import com.javaacademy.platform.catalog.entity.Task;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.common.ApiException;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LookupTaskToolTest {

    TaskRepository taskRepository;
    ObjectMapper objectMapper;
    LookupTaskTool tool;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        objectMapper = new ObjectMapper();
        tool = new LookupTaskTool(taskRepository, objectMapper);
    }

    @Test
    @SneakyThrows
    void execute_validTaskId_returnsFullTaskJson() {
        UUID taskId = UUID.randomUUID();
        Task task = buildTask(
                taskId,
                "Reverse a string",
                "Implement reverseString",
                "MEDIUM",
                "// TODO",
                "// test",
                "// solution",
                100L);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        JsonNode input = objectMapper.createObjectNode().put("taskId", taskId.toString());
        JsonNode result = tool.execute(input);

        assertThat(result.get("id").asText()).isEqualTo(taskId.toString());
        assertThat(result.get("title").asText()).isEqualTo("Reverse a string");
        assertThat(result.get("description").asText()).isEqualTo("Implement reverseString");
        assertThat(result.get("difficulty").asText()).isEqualTo("MEDIUM");
        assertThat(result.get("testCode").asText()).isEqualTo("// test");
        assertThat(result.get("solutionCode").asText()).isEqualTo("// solution");
        assertThat(result.get("xpReward").asLong()).isEqualTo(100L);
    }

    @Test
    @SneakyThrows
    void execute_taskWithNullFields_omitsNullKeys() {
        UUID taskId = UUID.randomUUID();
        Task task = buildTask(taskId, "Simple task", null, "EASY", null, null, null, 50L);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        JsonNode input = objectMapper.createObjectNode().put("taskId", taskId.toString());
        JsonNode result = tool.execute(input);

        assertThat(result.has("description")).isFalse();
        assertThat(result.has("templateCode")).isFalse();
        assertThat(result.has("testCode")).isFalse();
        assertThat(result.has("solutionCode")).isFalse();
    }

    @Test
    @SneakyThrows
    void execute_unknownTaskId_throwsApiException() {
        UUID taskId = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        JsonNode input = objectMapper.createObjectNode().put("taskId", taskId.toString());

        assertThatThrownBy(() -> tool.execute(input))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(taskId.toString());
    }

    @Test
    @SneakyThrows
    void execute_invalidUuid_throwsBadRequestApiException() {
        JsonNode input = objectMapper.createObjectNode().put("taskId", "not-a-uuid");

        assertThatThrownBy(() -> tool.execute(input))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid taskId UUID");
    }

    @Test
    void name_returnsLookupTask() {
        assertThat(tool.name()).isEqualTo("lookup_task");
    }

    @Test
    void inputSchema_hasRequiredTaskId() {
        JsonNode schema = tool.inputSchema();

        assertThat(schema.get("type").asText()).isEqualTo("object");
        assertThat(schema.get("required").get(0).asText()).isEqualTo("taskId");
        assertThat(schema.get("properties").has("taskId")).isTrue();
    }

    private static Task buildTask(
            UUID id,
            String title,
            String description,
            String difficulty,
            String templateCode,
            String testCode,
            String solutionCode,
            long xpReward) {
        Task task = new Task();
        setId(task, id);
        task.setTitle(title);
        task.setDescription(description);
        task.setDifficulty(difficulty);
        task.setTemplateCode(templateCode);
        task.setTestCode(testCode);
        task.setSolutionCode(solutionCode);
        task.setXpReward(xpReward);
        task.setLecture(mock(Lecture.class));
        return task;
    }

    @SneakyThrows
    private static void setId(Task task, UUID id) {
        var field = Task.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(task, id);
    }
}
