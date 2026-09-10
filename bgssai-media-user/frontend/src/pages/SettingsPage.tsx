import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Input,
  List,
  Row,
  Space,
  Typography,
  message,
} from 'antd';
import {
  ApiOutlined,
  CopyOutlined,
  DeleteOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import {
  createMcpToken,
  listMcpTokens,
  revokeMcpToken,
  type McpTokenView,
} from '@/api/mcp';

const { Title, Paragraph, Text } = Typography;

const CONNECTORS = [
  {
    key: 'claude',
    name: 'Claude',
    hint: 'Claude Desktop 经 mcp-remote 桥接 Streamable HTTP',
  },
  {
    key: 'codex',
    name: 'Codex',
    hint: '在 MCP 配置中填写本站 /api/mcp 与 Bearer PAT',
  },
  {
    key: 'cursor',
    name: 'Cursor',
    hint: '原生 Streamable HTTP：.cursor/mcp.json',
  },
  {
    key: 'grok-bot',
    name: 'Grok Bot',
    hint: 'Bot 侧添加远程 MCP，使用同一 PAT',
  },
  {
    key: 'bgssai-bot',
    name: 'bgssai-bot',
    hint: 'BGSSAI Bot 托管操作媒体仓，PAT 或 Chat 下发 MCP',
  },
] as const;

export default function SettingsPage() {
  const [tokens, setTokens] = useState<McpTokenView[]>([]);
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState('');
  const [freshToken, setFreshToken] = useState<string | null>(null);
  const [revokingId, setRevokingId] = useState<number | null>(null);

  const endpoint = useMemo(
    () => `${window.location.origin}/api/mcp`,
    [],
  );

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const list = await listMcpTokens();
      setTokens(list || []);
    } catch {
      // handled by client
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const onCreate = async () => {
    const trimmed = name.trim();
    if (!trimmed) {
      message.error('请先给令牌起个名字（如 Claude Desktop）');
      return;
    }
    setCreating(true);
    try {
      const created = await createMcpToken(trimmed);
      setFreshToken(created.token || null);
      setName('');
      await load();
    } catch {
      // handled
    } finally {
      setCreating(false);
    }
  };

  const onRevoke = async (token: McpTokenView) => {
    if (
      !window.confirm(
        `确认吊销「${token.name}」？使用该令牌的 AI 客户端将立即断开。`,
      )
    ) {
      return;
    }
    setRevokingId(token.id);
    try {
      await revokeMcpToken(token.id);
      message.success('令牌已吊销');
      await load();
    } catch {
      // handled
    } finally {
      setRevokingId(null);
    }
  };

  const copyText = async (value: string) => {
    try {
      await navigator.clipboard.writeText(value);
      message.success('已复制');
    } catch {
      message.error('复制失败，请手动选中复制');
    }
  };

  const sampleConfig = `// Cursor（.cursor/mcp.json）——原生 Streamable HTTP：
{
  "mcpServers": {
    "bgssai-media": {
      "url": "${endpoint}",
      "headers": { "Authorization": "Bearer <你的令牌>" }
    }
  }
}

// Claude Desktop（claude_desktop_config.json）——经 mcp-remote 桥接：
{
  "mcpServers": {
    "bgssai-media": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "${endpoint}",
               "--header", "Authorization: Bearer <你的令牌>"]
    }
  }
}

// Codex / Grok Bot / bgssai-bot：远程 MCP URL = ${endpoint}
// Header: Authorization: Bearer <你的令牌>`;

  return (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <div>
        <Title level={2} style={{ marginBottom: 8 }}>
          Connect your AI tools to bgssai
        </Title>
        <Paragraph type="secondary" style={{ marginBottom: 0 }}>
          把 Claude、Codex、Cursor、Grok Bot、bgssai-bot 连到 BGSSAI
          媒体：检索已发布短剧、查看分集、读取你的继续观看进度（只读）。
        </Paragraph>
      </div>

      <Row gutter={[16, 16]}>
        {CONNECTORS.map((item) => (
          <Col xs={24} sm={12} lg={8} key={item.key}>
            <Card size="small">
              <Space>
                <ApiOutlined />
                <div>
                  <Text strong>{item.name}</Text>
                  <div>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {item.hint}
                    </Text>
                  </div>
                </div>
              </Space>
            </Card>
          </Col>
        ))}
      </Row>

      <Card title="MCP 接入">
        <Paragraph type="secondary">
          端点 <Text code>{endpoint}</Text>
          ，鉴权用下方令牌（Authorization: Bearer）。
        </Paragraph>

        {freshToken && (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
            message="令牌已创建，仅显示这一次，请立即保存"
            description={
              <Space direction="vertical" style={{ width: '100%' }}>
                <Text code copyable={{ text: freshToken }}>
                  {freshToken}
                </Text>
                <Space>
                  <Button
                    size="small"
                    icon={<CopyOutlined />}
                    onClick={() => copyText(freshToken)}
                  >
                    复制
                  </Button>
                  <Button size="small" onClick={() => setFreshToken(null)}>
                    我已保存
                  </Button>
                </Space>
              </Space>
            }
          />
        )}

        <Space.Compact style={{ width: '100%', marginBottom: 16 }}>
          <Input
            maxLength={64}
            placeholder="令牌名称，如 Claude Desktop"
            value={name}
            onChange={(e) => setName(e.target.value)}
            disabled={creating}
          />
          <Button
            type="primary"
            icon={<PlusOutlined />}
            loading={creating}
            onClick={onCreate}
          >
            创建令牌
          </Button>
        </Space.Compact>

        <List
          loading={loading}
          locale={{ emptyText: '还没有令牌。创建一枚即可在 AI 客户端里连接本站。' }}
          dataSource={tokens}
          renderItem={(item) => (
            <List.Item
              actions={[
                <Button
                  key="revoke"
                  danger
                  type="link"
                  icon={<DeleteOutlined />}
                  loading={revokingId === item.id}
                  onClick={() => onRevoke(item)}
                >
                  吊销
                </Button>,
              ]}
            >
              <List.Item.Meta
                title={item.name}
                description={
                  <Space split={<Text type="secondary">|</Text>}>
                    <Text code>{item.token_masked}</Text>
                    <Text type="secondary">
                      {item.last_used_at
                        ? `最近使用 ${new Date(item.last_used_at).toLocaleString('zh-CN')}`
                        : '从未使用'}
                    </Text>
                  </Space>
                }
              />
            </List.Item>
          )}
        />

        <details style={{ marginTop: 16 }}>
          <summary style={{ cursor: 'pointer' }}>
            客户端配置示例（Claude / Cursor / Codex / Grok Bot / bgssai-bot）
          </summary>
          <pre
            style={{
              marginTop: 12,
              padding: 12,
              background: '#f5f5f5',
              borderRadius: 8,
              overflowX: 'auto',
              fontSize: 12,
            }}
          >
            {sampleConfig}
          </pre>
          <Button
            size="small"
            icon={<CopyOutlined />}
            onClick={() => copyText(sampleConfig)}
          >
            复制示例
          </Button>
        </details>
      </Card>
    </Space>
  );
}
