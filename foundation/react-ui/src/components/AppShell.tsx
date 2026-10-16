import type { ReactNode } from 'react';
import { NavLink, Outlet } from 'react-router';
import { ErrorBoundary } from './ErrorBoundary.tsx';

export interface NavItem {
  to: string;
  label: string;
}

/**
 * The frame of every screen: a title, the navigation and the place where the current route is shown (an {@link Outlet}), inside an
 * {@link ErrorBoundary}. Use it as the element of a parent route in react-router.
 */
export function AppShell(props: { title: string; nav: NavItem[]; actions?: ReactNode }) {
  return (
    <div>
      <header>
        <h1>{props.title}</h1>
        <nav aria-label="Main">
          {props.nav.map((item) => (
            <NavLink key={item.to} to={item.to}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        {props.actions}
      </header>
      <main>
        <ErrorBoundary>
          <Outlet />
        </ErrorBoundary>
      </main>
    </div>
  );
}
