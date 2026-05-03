package com.gymtracker.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

/**
 * Contract tests for the {@link LangChainSessionProcessor} interface.
 *
 * <p>Since the implementation is generated at runtime by LangChain4j {@code AiServices},
 * unit tests here focus on the interface contract: the interface is mockable, the method
 * signature matches callers' expectations, and the contract annotation ({@code @SystemMessage})
 * is present.
 */
class LangChainSessionProcessorTest {

    // ── interface is mockable ─────────────────────────────────────────────────

    @Test
    void interfaceCanBeMockedWithMockito() {
        LangChainSessionProcessor mock = mock(LangChainSessionProcessor.class);
        assertThat(mock).isNotNull();
    }

    @Test
    void mockedInterfaceReturnsConfiguredResponse() {
        LangChainSessionProcessor mock = mock(LangChainSessionProcessor.class);
        when(mock.process("mem-1", "any prompt")).thenReturn("analysis result");

        String result = mock.process("mem-1", "any prompt");

        assertThat(result).isEqualTo("analysis result");
    }

    // ── @SystemMessage annotation is present ─────────────────────────────────

    @Test
    void processMethodHasSystemMessageAnnotation() throws Exception {
        var method = LangChainSessionProcessor.class
                .getMethod("process", String.class, String.class);

        dev.langchain4j.service.SystemMessage annotation =
                method.getAnnotation(dev.langchain4j.service.SystemMessage.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isNotEmpty();
    }

    // ── @MemoryId and @UserMessage annotations are on the parameters ─────────

    @Test
    void processMethodParametersHaveCorrectAnnotations() throws Exception {
        var method = LangChainSessionProcessor.class
                .getMethod("process", String.class, String.class);
        var paramAnnotations = method.getParameterAnnotations();

        // first param must carry @MemoryId
        boolean hasMemoryId = java.util.Arrays.stream(paramAnnotations[0])
                .anyMatch(a -> a.annotationType().equals(dev.langchain4j.service.MemoryId.class));
        // second param must carry @UserMessage
        boolean hasUserMessage = java.util.Arrays.stream(paramAnnotations[1])
                .anyMatch(a -> a.annotationType().equals(dev.langchain4j.service.UserMessage.class));

        assertThat(hasMemoryId).as("first parameter must be @MemoryId").isTrue();
        assertThat(hasUserMessage).as("second parameter must be @UserMessage").isTrue();
    }
}
