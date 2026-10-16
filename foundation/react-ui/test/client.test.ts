// @vitest-environment node
import { describe, expect, it } from 'vitest';
import { ApiError, REQUEST_TIMEOUT_MS, createApiClient } from '../src/index.ts';
import type { paths } from './generated/sample';

function json(body: unknown, init: ResponseInit = {}): Response {
  return new Response(JSON.stringify(body), { headers: { 'content-type': 'application/json' }, ...init });
}

describe('createApiClient', () => {
  it('calls the contract operation and returns the typed data', async () => {
    const seen: Request[] = [];
    const client = createApiClient<paths>({
      baseUrl: 'https://api.example.test',
      fetch: async (request) => {
        seen.push(request);
        return json({ id: '7', name: 'Seven' });
      },
    });
    const { data } = await client.GET('/items/{id}', { params: { path: { id: '7' } } });
    expect(data).toEqual({ id: '7', name: 'Seven' });
    expect(seen).toHaveLength(1);
    expect(seen[0]?.url).toBe('https://api.example.test/items/7');
    expect(seen[0]?.method).toBe('GET');
  });

  it('turns a problem response into an ApiError', async () => {
    const client = createApiClient<paths>({
      baseUrl: 'https://api.example.test',
      fetch: async () =>
        new Response('{"title":"Not Found","detail":"No such item","traceId":"t-1"}', { status: 404, headers: { 'content-type': 'application/problem+json' } }),
    });
    const failure = await client.GET('/items/{id}', { params: { path: { id: 'x' } } }).catch((error: unknown) => error);
    expect(failure).toBeInstanceOf(ApiError);
    expect(failure).toMatchObject({ status: 404, title: 'Not Found', detail: 'No such item', traceId: 't-1' });
  });

  it('turns a network failure into an ApiError and never leaks the original as is', async () => {
    const client = createApiClient<paths>({
      baseUrl: 'https://api.example.test',
      fetch: async () => {
        throw new TypeError('connection refused');
      },
    });
    const failure = await client.GET('/items/{id}', { params: { path: { id: '1' } } }).catch((error: unknown) => error);
    expect(failure).toBeInstanceOf(ApiError);
    expect(failure).toMatchObject({ status: 0, title: 'Network error', detail: 'connection refused' });
  });

  it('passes an ApiError from a wrapped fetch through unchanged', async () => {
    const original = new ApiError(401, 'Unauthorized', 'Session expired');
    const client = createApiClient<paths>({
      baseUrl: 'https://api.example.test',
      fetch: async () => {
        throw original;
      },
    });
    const failure = await client.GET('/items/{id}', { params: { path: { id: '1' } } }).catch((error: unknown) => error);
    expect(failure).toBe(original);
  });

  it('abandons a request that takes longer than the timeout', async () => {
    const client = createApiClient<paths>({
      baseUrl: 'https://api.example.test',
      timeoutMs: 30,
      fetch: (request) =>
        new Promise<Response>((_, reject) => {
          request.signal.addEventListener('abort', () => reject(request.signal.reason));
        }),
    });
    const failure = await client.GET('/items/{id}', { params: { path: { id: '1' } } }).catch((error: unknown) => error);
    expect(failure).toBeInstanceOf(ApiError);
    expect(failure).toMatchObject({ status: 0, title: 'Request timed out' });
  });

  it('lets the caller cancel a request without calling it a timeout', async () => {
    const controller = new AbortController();
    const client = createApiClient<paths>({
      baseUrl: 'https://api.example.test',
      timeoutMs: 5000,
      fetch: (request) =>
        new Promise<Response>((_, reject) => {
          request.signal.addEventListener('abort', () => reject(request.signal.reason));
        }),
    });
    const pending = client.GET('/items/{id}', { params: { path: { id: '1' } }, signal: controller.signal }).catch((error: unknown) => error);
    controller.abort();
    const failure = await pending;
    expect(failure).toBeInstanceOf(ApiError);
    expect((failure as ApiError).title).toBe('Network error');
  });

  it('has the default timeout of the profile', () => {
    expect(REQUEST_TIMEOUT_MS).toBe(5000);
  });

  it('sends the bearer token, also when it comes from an asynchronous source', async () => {
    const seen: Array<string | null> = [];
    const client = createApiClient<paths>({
      baseUrl: 'https://api.example.test',
      getToken: async () => 'secret-token',
      fetch: async (request) => {
        seen.push(request.headers.get('authorization'));
        return json({ id: '1', name: 'One' });
      },
    });
    await client.GET('/items/{id}', { params: { path: { id: '1' } } });
    expect(seen).toEqual(['Bearer secret-token']);
  });

  it('sends no Authorization header when there is no token', async () => {
    const seen: Array<string | null> = [];
    const client = createApiClient<paths>({
      baseUrl: 'https://api.example.test',
      getToken: () => undefined,
      fetch: async (request) => {
        seen.push(request.headers.get('authorization'));
        return json({ id: '1', name: 'One' });
      },
    });
    await client.GET('/items/{id}', { params: { path: { id: '1' } } });
    expect(seen).toEqual([null]);
  });
});
