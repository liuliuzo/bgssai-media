import { useCallback, useEffect, useState } from 'react';
import { Button, Card, Input, Select, Space, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import { fetchSupportPage, type SupportSessionRow, type SupportTicket } from '../api/support';

const statusTag = (status?: string) => {
  if (status === 'pending') return <Tag color="gold">待处理</Tag>;
  if (status === 'open') return <Tag color="blue">进行中</Tag>;
  if (status === 'closed') return <Tag>已关闭</Tag>;
  return <Tag>{status || '-'}</Tag>;
};

const ticketTag = (ticket?: SupportTicket | null) => {
  if (!ticket) return <span style={{ color: '#94a3b8' }}>无</span>;
  const color =
    ticket.status === 'open'
      ? 'blue'
      : ticket.status === 'pending'
        ? 'gold'
        : ticket.status === 'resolved'
          ? 'green'
          : 'default';
  return (
    <Space size={4}>
      <Tag color={color}>#{ticket.id}</Tag>
      <span>{ticket.status}</span>
    </Space>
  );
};

export default function SupportSessions() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<SupportSessionRow[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [status, setStatus] = useState<string | undefined>();
  const [keyword, setKeyword] = useState('');

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const result = await fetchSupportPage({
        page_num: pageNum,
        page_size: pageSize,
        status,
        keyword: keyword.trim() || undefined,
      });
      setData(result.list);
      setTotal(result.total);
    } catch {
      // handled by request interceptor
    } finally {
      setLoading(false);
    }
  }, [pageNum, pageSize, status, keyword]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const columns: ColumnsType<SupportSessionRow> = [
    {
      title: 'ID',
      width: 80,
      render: (_, row) => row.session.id,
    },
    {
      title: '主题',
      ellipsis: true,
      render: (_, row) => row.session.subject || '-',
    },
    {
      title: '联系方式',
      width: 200,
      render: (_, row) =>
        [row.session.contact_name, row.session.contact_email, row.session.contact_phone]
          .filter(Boolean)
          .join(' / ') ||
        (row.session.user_id ? `用户#${row.session.user_id}` : '匿名'),
    },
    {
      title: '会话状态',
      width: 100,
      render: (_, row) => statusTag(row.session.status),
    },
    {
      title: '工单',
      width: 160,
      render: (_, row) => ticketTag(row.ticket),
    },
    {
      title: '未读',
      width: 80,
      render: (_, row) =>
        row.session.admin_unread && row.session.admin_unread > 0 ? (
          <Tag color="red">{row.session.admin_unread}</Tag>
        ) : (
          <span style={{ color: '#94a3b8' }}>0</span>
        ),
    },
    {
      title: '最近消息',
      width: 180,
      render: (_, row) =>
        row.session.last_message_at
          ? new Date(row.session.last_message_at).toLocaleString('zh-CN')
          : '-',
    },
    {
      title: '操作',
      width: 100,
      render: (_, row) => (
        <Button type="link" onClick={() => navigate(`/support/${row.session.id}`)}>
          处理
        </Button>
      ),
    },
  ];

  return (
    <Card
      title="在线客服"
      extra="会话 + 工单处理台（chat-first + ticket-from-chat）"
    >
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          allowClear
          placeholder="会话状态"
          style={{ width: 140 }}
          value={status}
          onChange={(v) => {
            setPageNum(1);
            setStatus(v);
          }}
          options={[
            { value: 'pending', label: '待处理' },
            { value: 'open', label: '进行中' },
            { value: 'closed', label: '已关闭' },
          ]}
        />
        <Input.Search
          allowClear
          placeholder="按主题搜索"
          style={{ width: 240 }}
          onSearch={(v) => {
            setPageNum(1);
            setKeyword(v);
          }}
        />
        <Button onClick={() => void loadData()}>刷新</Button>
      </Space>
      <Table
        rowKey={(row) => String(row.session.id)}
        loading={loading}
        columns={columns}
        dataSource={data}
        scroll={{ x: 1100 }}
        pagination={{
          current: pageNum,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (page, size) => {
            setPageNum(page);
            setPageSize(size);
          },
        }}
      />
    </Card>
  );
}
