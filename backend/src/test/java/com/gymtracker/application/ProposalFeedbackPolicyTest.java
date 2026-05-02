package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymtracker.api.exception.ValidationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProposalFeedbackPolicyTest {

    private final ProposalFeedbackService service = new ProposalFeedbackService(null, null);

    @Test
    void acceptsReasonableFeedbackText() {
        assertThatCode(() -> service.validateFeedback(UUID.randomUUID(), UUID.randomUUID(), "Please reduce deadlift volume"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsBlankFeedbackText() {
        assertThatThrownBy(() -> service.validateFeedback(UUID.randomUUID(), UUID.randomUUID(), "   "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("requestedChanges");
    }

    @Test
    void rejectsTooLongFeedbackText() {
        String tooLong = "x".repeat(2001);
        assertThatThrownBy(() -> service.validateFeedback(UUID.randomUUID(), UUID.randomUUID(), tooLong))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("2000");
    }
}

