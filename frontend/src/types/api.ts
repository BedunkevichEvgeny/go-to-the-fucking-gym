export type SessionType = 'PROGRAM' | 'FREE';
export type ExerciseType = 'STRENGTH' | 'BODYWEIGHT' | 'CARDIO';
export type WeightUnit = 'KG' | 'LBS';
export type DistanceUnit = 'KM' | 'MI';
export type ProgressionMetricType = 'WEIGHT' | 'DURATION' | 'DISTANCE';

export interface ProgramExerciseTargetView {
  exerciseName: string;
  exerciseType: ExerciseType;
  targetSets?: number | null;
  targetReps?: number | null;
  targetWeight?: number | null;
  targetWeightUnit?: WeightUnit | null;
  targetDurationSeconds?: number | null;
  targetDistance?: number | null;
  targetDistanceUnit?: DistanceUnit | null;
}

export interface ProgramSessionView {
  programSessionId: string;
  sequenceNumber: number;
  name: string;
  exercises: ProgramExerciseTargetView[];
}

export interface SessionFeelingsInput {
  rating: number;
  comment?: string;
}

export interface StrengthSetInput {
  reps: number;
  isBodyWeight: boolean;
  weightValue?: number | null;
  weightUnit?: WeightUnit | null;
}

export interface CardioLapInput {
  durationSeconds: number;
  distanceValue?: number | null;
  distanceUnit?: DistanceUnit | null;
}

export interface ExerciseEntryInput {
  exerciseId?: string | null;
  customExerciseName?: string | null;
  exerciseName: string;
  exerciseType: ExerciseType;
  sets?: StrengthSetInput[];
  cardioLaps?: CardioLapInput[];
}

export interface LoggedSessionCreateRequest {
  sessionType: SessionType;
  programSessionId?: string | null;
  sessionDate: string;
  name?: string;
  notes?: string;
  totalDurationSeconds?: number | null;
  feelings: SessionFeelingsInput;
  exerciseEntries: ExerciseEntryInput[];
}

export interface StrengthSetView extends StrengthSetInput {
  setOrder: number;
  durationSeconds?: number | null;
  restSeconds?: number | null;
}

export interface CardioLapView extends CardioLapInput {
  lapOrder: number;
}

export interface ExerciseEntryView {
  exerciseId?: string | null;
  customExerciseName?: string | null;
  exerciseName: string;
  exerciseType: ExerciseType;
  sets: StrengthSetView[];
  cardioLaps: CardioLapView[];
}

export interface LoggedSessionDetail {
  sessionId: string;
  sessionType: SessionType;
  programSessionId?: string | null;
  sessionDate: string;
  name?: string;
  notes?: string;
  totalDurationSeconds?: number | null;
  feelings: SessionFeelingsInput;
  exerciseEntries: ExerciseEntryView[];
}

export interface SessionHistoryItem {
  sessionId: string;
  sessionDate: string;
  sessionType: SessionType;
  exerciseCount: number;
  totalDurationSeconds?: number | null;
  name?: string | null;
}

export interface SessionHistoryPage {
  items: SessionHistoryItem[];
  page: number;
  size: number;
  totalItems: number;
}

export interface ExerciseDto {
  id: string;
  name: string;
  category: string;
  type: ExerciseType;
  description?: string | null;
}

export interface ProgressionPoint {
  sessionId: string;
  sessionDate: string;
  metricType: ProgressionMetricType;
  metricValue: number;
}

export interface ProgressionResponse {
  exerciseName: string;
  points: ProgressionPoint[];
}

