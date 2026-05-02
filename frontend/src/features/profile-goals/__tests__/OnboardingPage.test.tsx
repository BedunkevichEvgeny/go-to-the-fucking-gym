import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { describe, expect, it } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { ProfileGoalOnboardingPage } from '../../../pages/ProfileGoalOnboardingPage';

describe('ProfileGoalOnboardingPage', () => {
  it('renders onboarding entry heading and form labels', () => {
    render(
      <MemoryRouter>
        <ProfileGoalOnboardingPage />
      </MemoryRouter>,
    );

    expect(screen.getByText(/My Profile & Goals/i)).toBeTruthy();
    expect(screen.getByLabelText(/Age/i)).toBeTruthy();
    expect(screen.getByLabelText(/Current Weight/i)).toBeTruthy();
    expect(screen.getByLabelText(/Primary Goal/i)).toBeTruthy();
  });

  it('shows submit action for generating initial proposal', () => {
    render(
      <MemoryRouter>
        <ProfileGoalOnboardingPage />
      </MemoryRouter>,
    );

    expect(screen.getByRole('button', { name: /Generate Plan/i })).toBeTruthy();
  });
});

