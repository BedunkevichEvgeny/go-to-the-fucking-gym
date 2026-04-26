import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ProgramSessionForm } from '../ProgramSessionForm';
import type { LoggedSessionCreateRequest, ProgramSessionView } from '../../../types/api';

const sessionFixture: ProgramSessionView = {
  programSessionId: '11111111-1111-1111-1111-111111111111',
  sequenceNumber: 1,
  name: 'Upper Body',
  exercises: [
    {
      exerciseName: 'Bench Press',
      exerciseType: 'STRENGTH',
      targetSets: 3,
      targetReps: 8,
      targetWeight: 70,
      targetWeightUnit: 'KG',
    },
    {
      exerciseName: 'Pull Up',
      exerciseType: 'BODYWEIGHT',
      targetSets: 3,
      targetReps: 8,
    },
    {
      exerciseName: 'Running',
      exerciseType: 'CARDIO',
      targetDurationSeconds: 900,
    },
  ],
};

describe('ProgramSessionForm', () => {
  it('renders session header, exercises, targets, and feelings controls', () => {
    render(<ProgramSessionForm session={sessionFixture} onSubmit={vi.fn()} isSaving={false} />);

    expect(screen.getByText('Next Program Session: Session 1 - Upper Body')).toBeTruthy();
    expect(screen.getByText('Bench Press')).toBeTruthy();
    expect(screen.getByText('Pull Up')).toBeTruthy();
    expect(screen.getByText('Running')).toBeTruthy();
    expect(screen.getByText(/Target: 3 × 8 @ 70 KG/)).toBeTruthy();
    expect(screen.getByText('How did it feel?')).toBeTruthy();
    expect(screen.getByLabelText('Rating (1-10)')).toBeTruthy();
    expect(screen.getByLabelText('Comment')).toBeTruthy();
  });

  it('allows updating set values and bodyweight toggle', () => {
    render(<ProgramSessionForm session={sessionFixture} onSubmit={vi.fn()} isSaving={false} />);

    const benchSection = screen.getByText('Bench Press').closest('section');
    expect(benchSection).toBeTruthy();

    const repsInput = within(benchSection as HTMLElement).getByLabelText('Set 1 reps');
    const weightInput = within(benchSection as HTMLElement).getByLabelText('Weight');
    const bodyweightToggle = within(benchSection as HTMLElement).getByLabelText('Bodyweight');

    fireEvent.change(repsInput, { target: { value: '2' } });
    fireEvent.click(bodyweightToggle);

    expect(repsInput).toHaveValue(2);
    expect(bodyweightToggle).toBeChecked();
    expect(weightInput).toBeDisabled();
  });

  it('allows adding extra sets and laps while keeping at least one entry', () => {
    render(<ProgramSessionForm session={sessionFixture} onSubmit={vi.fn()} isSaving={false} />);

    fireEvent.click(screen.getAllByRole('button', { name: 'Add set' })[0]);
    fireEvent.click(screen.getByRole('button', { name: 'Add lap' }));

    expect(screen.getAllByLabelText(/Set [0-9]+ reps/).length).toBeGreaterThanOrEqual(3);
    expect(screen.getAllByLabelText(/Lap [0-9]+ duration \(sec\)/).length).toBeGreaterThanOrEqual(2);
    expect(screen.queryByRole('button', { name: 'Remove set' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Remove lap' })).toBeTruthy();
  });

  it('shows loading state when save is in progress', () => {
    render(<ProgramSessionForm session={sessionFixture} onSubmit={vi.fn()} isSaving={true} />);

    const button = screen.getByRole('button', { name: 'Saving…' });
    expect(button).toBeDisabled();
  });

  it('submits expected payload and shows success message', async () => {
    const onSubmit = vi.fn<
      (payload: LoggedSessionCreateRequest) => Promise<void>
    >().mockResolvedValue(undefined);

    render(<ProgramSessionForm session={sessionFixture} onSubmit={onSubmit} isSaving={false} />);

    fireEvent.change(screen.getByLabelText('Rating (1-10)'), { target: { value: '9' } });
    fireEvent.change(screen.getByLabelText('Comment'), { target: { value: 'Great session' } });

    fireEvent.click(screen.getByRole('button', { name: 'Save session' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));

    const payload = onSubmit.mock.calls[0][0];
    expect(payload.sessionType).toBe('PROGRAM');
    expect(payload.programSessionId).toBe(sessionFixture.programSessionId);
    expect(payload.sessionDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(payload.feelings.rating).toBe(9);
    expect(payload.feelings.comment).toBe('Great session');
    expect(payload.exerciseEntries).toHaveLength(3);
    expect(screen.getByText('Session saved successfully.')).toBeTruthy();
  });

  it('validates rating range before submit', async () => {
    const onSubmit = vi.fn();
    render(<ProgramSessionForm session={sessionFixture} onSubmit={onSubmit} isSaving={false} />);

    fireEvent.change(screen.getByLabelText('Rating (1-10)'), { target: { value: '0' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save session' }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Rating is required and must be between 1 and 10.');
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });
});

