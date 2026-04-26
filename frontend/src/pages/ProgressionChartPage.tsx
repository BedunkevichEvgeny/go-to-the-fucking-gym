import { useNavigate, useParams } from 'react-router-dom';
import { ProgressionChart } from '../features/progression/ProgressionChart';
import { useExerciseProgression } from '../hooks/useExerciseProgression';

export function ProgressionChartPage() {
  const navigate = useNavigate();
  const { exerciseName } = useParams();
  const decodedName = exerciseName ? decodeURIComponent(exerciseName) : undefined;
  const { data, isLoading } = useExerciseProgression(decodedName);

  return (
    <section className="stack-lg">
      <div className="between">
        <div>
          <p className="eyebrow">Progression</p>
          <h1>{decodedName}</h1>
        </div>
        <button className="button ghost" type="button" onClick={() => navigate(-1)}>
          Back
        </button>
      </div>
      {isLoading ? <p className="card">Loading progression…</p> : <ProgressionChart points={data?.points ?? []} />}
    </section>
  );
}

