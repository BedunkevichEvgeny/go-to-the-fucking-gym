import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
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

    expect(screen.getByText(/Next Program Session: Session 1/)).toBeTruthy();
    expect(screen.getByText('Bench Press')).toBeTruthy();
    expect(screen.getByText('Pull Up')).toBeTruthy();
    expect(screen.getByText('Running')).toBeTruthy();
    expect(screen.getByText(/Target: 3 × 8 @ 70 KG/)).toBeTruthy();
    expect(screen.getByText('How did it feel?')).toBeTruthy();
    expect(screen.getByRole('slider')).toBeTruthy();
    expect(screen.getByRole('textbox')).toBeTruthy();
  });

  it('allows updating set values and bodyweight toggle', () => {
    render(<ProgramSessionForm session={sessionFixture} onSubmit={vi.fn()} isSaving={false} />);

    const benchSection = screen.getByText('Bench Press').closest('section');
    expect(benchSection).toBeTruthy();

    const [repsInput, weightInput] = within(benchSection as HTMLElement).getAllByRole('spinbutton');
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

    expect(screen.getAllByRole('spinbutton').length).toBeGreaterThanOrEqual(6);
    expect(screen.queryAllByRole('button', { name: 'Remove set' }).length).toBeGreaterThan(0);
    expect(screen.queryAllByRole('button', { name: 'Remove lap' }).length).toBeGreaterThan(0);
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

    fireEvent.change(screen.getByRole('slider'), { target: { value: '9' } });
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'Great session' } });

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

  it('submits with minimum rating value', async () => {
    const onSubmit = vi.fn();
    render(<ProgramSessionForm session={sessionFixture} onSubmit={onSubmit} isSaving={false} />);

    fireEvent.change(screen.getByRole('slider'), { target: { value: '1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save session' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    expect(onSubmit.mock.calls[0][0].feelings.rating).toBe(1);
  });
});




