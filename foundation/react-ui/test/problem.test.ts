// @vitest-environment node
import { describe, expect, it } from 'vitest';
import { ApiError } from '../src/index.ts';

function respond(status: number, body: string, contentType: string, statusText = ''): Response {
  return new Response(body, { status, statusText, headers: { 'content-type': contentType } });
}

describe('ApiError.fromResponse', () => {
  it('reads an RFC 7807 problem document', async () => {
    const error = await ApiError.fromResponse(
      respond(404, '{"title":"Not Found","status":404,"detail":"No such item","traceId":"abc123"}', 'application/problem+json'),
    );
    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(404);
    expect(error.title).toBe('Not Found');
    expect(error.detail).toBe('No such item');
    expect(error.traceId).toBe('abc123');
    expect(error.message).toBe('Not Found: No such item');
  });

  it('falls back to the status text when the body is not a problem document', async () => {
    const error = await ApiError.fromResponse(respond(502, '<html>bad gateway</html>', 'text/html', 'Bad Gateway'));
    expect(error.status).toBe(502);
    expect(error.title).toBe('Bad Gateway');
    expect(error.detail).toBeUndefined();
    expect(error.message).toBe('Bad Gateway');
  });

  it('survives a body that says it is JSON and is not', async () => {
    const error = await ApiError.fromResponse(respond(500, '{broken', 'application/json', 'Internal Server Error'));
    expect(error.title).toBe('Internal Server Error');
  });

  it('survives JSON that is not an object, and fields of the wrong type', async () => {
    expect((await ApiError.fromResponse(respond(500, '[1,2]', 'application/json', 'Oops'))).title).toBe('Oops');
    expect((await ApiError.fromResponse(respond(500, '"text"', 'application/json', 'Oops'))).title).toBe('Oops');
    const error = await ApiError.fromResponse(respond(400, '{"title":42,"detail":{"a":1},"traceId":[]}', 'application/json', 'Bad Request'));
    expect(error.title).toBe('Bad Request');
    expect(error.detail).toBeUndefined();
    expect(error.traceId).toBeUndefined();
  });

  it('uses a generic title when there is no status text', async () => {
    expect((await ApiError.fromResponse(respond(503, '', 'text/plain'))).title).toBe('HTTP 503');
  });

  it('cuts long texts so a hostile server cannot flood the screen', async () => {
    const long = 'x'.repeat(5000);
    const error = await ApiError.fromResponse(respond(400, JSON.stringify({ title: long, detail: long, traceId: long }), 'application/problem+json'));
    expect(error.title).toHaveLength(500);
    expect(error.detail).toHaveLength(500);
    expect(error.traceId).toHaveLength(500);
  });
});
