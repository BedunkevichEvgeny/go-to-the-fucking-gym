import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { describe, expect, it } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { ProfileGoalOnboardingPage } from '../../../pages/ProfileGoalOnboardingPage';

const renderPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ProfileGoalOnboardingPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
};

describe('ProfileGoalOnboardingPage', () => {
  it('renders onboarding entry heading and form labels', () => {
    renderPage();

    expect(screen.getByText(/My Profile & Goals/i)).toBeTruthy();
    expect(screen.getByLabelText(/Age/i)).toBeTruthy();
    expect(screen.getByLabelText(/Current Weight/i)).toBeTruthy();
    expect(screen.getByLabelText(/Primary Goal/i)).toBeTruthy();
  });

  it('shows submit action for generating initial proposal', () => {
    renderPage();

    expect(screen.getByRole('button', { name: /Generate Plan/i })).toBeTruthy();
  });
});

