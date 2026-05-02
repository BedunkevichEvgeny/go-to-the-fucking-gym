import type { FormEvent } from 'react';
import { useState } from 'react';
import { CardioLapInputRow } from '../../components/CardioLapInputRow';
import { SetInputRow } from '../../components/SetInputRow';
import type { ExerciseEntryInput, LoggedSessionCreateRequest, ProgramSessionView } from '../../types/api';

/** Props for logging the next prescribed program session. */
interface ProgramSessionFormProps {
  /** Program session payload including exercise targets to log against. */
  session: ProgramSessionView;
  /** Submit handler invoked with the completed logged session payload. */
  onSubmit: (payload: LoggedSessionCreateRequest) => Promise<void> | void;
  /** Indicates whether save is in progress to disable submit actions. */
  isSaving: boolean;
}

function createInitialEntry(exercise: ProgramSessionView['exercises'][number]): ExerciseEntryInput {
  return {
    exerciseName: exercise.exerciseName,
    exerciseType: exercise.exerciseType,
    sets: exercise.exerciseType === 'CARDIO' ? [] : [{ reps: exercise.targetReps ?? 8, isBodyWeight: exercise.exerciseType === 'BODYWEIGHT', weightValue: exercise.exerciseType === 'STRENGTH' ? exercise.targetWeight ?? null : null, weightUnit: exercise.targetWeightUnit ?? 'KG' }],
    cardioLaps: exercise.exerciseType === 'CARDIO' ? [{ durationSeconds: exercise.targetDurationSeconds ?? 600, distanceValue: exercise.targetDistance ?? null, distanceUnit: exercise.targetDistanceUnit ?? 'KM' }] : [],
  };
}

export function ProgramSessionForm({ session, onSubmit, isSaving }: ProgramSessionFormProps) {
  const [entries, setEntries] = useState<ExerciseEntryInput[]>(() => session.exercises.map(createInitialEntry));
  const [rating, setRating] = useState(7);
  const [comment, setComment] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFormError(null);
    setSuccessMessage(null);

    if (rating < 1 || rating > 10) {
      setFormError('Rating is required and must be between 1 and 10.');
      return;
    }

    const hasInvalidEntry = entries.some((entry) => {
      if (entry.exerciseType === 'CARDIO') {
        return !entry.cardioLaps || entry.cardioLaps.length === 0;
      }
      return !entry.sets || entry.sets.length === 0;
    });

    if (hasInvalidEntry) {
      setFormError('At least one set or lap is required for each exercise.');
      return;
    }

    await onSubmit({
      sessionType: 'PROGRAM',
      programSessionId: session.programSessionId,
      sessionDate: new Date().toISOString().slice(0, 10),
      feelings: { rating, comment },
      exerciseEntries: entries,
    });

    setSuccessMessage('Session saved successfully.');
  };

  return (
    <form className="stack-lg" onSubmit={handleSubmit}>
      <h2>
        Next Program Session: Session {session.sequenceNumber} — {session.name}
      </h2>
      {session.exercises.map((exercise, exerciseIndex) => {
        const entry = entries[exerciseIndex];
        return (
          <section key={exercise.exerciseName} className="card stack-sm">
            <h2>{exercise.exerciseName}</h2>
            <p className="muted">
              Target:{' '}
              {exercise.exerciseType === 'CARDIO'
                ? `${exercise.targetDurationSeconds ?? 0}s`
                : `${exercise.targetSets ?? 1} × ${exercise.targetReps ?? 0}${exercise.targetWeight ? ` @ ${exercise.targetWeight} ${exercise.targetWeightUnit ?? ''}` : ''}`}
            </p>
            {exercise.exerciseType === 'CARDIO'
              ? entry.cardioLaps?.map((lap, lapIndex) => (
                  <CardioLapInputRow
                    key={`${exercise.exerciseName}-${lapIndex}`}
                    index={lapIndex}
                    value={lap}
                    onChange={(next) => {
                      const nextEntries = [...entries];
                      nextEntries[exerciseIndex] = {
                        ...entry,
                        cardioLaps: entry.cardioLaps?.map((item, index) => (index === lapIndex ? next : item)),
                      };
                      setEntries(nextEntries);
                    }}
                    onRemove={
                      entry.cardioLaps && entry.cardioLaps.length > 1
                        ? () => {
                            const nextEntries = [...entries];
                            nextEntries[exerciseIndex] = {
                              ...entry,
                              cardioLaps: entry.cardioLaps?.filter((_, index) => index !== lapIndex),
                            };
                            setEntries(nextEntries);
                          }
                        : undefined
                    }
                  />
                ))
              : entry.sets?.map((set, setIndex) => (
                  <SetInputRow
                    key={`${exercise.exerciseName}-${setIndex}`}
                    index={setIndex}
                    value={set}
                    onChange={(next) => {
                      const nextEntries = [...entries];
                      nextEntries[exerciseIndex] = {
                        ...entry,
                        sets: entry.sets?.map((item, index) => (index === setIndex ? next : item)),
                      };
                      setEntries(nextEntries);
                    }}
                    onRemove={
                      entry.sets && entry.sets.length > 1
                        ? () => {
                            const nextEntries = [...entries];
                            nextEntries[exerciseIndex] = {
                              ...entry,
                              sets: entry.sets?.filter((_, index) => index !== setIndex),
                            };
                            setEntries(nextEntries);
                          }
                        : undefined
                    }
                  />
                ))}
            <button
              className="button ghost"
              type="button"
              onClick={() => {
                const nextEntries = [...entries];
                nextEntries[exerciseIndex] = exercise.exerciseType === 'CARDIO'
                  ? { ...entry, cardioLaps: [...(entry.cardioLaps ?? []), { durationSeconds: 300, distanceValue: null, distanceUnit: 'KM' }] }
                  : { ...entry, sets: [...(entry.sets ?? []), { reps: 8, isBodyWeight: exercise.exerciseType === 'BODYWEIGHT', weightValue: exercise.exerciseType === 'STRENGTH' ? exercise.targetWeight ?? null : null, weightUnit: exercise.targetWeightUnit ?? 'KG' }] };
                setEntries(nextEntries);
              }}
            >
              Add {exercise.exerciseType === 'CARDIO' ? 'lap' : 'set'}
            </button>
          </section>
        );
      })}
      <section className="card stack-sm">
        <h2>How did it feel?</h2>
        <label>
          <span>Rating (1-10)</span>
          <input max={10} min={1} type="range" value={rating} onChange={(event) => setRating(Number(event.target.value))} />
          <strong>{rating}</strong>
        </label>
        <label>
          <span>Comment</span>
          <textarea rows={3} value={comment} onChange={(event) => setComment(event.target.value)} />
        </label>
      </section>
      <button className="button primary" disabled={isSaving} type="submit">
        {isSaving ? 'Saving…' : 'Save session'}
      </button>
      {formError ? <p role="alert">{formError}</p> : null}
      {successMessage ? <p>{successMessage}</p> : null}
    </form>
  );
}


