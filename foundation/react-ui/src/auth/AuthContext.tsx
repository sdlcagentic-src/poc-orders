import { createContext, useContext } from 'react';
import type { ReactNode } from 'react';

export type TokenSource = () => string | undefined | Promise<string | undefined>;

const AuthContext = createContext<TokenSource | undefined>(undefined);

/** Carries the function that supplies the bearer token to the components below it. */
export function AuthProvider(props: { getToken: TokenSource; children: ReactNode }) {
  return <AuthContext.Provider value={props.getToken}>{props.children}</AuthContext.Provider>;
}

/** The token source of the nearest {@link AuthProvider}; fails loudly when there is none instead of sending requests without credentials. */
export function useAuth(): { getToken: TokenSource } {
  const getToken = useContext(AuthContext);
  if (!getToken) {
    throw new Error('useAuth must be used inside an AuthProvider');
  }
  return { getToken };
}
