# Bug Fix Remediation Guide - Feature 002

This guide provides concrete code-level remediation steps for both critical bugs.

---

## Bug #1: Hardcoded AI Proposals (T063-BUG-001)

### Current Code (BROKEN)

**File**: `backend/src/main/java/com/gymtracker/infrastructure/ai/OnboardingPlanGenerator.java`

```java
@Component
public class OnboardingPlanGenerator {
    private final AzureOpenAiOnboardingProperties properties;

    public OnboardingPlanGenerator(AzureOpenAiOnboardingProperties properties) {
        this.properties = properties;
    }

    public PlanProposalResponse generateInitialProposal(UUID userId, OnboardingSubmissionRequest request) {
        UUID attemptId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();

        // ❌ HARDCODED: These exercises are always the same, no AI involved
        ProposedExerciseTarget squat = new ProposedExerciseTarget(
            "Back Squat", ExerciseType.STRENGTH, 4, 6, new BigDecimal("70"),
            request.weightUnit() == null ? WeightUnit.KG : request.weightUnit(),
            null, null, null
        );

        ProposedExerciseTarget run = new ProposedExerciseTarget(
            "Treadmill Run", ExerciseType.CARDIO,
            null, null, null, null, 900, null, null
        );

        List<ProposedSession> sessions = List.of(
            new ProposedSession(1, "Strength Foundation", List.of(squat)),
            new ProposedSession(2, "Cardio Builder", List.of(run))
        );

        return new PlanProposalResponse(
            attemptId, proposalId, 1, ProposalStatus.PROPOSED,
            new GeneratedBy(ProposalProvider.AZURE_OPENAI, properties.getDeployment()),
            sessions
        );
    }
}
```

### What Needs to Change

1. **Add LangChain4j dependency** in `backend/pom.xml`:
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-azure-open-ai</artifactId>
    <version>0.30.0</version>
</dependency>
```

2. **Create Azure OpenAI client bean** in a configuration class:
```java
@Configuration
public class AzureOpenAiClientConfig {
    
    @Bean
    public AzureChatLanguageModel azureChatLanguageModel(AzureOpenAiOnboardingProperties props) {
        return AzureChatLanguageModel.builder()
            .endpoint(props.getEndpoint())
            .apiKey(props.getApiKey())
            .deploymentName(props.getDeployment())
            .build();
    }
}
```

3. **Implement real LLM call** in `OnboardingPlanGenerator`:
```java
@Component
public class OnboardingPlanGenerator {
    
    private final AzureOpenAiOnboardingProperties properties;
    private final AzureChatLanguageModel chatModel;

    public OnboardingPlanGenerator(
        AzureOpenAiOnboardingProperties properties,
        AzureChatLanguageModel chatModel
    ) {
        this.properties = properties;
        this.chatModel = chatModel;
    }

    public PlanProposalResponse generateInitialProposal(UUID userId, OnboardingSubmissionRequest request) {
        UUID attemptId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();

        // ✅ REAL: Call Azure OpenAI to generate proposal
        String prompt = buildPrompt(request);
        String llmResponse = chatModel.generate(prompt);
        
        // Parse LLM response into ProposedSession objects
        List<ProposedSession> sessions = parseLlmResponse(llmResponse, request);

        return new PlanProposalResponse(
            attemptId, proposalId, 1, ProposalStatus.PROPOSED,
            new GeneratedBy(ProposalProvider.AZURE_OPENAI, properties.getDeployment()),
            sessions
        );
    }

    private String buildPrompt(OnboardingSubmissionRequest request) {
        return """
            Generate a personalized workout plan with 3-4 sessions for a user with:
            - Age: %d
            - Current Weight: %s %s
            - Primary Goal: %s
            - Target Bucket: %s
            
            Format response as JSON with structure:
            {
                "sessions": [
                    {"sequenceNumber": 1, "name": "...", "exercises": [...]}
                ]
            }
            """.formatted(
                request.age(),
                request.currentWeight(),
                request.weightUnit(),
                request.primaryGoal(),
                request.goalTargetBucket()
            );
    }

    private List<ProposedSession> parseLlmResponse(String json, OnboardingSubmissionRequest request) {
        // Parse JSON and convert to ProposedSession objects
        // Handle errors: throw RetryableException or ValidationException
        // Example parsing logic
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            // Parse and build sessions...
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }
}
```

### Validation
- ✅ Exercise names vary based on user input (age, weight, goal)
- ✅ NOT "Back Squat" and "Treadmill Run"
- ✅ Response comes from LLM, not hardcoded

---

## Bug #2: Incomplete Proposal Persistence (T064-BUG-002)

### Current Code (BROKEN)

**File**: `backend/src/main/java/com/gymtracker/application/PlanProposalService.java`

```java
@Service
public class PlanProposalService {
    
    private final OnboardingValidationService validationService;
    private final OnboardingPlanGenerator onboardingPlanGenerator;

    public PlanProposalService(
        OnboardingValidationService validationService,
        OnboardingPlanGenerator onboardingPlanGenerator
    ) {
        this.validationService = validationService;
        this.onboardingPlanGenerator = onboardingPlanGenerator;
    }

    // ❌ BROKEN: No persistence logic
    public Optional<OnboardingAttemptResponse> getCurrentAttempt(UUID userId) {
        return Optional.empty();  // Always empty!
    }

    // ❌ BROKEN: Breaks proposal chain
    public PlanProposalResponse createRevision(UUID userId, UUID proposalId, String requestedChanges) {
        PlanProposalResponse initial = onboardingPlanGenerator.generateInitialProposal(
            userId,
            resolveAttemptSnapshot(userId, UUID.randomUUID())  // Wrong UUID!
        );
        return new PlanProposalResponse(
            initial.attemptId,
            UUID.randomUUID(),  // New UUID each time - breaks chain
            initial.version() + 1,
            ProposalStatus.PROPOSED,
            new GeneratedBy(initial.generatedBy().provider(), initial.generatedBy().deployment()),
            initial.sessions()
        );
    }

    // ❌ BROKEN: Hardcoded response
    public OnboardingSubmissionRequest resolveAttemptSnapshot(UUID userId, UUID attemptId) {
        return new OnboardingSubmissionRequest(
            30, BigDecimal.valueOf(75), WeightUnit.KG,
            OnboardingPrimaryGoal.STRENGTH, null
        );
    }

    // ❌ BROKEN: Incomplete logic
    public TrackingAccessGateResponse getTrackingAccessGate(UUID userId) {
        boolean allowed = !UUID.fromString("22222222-2222-2222-2222-222222222222").equals(userId);
        return new TrackingAccessGateResponse(allowed, allowed ? "ALLOWED" : "ONBOARDING_REQUIRED", null);
    }
}
```

### What Needs to Change

1. **Add repository dependencies**:
```java
@Service
public class PlanProposalService {
    
    private final OnboardingValidationService validationService;
    private final OnboardingPlanGenerator onboardingPlanGenerator;
    private final OnboardingAttemptRepository attemptRepository;  // NEW
    private final PlanProposalRepository proposalRepository;      // NEW
    private final AcceptedProgramActivationRepository activationRepository;  // NEW
    private final EntityManager entityManager;  // NEW
    private final AuthenticationService authService;  // NEW

    public PlanProposalService(
        OnboardingValidationService validationService,
        OnboardingPlanGenerator onboardingPlanGenerator,
        OnboardingAttemptRepository attemptRepository,
        PlanProposalRepository proposalRepository,
        AcceptedProgramActivationRepository activationRepository,
        EntityManager entityManager,
        AuthenticationService authService
    ) {
        this.validationService = validationService;
        this.onboardingPlanGenerator = onboardingPlanGenerator;
        this.attemptRepository = attemptRepository;
        this.proposalRepository = proposalRepository;
        this.activationRepository = activationRepository;
        this.entityManager = entityManager;
        this.authService = authService;
    }
```

2. **Implement `getCurrentAttempt()` with real DB query**:
```java
@Transactional(readOnly = true)
public Optional<OnboardingAttemptResponse> getCurrentAttempt(UUID userId) {
    // Find latest IN_PROGRESS attempt for user
    ProfileGoalOnboardingAttempt attempt = attemptRepository
        .findLatestByUserIdAndStatus(userId, ProposalStatus.IN_PROGRESS)
        .orElse(null);
    
    if (attempt == null) {
        return Optional.empty();
    }
    
    // Load latest proposal for this attempt
    PlanProposal latestProposal = proposalRepository
        .findLatestByAttemptId(attempt.getId())
        .orElse(null);
    
    if (latestProposal == null) {
        return Optional.empty();
    }
    
    // Map to response DTO
    PlanProposalResponse proposalResponse = mapProposalToResponse(latestProposal);
    
    return Optional.of(new OnboardingAttemptResponse(
        attempt.getId(),
        attempt.getUserId(),
        attempt.getStatus(),
        attempt.getCreatedAt(),
        proposalResponse  // Latest proposal
    ));
}

private PlanProposalResponse mapProposalToResponse(PlanProposal proposal) {
    // Deserialize payload JSON to ProposedSession objects
    List<ProposedSession> sessions = deserializePayload(proposal.getPayload());
    
    return new PlanProposalResponse(
        proposal.getAttemptId(),
        proposal.getId(),
        proposal.getVersion(),
        proposal.getStatus(),
        new GeneratedBy(proposal.getProvider(), proposal.getDeployment()),
        sessions
    );
}
```

3. **Fix `createRevision()` to maintain proposal chain**:
```java
@Transactional
public PlanProposalResponse createRevision(UUID userId, UUID proposalId, String requestedChanges) {
    // Load the PARENT proposal being rejected
    PlanProposal parentProposal = proposalRepository.findById(proposalId)
        .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
    
    if (!parentProposal.getUserId().equals(userId)) {
        throw new ValidationException("User does not own this proposal");
    }
    
    UUID attemptId = parentProposal.getAttemptId();  // ✅ REUSE same attempt ID
    
    // Retrieve original user inputs from parent proposal
    OnboardingSubmissionRequest originalRequest = deserializeRequest(
        parentProposal.getOriginalRequest()
    );
    
    // Generate revised proposal with same inputs
    PlanProposalResponse revised = onboardingPlanGenerator.generateInitialProposal(
        userId,
        originalRequest
    );
    
    // Create v2 proposal linked to v1
    PlanProposal newProposal = new PlanProposal(
        attemptId,  // ✅ Same attempt
        UUID.randomUUID(),  // ✅ New proposal ID (different from parent)
        parentProposal.getVersion() + 1,  // ✅ Increment version
        ProposalStatus.PROPOSED,
        serializePayload(revised.sessions()),
        revised.generatedBy().provider(),
        revised.generatedBy().deployment(),
        originalRequest,  // Store for future revisions
        userId
    );
    
    proposalRepository.save(newProposal);
    
    return revised;
}
```

4. **Implement `resolveAttemptSnapshot()` to load actual data**:
```java
@Transactional(readOnly = true)
public OnboardingSubmissionRequest resolveAttemptSnapshot(UUID userId, UUID attemptId) {
    // Load the initial proposal for this attempt
    PlanProposal initialProposal = proposalRepository
        .findFirstByAttemptIdOrderByVersionAsc(attemptId)
        .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));
    
    if (!initialProposal.getUserId().equals(userId)) {
        throw new ValidationException("User does not own this attempt");
    }
    
    // Deserialize and return the original request
    return deserializeRequest(initialProposal.getOriginalRequest());
}
```

5. **Implement `getTrackingAccessGate()` with real validation**:
```java
@Transactional(readOnly = true)
public TrackingAccessGateResponse getTrackingAccessGate(UUID userId) {
    // Check if user has ACCEPTED onboarding attempt
    boolean hasAcceptedAttempt = attemptRepository
        .existsByUserIdAndStatus(userId, ProposalStatus.ACCEPTED);
    
    if (hasAcceptedAttempt) {
        return new TrackingAccessGateResponse(
            true,
            "ALLOWED",
            null
        );
    }
    
    // Check if user has active AcceptedProgramActivation
    boolean hasActiveActivation = activationRepository
        .existsByUserIdAndIsActiveTrue(userId);
    
    if (hasActiveActivation) {
        return new TrackingAccessGateResponse(
            true,
            "ALLOWED",
            null
        );
    }
    
    // User has NOT completed onboarding
    return new TrackingAccessGateResponse(
        false,
        "ONBOARDING_REQUIRED",
        "Complete your profile and goals setup to access program tracking"
    );
}
```

### Repository Interfaces Needed

```java
// OnboardingAttemptRepository
public interface OnboardingAttemptRepository extends JpaRepository<ProfileGoalOnboardingAttempt, UUID> {
    Optional<ProfileGoalOnboardingAttempt> findLatestByUserIdAndStatus(UUID userId, ProposalStatus status);
    boolean existsByUserIdAndStatus(UUID userId, ProposalStatus status);
}

// PlanProposalRepository
public interface PlanProposalRepository extends JpaRepository<PlanProposal, UUID> {
    Optional<PlanProposal> findLatestByAttemptId(UUID attemptId);
    Optional<PlanProposal> findFirstByAttemptIdOrderByVersionAsc(UUID attemptId);
}

// AcceptedProgramActivationRepository
public interface AcceptedProgramActivationRepository extends JpaRepository<AcceptedProgramActivation, UUID> {
    boolean existsByUserIdAndIsActiveTrue(UUID userId);
}
```

### Validation
- ✅ `getCurrentAttempt()` returns actual persisted attempt from DB
- ✅ `createRevision()` maintains same attemptId for chain continuity
- ✅ `createRevision()` increments version (1 → 2 → 3)
- ✅ `resolveAttemptSnapshot()` returns actual user inputs (not hardcoded)
- ✅ `getTrackingAccessGate()` validates against real acceptance status
- ✅ Reject/revise cycle maintains proposal linkage

---

## Testing

### For T063-BUG-001 (AI Generation)

```java
@SpringBootTest
@WithMockAzureOpenAi
class AzureOpenAiIntegrationIT {
    
    @Test
    void generateProposal_ReturnsNonHardcodedExercises() {
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
            35, BigDecimal.valueOf(80), KG, BUILD_MUSCLES, null
        );
        
        PlanProposalResponse proposal = generator.generateInitialProposal(userId, request);
        
        List<String> exerciseNames = proposal.sessions().stream()
            .flatMap(s -> s.exercises().stream())
            .map(ProposedExerciseTarget::exerciseName)
            .toList();
        
        // ✅ Key assertion: NOT the hardcoded names
        assertThat(exerciseNames)
            .doesNotContain("Back Squat", "Treadmill Run");
        
        // ✅ Verify it's from Azure OpenAI
        assertThat(proposal.generatedBy().provider())
            .isEqualTo(ProposalProvider.AZURE_OPENAI);
    }
}
```

### For T064-BUG-002 (Proposal Persistence)

```java
@SpringBootTest
@Transactional
class PlanProposalServiceIT {
    
    @Test
    void getCurrentAttempt_ReturnsPersistedAttempt() {
        // Create proposal via API
        PlanProposalResponse v1 = planProposalService.createInitialProposal(userId, request);
        
        // Query for current attempt
        Optional<OnboardingAttemptResponse> result = planProposalService.getCurrentAttempt(userId);
        
        assertThat(result).isPresent();
        assertThat(result.get().latestProposal().version()).isEqualTo(1);
    }
    
    @Test
    void createRevision_MaintainsProposalChain() {
        PlanProposalResponse v1 = planProposalService.createInitialProposal(userId, request);
        PlanProposalResponse v2 = planProposalService.createRevision(userId, v1.proposalId(), "Make it harder");
        
        // ✅ Same attempt
        assertThat(v2.attemptId()).isEqualTo(v1.attemptId());
        
        // ✅ Different proposal IDs (linked chain)
        assertThat(v2.proposalId()).isNotEqualTo(v1.proposalId());
        
        // ✅ Version incremented
        assertThat(v2.version()).isEqualTo(2);
    }
    
    @Test
    void getTrackingAccessGate_AllowsAfterAcceptance() {
        // Before acceptance
        TrackingAccessGateResponse before = planProposalService.getTrackingAccessGate(userId);
        assertThat(before.canAccessProgramTracking()).isFalse();
        
        // Accept proposal
        acceptedProgramActivationService.acceptProposal(userId, proposalId);
        
        // After acceptance
        TrackingAccessGateResponse after = planProposalService.getTrackingAccessGate(userId);
        assertThat(after.canAccessProgramTracking()).isTrue();
    }
}
```

---

## Execution Checklist

### T063-BUG-001 (AI Generation)
- [ ] Add LangChain4j dependency to pom.xml
- [ ] Create AzureOpenAiClientConfig bean
- [ ] Implement `buildPrompt()` method
- [ ] Implement `parseLlmResponse()` method
- [ ] Update `generateInitialProposal()` to call LLM
- [ ] Add error handling for LLM failures
- [ ] Run T063-BUG-001-TEST
- [ ] Verify exercises are NOT "Back Squat" and "Treadmill Run"

### T064-BUG-002 (Proposal Persistence)
- [ ] Create repository interfaces
- [ ] Implement `getCurrentAttempt()` with DB query
- [ ] Implement `createRevision()` with chain maintenance
- [ ] Implement `resolveAttemptSnapshot()` with data loading
- [ ] Implement `getTrackingAccessGate()` with status validation
- [ ] Add helper methods for serialization/deserialization
- [ ] Run T064-BUG-002-TEST
- [ ] Verify proposal chain: v1 → v2 → v3 with continuity
- [ ] Verify access gate allows after acceptance

### Final Validation
- [ ] Re-run smoke test
- [ ] "Generate Plan" uses AI (not hardcoded)
- [ ] "Accept Plan" creates program and gates access
- [ ] "Reject & Revise" creates v2 with feedback
- [ ] Accept v2 completes onboarding
- [ ] All tests pass

---

**This guide is ready for handoff to the backend team.**

