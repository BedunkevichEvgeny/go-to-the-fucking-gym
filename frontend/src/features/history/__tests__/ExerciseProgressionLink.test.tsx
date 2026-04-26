import '@testing-library/jest-dom/vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { ExerciseProgressionLink } from '../ExerciseProgressionLink';

describe('ExerciseProgressionLink', () => {
  it('renders View Progression link for an exercise and navigates to progression page', () => {
    render(
      <MemoryRouter initialEntries={['/history/session-1']}>
        <Routes>
          <Route path="/history/:id" element={<ExerciseProgressionLink exerciseName="Bench Press" />} />
          <Route path="/progression/:exerciseName" element={<p>Progression route loaded</p>} />
        </Routes>
      </MemoryRouter>,
    );

    const link = screen.getByRole('link', { name: /view progression/i });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', '/progression/Bench%20Press');

    fireEvent.click(link);

    expect(screen.getByText('Progression route loaded')).toBeInTheDocument();
  });
});

