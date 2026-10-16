import { ApiError } from '../api/problem.ts';

/** Shows a failed call: the title, the detail when there is one, and the trace id so a user can quote it. Anything else becomes a generic message. */
export function ProblemAlert(props: { error: unknown }) {
  const { error } = props;
  if (!(error instanceof ApiError)) {
    return <p role="alert">Something went wrong.</p>;
  }
  return (
    <div role="alert">
      <strong>{error.title}</strong>
      {error.detail ? <p>{error.detail}</p> : null}
      {error.traceId ? <small>Reference: {error.traceId}</small> : null}
    </div>
  );
}
