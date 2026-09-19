import { useCallback, useEffect, useState } from 'react';
import { Button, Card, Input, Select, Space, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import { fetchSupportPage, type SupportSession } from '../api/support';

const statusTag = (status?: string) => {
  if (status === 'pending') return <Tag color="gold">待处理</Tag>;
  if (status === 'open') return <Tag color="blue">进行中</Tag>;
  if (status === 'closed') return <Tag>已关闭</Tag>;
  return <Tag>{status || '-'}</Tag>;
};

export default function SupportSessions() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<SupportSession[]>([]);
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

  const columns: ColumnsType<SupportSession> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    {
      title: '主题',
      dataIndex: 'subject',
      ellipsis: true,
      render: (val: string) => val || '-',
    },
    {
      title: '联系方式',
      width: 200,
      render: (_, row) =>
        [row.contact_name, row.contact_email, row.contact_phone].filter(Boolean).join(' / ') ||
        (row.user_id ? `用户#${row.user_id}` : '匿名'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (val: string) => statusTag(val),
    },
    {
      title: '未读',
      dataIndex: 'admin_unread',
      width: 80,
      render: (val?: number) =>
        val && val > 0 ? <Tag color="red">{val}</Tag> : <span style={{ color: '#94a3b8' }}>0</span>,
    },
    {
      title: '最近消息',
      dataIndex: 'last_message_at',
      width: 180,
      render: (val?: string) => (val ? new Date(val).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '操作',
      width: 100,
      render: (_, row) => (
        <Button type="link" onClick={() => navigate(`/support/${row.id}`)}>
          处理
        </Button>
      ),
    },
  ];

  return (
    <Card
      title="在线客服"
      extra="用户留言会话处理台（support=in-app messaging）"
    >
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          allowClear
          placeholder="状态"
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
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data}
        scroll={{ x: 1000 }}
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
