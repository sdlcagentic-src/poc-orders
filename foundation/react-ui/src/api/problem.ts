/** An RFC 7807 problem document, as far as this library reads it. */
export interface ProblemDocument {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  traceId?: string;
}

const MAX_TEXT = 500;

function text(value: unknown): string | undefined {
  return typeof value === 'string' && value.length > 0 ? value.slice(0, MAX_TEXT) : undefined;
}

/**
 * A failed call. Every non-success response becomes one of these, whether the server sent a problem document or something else, so the
 * code that shows errors has one shape to handle. The texts are cut at 500 characters.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly title: string;
  readonly detail: string | undefined;
  readonly traceId: string | undefined;

  constructor(status: number, title: string, detail?: string, traceId?: string) {
    super(detail ? `${title}: ${detail}` : title);
    this.name = 'ApiError';
    this.status = status;
    this.title = title;
    this.detail = detail;
    this.traceId = traceId;
  }

  /** Reads a failed response: a problem document if there is one, otherwise the status text. */
  static async fromResponse(response: Response): Promise<ApiError> {
    let problem: ProblemDocument = {};
    const type = response.headers.get('content-type') ?? '';
    if (type.includes('json')) {
      try {
        const body: unknown = await response.json();
        if (body !== null && typeof body === 'object') {
          problem = body as ProblemDocument;
        }
      } catch {
        // not JSON after all: fall back to the status text
      }
    }
    return new ApiError(
      response.status,
      text(problem.title) ?? (response.statusText || `HTTP ${response.status}`),
      text(problem.detail),
      text(problem.traceId),
    );
  }
}
