import { Link } from 'react-router-dom';

/** Props for navigation link to exercise progression view. */
interface ExerciseProgressionLinkProps {
  /** Exercise name used to build the progression route segment. */
  exerciseName: string;
}

export function ExerciseProgressionLink({ exerciseName }: ExerciseProgressionLinkProps) {
  return (
    <Link className="button ghost" to={`/progression/${encodeURIComponent(exerciseName)}`}>
      View progression
    </Link>
  );
}

