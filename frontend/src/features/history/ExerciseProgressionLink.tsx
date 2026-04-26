import { Link } from 'react-router-dom';

interface ExerciseProgressionLinkProps {
  exerciseName: string;
}

export function ExerciseProgressionLink({ exerciseName }: ExerciseProgressionLinkProps) {
  return (
    <Link className="button ghost" to={`/progression/${encodeURIComponent(exerciseName)}`}>
      View progression
    </Link>
  );
}

