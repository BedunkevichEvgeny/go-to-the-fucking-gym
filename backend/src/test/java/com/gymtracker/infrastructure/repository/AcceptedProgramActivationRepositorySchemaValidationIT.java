package com.gymtracker.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.domain.AcceptedProgramActivation;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "azure.openai.endpoint=https://schema-validation.test.azure.openai",
        "azure.openai.api-key=test-api-key",
        "azure.openai.deployment=test-deployment"
})
class AcceptedProgramActivationRepositorySchemaValidationIT {

    @Autowired
    private AcceptedProgramActivationRepository repository;

    @Test
    void startupAndDerivedQueriesUseExistingEntityProperties() {
        UUID userId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();

        AcceptedProgramActivation activation = AcceptedProgramActivation.builder()
                .attemptId(UUID.randomUUID())
                .proposalId(proposalId)
                .userId(userId)
                .activatedProgramId(UUID.randomUUID())
                .replacedProgramId(null)
                .build();

        repository.saveAndFlush(activation);

        assertThat(repository.existsByUserId(userId)).isTrue();
        assertThat(repository.findByProposalId(proposalId)).isPresent();
        assertThat(repository.existsByUserId(UUID.randomUUID())).isFalse();
    }
}
