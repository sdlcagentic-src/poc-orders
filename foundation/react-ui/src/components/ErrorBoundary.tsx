import { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';
import { logger } from '../logging/logger.ts';

interface Props {
  children: ReactNode;
  /** What to show instead of the broken part; defaults to a short message. */
  fallback?: ReactNode;
}

interface State {
  failed: boolean;
}

/** Catches a rendering error below it, logs it (with personal data redacted) and shows a message instead of a blank page. */
export class ErrorBoundary extends Component<Props, State> {
  override state: State = { failed: false };

  static getDerivedStateFromError(): State {
    return { failed: true };
  }

  override componentDidCatch(error: Error, info: ErrorInfo): void {
    logger.error('rendering failed', error, info.componentStack ?? '');
  }

  override render(): ReactNode {
    if (this.state.failed) {
      return this.props.fallback ?? <p role="alert">Something went wrong. Try again, and tell support if it keeps happening.</p>;
    }
    return this.props.children;
  }
}
