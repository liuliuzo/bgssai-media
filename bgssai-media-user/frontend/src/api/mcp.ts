import client from './client';

export interface McpTokenView {
  id: number;
  name: string;
  token_masked: string;
  status?: string;
  last_used_at?: string | null;
  created_at?: string;
  token?: string;
}

export async function listMcpTokens() {
  const { data } = await client.get<McpTokenView[]>('/mcp-tokens');
  return data;
}

export async function createMcpToken(name: string) {
  const { data } = await client.post<McpTokenView>('/mcp-tokens', { name });
  return data;
}

export async function revokeMcpToken(id: number) {
  const { data } = await client.post<void>(`/mcp-tokens/${id}/revoke`, {});
  return data;
}
