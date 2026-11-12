export interface BackendStatus {
  application: 'steelstory';
  status: 'UP';
}

// Small hand-written contract adapter; generation is deferred until useful.
export async function fetchStatus(signal: AbortSignal): Promise<BackendStatus> {
  const response = await fetch('/api/v1/status', { signal, headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error(`Backend returned HTTP ${response.status}`);
  const data: unknown = await response.json();
  if (typeof data !== 'object' || data === null ||
      !('application' in data) || data.application !== 'steelstory' ||
      !('status' in data) || data.status !== 'UP') {
    throw new Error('Backend returned an unexpected status response');
  }
  return { application: data.application, status: data.status };
}
