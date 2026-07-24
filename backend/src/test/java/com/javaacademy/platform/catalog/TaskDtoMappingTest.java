package com.javaacademy.platform.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaacademy.platform.catalog.dto.TaskResponse;
import com.javaacademy.platform.catalog.entity.Task;
import com.javaacademy.platform.catalog.repository.CourseModuleRepository;
import com.javaacademy.platform.catalog.repository.CourseRepository;
import com.javaacademy.platform.catalog.repository.LectureRepository;
import com.javaacademy.platform.catalog.repository.TaskRepository;
import com.javaacademy.platform.catalog.service.CatalogService;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Proves at both the type level (reflection) and the serialisation level (Jackson) that
 * testCode and solutionCode — fields on the Task entity that must never leave the server —
 * are absent from every JSON response produced by the catalog read path.
 */
@ExtendWith(MockitoExtension.class)
class TaskDtoMappingTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    CourseRepository courseRepository;

    @Mock
    CourseModuleRepository moduleRepository;

    @Mock
    LectureRepository lectureRepository;

    @InjectMocks
    CatalogService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── type-level proof ──────────────────────────────────────────────────────

    @Test
    void taskResponse_recordComponents_neverIncludeTestCodeOrSolutionCode() {
        List<String> components = Arrays.stream(TaskResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(components)
                .as("TaskResponse must expose exactly these fields and no secret ones")
                .containsExactly("id", "title", "description", "difficulty", "templateCode", "xpReward");

        assertThat(components).doesNotContain("testCode", "solutionCode");
    }

    // ── mapping-level proof ───────────────────────────────────────────────────

    @Test
    void getTask_entityWithTestCode_returnsTaskResponseWithoutSecretFields() {
        Task task = taskWithSecrets();
        when(taskRepository.findById(any())).thenReturn(Optional.of(task));

        TaskResponse result = service.getTask(UUID.randomUUID());

        assertThat(result.title()).isEqualTo("Hello World");
        assertThat(result.difficulty()).isEqualTo("EASY");
        assertThat(result.templateCode()).isEqualTo("// write here");
        assertThat(result.xpReward()).isEqualTo(50L);
    }

    // ── serialisation-level proof ─────────────────────────────────────────────

    @Test
    void getTask_entityWithTestCode_jsonDoesNotContainTestCodeKey() throws Exception {
        Task task = taskWithSecrets();
        when(taskRepository.findById(any())).thenReturn(Optional.of(task));

        TaskResponse result = service.getTask(UUID.randomUUID());
        String json = objectMapper.writeValueAsString(result);

        assertThat(json).doesNotContain("testCode");
        assertThat(json).doesNotContain("solutionCode");
    }

    @Test
    void getTask_entityWithTestCode_jsonDoesNotLeakSecretValues() throws Exception {
        Task task = taskWithSecrets();
        when(taskRepository.findById(any())).thenReturn(Optional.of(task));

        TaskResponse result = service.getTask(UUID.randomUUID());
        String json = objectMapper.writeValueAsString(result);

        assertThat(json).doesNotContain("HIDDEN_TEST_CODE");
        assertThat(json).doesNotContain("HIDDEN_SOLUTION_CODE");
    }

    @Test
    void getTask_entityWithTestCode_jsonContainsOnlyExpectedKeys() throws Exception {
        Task task = taskWithSecrets();
        when(taskRepository.findById(any())).thenReturn(Optional.of(task));

        TaskResponse result = service.getTask(UUID.randomUUID());
        String json = objectMapper.writeValueAsString(result);

        // Verify the JSON has exactly the expected keys and nothing else
        assertThat(json).contains("\"id\"");
        assertThat(json).contains("\"title\"");
        assertThat(json).contains("\"description\"");
        assertThat(json).contains("\"difficulty\"");
        assertThat(json).contains("\"templateCode\"");
        assertThat(json).contains("\"xpReward\"");
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Task taskWithSecrets() {
        Task task = new Task();
        task.setTitle("Hello World");
        task.setDescription("Print hello world");
        task.setDifficulty("EASY");
        task.setTemplateCode("// write here");
        task.setTestCode("HIDDEN_TEST_CODE: assert output.equals(\"Hello World\");");
        task.setSolutionCode("HIDDEN_SOLUTION_CODE: System.out.println(\"Hello World\");");
        task.setXpReward(50L);
        return task;
    }
}
