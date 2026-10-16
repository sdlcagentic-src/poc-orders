import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from '../src/index.ts';

afterEach(cleanup);

function Show() {
  const { getToken } = useAuth();
  return <p>{String(getToken())}</p>;
}

describe('AuthProvider', () => {
  it('gives the components below it the token source', () => {
    render(
      <AuthProvider getToken={() => 'abc'}>
        <Show />
      </AuthProvider>,
    );
    expect(screen.getByText('abc')).toBeTruthy();
  });

  it('refuses to be used without a provider instead of working without credentials', () => {
    const logged = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    expect(() => render(<Show />)).toThrow('useAuth must be used inside an AuthProvider');
    logged.mockRestore();
  });
});
