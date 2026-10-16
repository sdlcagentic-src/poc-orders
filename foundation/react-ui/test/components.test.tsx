import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, AppShell, ErrorBoundary, ProblemAlert } from '../src/index.ts';

afterEach(cleanup);

function Broken(): never {
  throw new Error('boom for jane@example.com');
}

describe('ErrorBoundary', () => {
  it('shows a message instead of a blank page and logs what the masking option allows', () => {
    const logged = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    render(
      <ErrorBoundary>
        <Broken />
      </ErrorBoundary>,
    );
    expect(screen.getByRole('alert').textContent).toContain('Something went wrong');
    const output = JSON.stringify(logged.mock.calls);
    expect(output).not.toContain('jane@example.com');
    expect(output).toContain('[email]');
    logged.mockRestore();
  });

  it('shows a custom fallback when given one, and the children when nothing fails', () => {
    const logged = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    render(
      <ErrorBoundary fallback={<p>custom</p>}>
        <Broken />
      </ErrorBoundary>,
    );
    expect(screen.getByText('custom')).toBeTruthy();
    cleanup();
    render(
      <ErrorBoundary>
        <p>fine</p>
      </ErrorBoundary>,
    );
    expect(screen.getByText('fine')).toBeTruthy();
    logged.mockRestore();
  });
});

describe('ProblemAlert', () => {
  it('shows title, detail and reference of an ApiError', () => {
    render(<ProblemAlert error={new ApiError(404, 'Not Found', 'No such item', 'trace-9')} />);
    expect(screen.getByRole('alert').textContent).toContain('Not Found');
    expect(screen.getByText('No such item')).toBeTruthy();
    expect(screen.getByText('Reference: trace-9')).toBeTruthy();
  });

  it('leaves out what is missing', () => {
    render(<ProblemAlert error={new ApiError(500, 'Server Error')} />);
    expect(screen.queryByText(/Reference/)).toBeNull();
  });

  it('shows a generic message for anything else and never the raw error', () => {
    render(<ProblemAlert error={new Error('secret internals')} />);
    expect(screen.getByRole('alert').textContent).toBe('Something went wrong.');
  });
});

describe('AppShell', () => {
  it('shows the title, the navigation and the current route', () => {
    render(
      <MemoryRouter initialEntries={['/second']}>
        <Routes>
          <Route
            element={
              <AppShell
                title="Demo app"
                nav={[
                  { to: '/first', label: 'First' },
                  { to: '/second', label: 'Second' },
                ]}
              />
            }
          >
            <Route path="/first" element={<p>first page</p>} />
            <Route path="/second" element={<p>second page</p>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByRole('heading', { name: 'Demo app' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'First' }).getAttribute('href')).toBe('/first');
    expect(screen.getByRole('link', { name: 'Second' }).getAttribute('aria-current')).toBe('page');
    expect(screen.getByText('second page')).toBeTruthy();
    expect(screen.queryByText('first page')).toBeNull();
  });

  it('keeps the frame when a screen breaks', () => {
    const logged = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    render(
      <MemoryRouter initialEntries={['/broken']}>
        <Routes>
          <Route element={<AppShell title="Demo app" nav={[]} />}>
            <Route path="/broken" element={<Broken />} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByRole('heading', { name: 'Demo app' })).toBeTruthy();
    expect(screen.getByRole('alert')).toBeTruthy();
    logged.mockRestore();
  });
});
