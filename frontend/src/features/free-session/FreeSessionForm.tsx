import type { FormEvent } from 'react';
import { useState } from 'react';
import { CardioLapInputRow } from '../../components/CardioLapInputRow';
import { SetInputRow } from '../../components/SetInputRow';
import type { ExerciseDto, ExerciseEntryInput, ExerciseType, LoggedSessionCreateRequest } from '../../types/api';
import { ExerciseLibrarySearch } from './ExerciseLibrarySearch';

interface FreeSessionFormProps {
  onSubmit: (payload: LoggedSessionCreateRequest) => Promise<void> | void;
  isSaving: boolean;
}

export function FreeSessionForm({ onSubmit, isSaving }: FreeSessionFormProps) {
  const [entries, setEntries] = useState<ExerciseEntryInput[]>([]);
  const [rating, setRating] = useState(8);
  const [comment, setComment] = useState('');

  const addExercise = (exercise: ExerciseDto | { name: string }) => {
    const exerciseType: ExerciseType = 'type' in exercise ? exercise.type : 'STRENGTH';
    setEntries((current) => [
      ...current,
      {
        exerciseId: 'id' in exercise ? exercise.id : null,
        customExerciseName: 'id' in exercise ? null : exercise.name,
        exerciseName: exercise.name,
        exerciseType,
        sets: exerciseType === 'CARDIO' ? [] : [{ reps: 8, isBodyWeight: exerciseType === 'BODYWEIGHT', weightValue: exerciseType === 'STRENGTH' ? 20 : null, weightUnit: 'KG' }],
        cardioLaps: exerciseType === 'CARDIO' ? [{ durationSeconds: 300, distanceValue: null, distanceUnit: 'KM' }] : [],
      },
    ]);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await onSubmit({
      sessionType: 'FREE',
      sessionDate: new Date().toISOString().slice(0, 10),
      feelings: { rating, comment },
      exerciseEntries: entries,
    });
  };

  return (
    <form className="stack-lg" onSubmit={handleSubmit}>
      <ExerciseLibrarySearch onAddExercise={addExercise} />
      {entries.map((entry, exerciseIndex) => (
        <section key={`${entry.exerciseName}-${exerciseIndex}`} className="card stack-sm">
          <div className="between">
            <h2>{entry.exerciseName}</h2>
            <button className="button ghost" type="button" onClick={() => setEntries((current) => current.filter((_, index) => index !== exerciseIndex))}>
              Remove exercise
            </button>
          </div>
          {entry.exerciseType === 'CARDIO'
            ? entry.cardioLaps?.map((lap, lapIndex) => (
                <CardioLapInputRow
                  key={`${entry.exerciseName}-${lapIndex}`}
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
                          nextEntries[exerciseIndex] = { ...entry, cardioLaps: entry.cardioLaps?.filter((_, index) => index !== lapIndex) };
                          setEntries(nextEntries);
                        }
                      : undefined
                  }
                />
              ))
            : entry.sets?.map((set, setIndex) => (
                <SetInputRow
                  key={`${entry.exerciseName}-${setIndex}`}
                  index={setIndex}
                  value={set}
                  onChange={(next) => {
                    const nextEntries = [...entries];
                    nextEntries[exerciseIndex] = { ...entry, sets: entry.sets?.map((item, index) => (index === setIndex ? next : item)) };
                    setEntries(nextEntries);
                  }}
                  onRemove={
                    entry.sets && entry.sets.length > 1
                      ? () => {
                          const nextEntries = [...entries];
                          nextEntries[exerciseIndex] = { ...entry, sets: entry.sets?.filter((_, index) => index !== setIndex) };
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
              nextEntries[exerciseIndex] = entry.exerciseType === 'CARDIO'
                ? { ...entry, cardioLaps: [...(entry.cardioLaps ?? []), { durationSeconds: 300, distanceValue: null, distanceUnit: 'KM' }] }
                : { ...entry, sets: [...(entry.sets ?? []), { reps: 8, isBodyWeight: entry.exerciseType === 'BODYWEIGHT', weightValue: entry.exerciseType === 'STRENGTH' ? 20 : null, weightUnit: 'KG' }] };
              setEntries(nextEntries);
            }}
          >
            Add {entry.exerciseType === 'CARDIO' ? 'lap' : 'set'}
          </button>
        </section>
      ))}
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
      <button className="button primary" disabled={isSaving || entries.length === 0} type="submit">
        {isSaving ? 'Saving…' : 'Save free session'}
      </button>
    </form>
  );
}


