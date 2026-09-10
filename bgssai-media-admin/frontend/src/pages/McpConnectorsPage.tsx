import { Card, Col, Row, Space, Typography, Alert } from 'antd';
import { ApiOutlined, LinkOutlined } from '@ant-design/icons';

const { Title, Paragraph, Text } = Typography;

const CONNECTORS = [
  { name: 'Claude', detail: 'Claude Desktop + mcp-remote → POST /api/mcp' },
  { name: 'Codex', detail: '远程 MCP URL + Authorization: Bearer PAT' },
  { name: 'Cursor', detail: '.cursor/mcp.json 原生 Streamable HTTP' },
  { name: 'Grok Bot', detail: 'Bot 侧挂载远程 MCP，共用用户 PAT' },
  { name: 'bgssai-bot', detail: '产品托管：PAT 或 Chat 登录后下发 MCP' },
];

/**
 * Admin 侧管理说明页：连接器清单与端点约定。
 * PAT 自助创建在用户端 /settings（对照 blog），避免 admin 跨服务代发用户令牌。
 */
export default function McpConnectorsPage() {
  const userEndpointHint =
    '用户端后端：POST /api/mcp （Authorization: Bearer <PAT>）';
  const tokenApiHint =
    '用户端：GET/POST /api/mcp-tokens ，POST /api/mcp-tokens/{id}/revoke';

  return (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <div>
        <Title level={3} style={{ marginBottom: 8 }}>
          Connect your AI tools to bgssai
        </Title>
        <Paragraph type="secondary" style={{ marginBottom: 0 }}>
          媒体仓 MCP 连接器管理面：说明如何将 Claude / Codex / Cursor / Grok Bot /
          bgssai-bot 接到本产品。个人令牌（PAT）由用户在用户端设置页自助签发。
        </Paragraph>
      </div>

      <Alert
        type="info"
        showIcon
        message="PAT 签发位置"
        description={
          <div>
            <div>用户前端：设置 → MCP 接入（/settings）</div>
            <div>
              <Text code>{tokenApiHint}</Text>
            </div>
            <div>
              <Text code>{userEndpointHint}</Text>
            </div>
            <div>
              文档：<Text code>docs/feature/mcp.md</Text>、
              <Text code>docs/api/mcp.md</Text>
            </div>
          </div>
        }
      />

      <Row gutter={[16, 16]}>
        {CONNECTORS.map((item) => (
          <Col xs={24} md={12} lg={8} key={item.name}>
            <Card>
              <Space align="start">
                <ApiOutlined style={{ fontSize: 20 }} />
                <div>
                  <Title level={5} style={{ margin: 0 }}>
                    {item.name}
                  </Title>
                  <Paragraph type="secondary" style={{ marginBottom: 0 }}>
                    {item.detail}
                  </Paragraph>
                </div>
              </Space>
            </Card>
          </Col>
        ))}
      </Row>

      <Card title="最小只读工具">
        <ul style={{ marginBottom: 0, paddingLeft: 20 }}>
          <li>
            <Text code>ping</Text> — 连通性
          </li>
          <li>
            <Text code>list_published_dramas</Text> — 已发布短剧列表
          </li>
          <li>
            <Text code>get_drama</Text> — 已发布剧集详情与分集
          </li>
          <li>
            <Text code>list_continue_watching</Text> — 当前用户继续观看
          </li>
        </ul>
        <Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0 }}>
          <LinkOutlined /> 不做写入、不做摄入、不暴露中间件密钥。
        </Paragraph>
      </Card>
    </Space>
  );
}
