package com.gymtracker.application;

import com.gymtracker.domain.ProgramStatus;
import com.gymtracker.domain.WorkoutProgram;
import org.springframework.stereotype.Component;

/**
 * Implements active program replacement policy.
 *
 * Ensures that when a user accepts a new onboarding plan to replace their active program,
 * the transition happens cleanly without affecting historical data, progression tracking,
 * or logged session records from the previous program.
 */
@Component
public class ProgramReplacementPolicy {

    public void replaceActiveProgram(WorkoutProgram oldProgram, WorkoutProgram newProgram) {
        // Mark old program as no longer active (but preserve it for history)
        // Don't force-complete it; let users see what was in flight
        if (oldProgram.getStatus() == ProgramStatus.ACTIVE) {
            oldProgram.setStatus(ProgramStatus.REPLACED);
        }

        // New program remains ACTIVE and ready for session logging
        newProgram.setStatus(ProgramStatus.ACTIVE);

        // Both programs remain in database:
        // - Old: for history, progression, and logged session queries
        // - New: for current session tracking and logging
    }
}

