import createClient from 'openapi-fetch';
import { REQUEST_TIMEOUT_MS } from '../config.ts';
import { ApiError } from './problem.ts';

export interface ApiClientOptions {
  /** Where the service is, for example "/api" behind a gateway or "https://host". */
  baseUrl: string;
  /** Milliseconds before a request is abandoned; defaults to the profile's request-timeout-ms. */
  timeoutMs?: number;
  /** The bearer token to send, if any. May be asynchronous (for example a refresh). */
  getToken?: () => string | undefined | Promise<string | undefined>;
  /** For tests: replaces fetch. */
  fetch?: (request: Request) => Promise<Response>;
}

/**
 * A typed client for an OpenAPI contract: pass the `paths` type generated from the contract (openapi-typescript). Every request gets
 * a timeout; a non-success response, a timeout or a network failure rejects with an {@link ApiError}.
 */
export function createApiClient<Paths extends object>(options: ApiClientOptions) {
  const timeoutMs = options.timeoutMs ?? REQUEST_TIMEOUT_MS;
  const send = options.fetch ?? ((request: Request) => globalThis.fetch(request));

  const guarded = async (original: Request): Promise<Response> => {
    const token = options.getToken ? await options.getToken() : undefined;
    const request = token ? new Request(original, { headers: withBearer(original.headers, token) }) : original;
    const signal = AbortSignal.any([request.signal, AbortSignal.timeout(timeoutMs)]);
    let response: Response;
    try {
      response = await send(new Request(request, { signal }));
    } catch (error) {
      if (error instanceof ApiError) {
        throw error;
      }
      if (signal.aborted && !request.signal.aborted) {
        throw new ApiError(0, 'Request timed out', `No answer within ${timeoutMs} ms`);
      }
      throw new ApiError(0, 'Network error', error instanceof Error ? error.message : undefined);
    }
    if (!response.ok) {
      throw await ApiError.fromResponse(response);
    }
    return response;
  };

  return createClient<Paths>({ baseUrl: options.baseUrl, fetch: guarded });
}

function withBearer(headers: Headers, token: string): Headers {
  const copy = new Headers(headers);
  copy.set('Authorization', `Bearer ${token}`);
  return copy;
}
