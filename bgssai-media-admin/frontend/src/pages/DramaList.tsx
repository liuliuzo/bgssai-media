import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Input,
  Select,
  Space,
  Table,
  Tag,
  message,
  Modal,
} from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import {
  deleteDrama,
  fetchDramaPage,
  publishDrama,
} from '../api/drama';
import type { MediaDrama } from '../types';

const STATUS_OPTIONS = [
  { label: '全部状态', value: '' },
  { label: '草稿', value: 'draft' },
  { label: '已发布', value: 'published' },
];

const statusTag = (status?: string) => {
  if (status === 'published') {
    return <Tag color="green">已发布</Tag>;
  }
  return <Tag>草稿</Tag>;
};

export default function DramaList() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<MediaDrama[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('');

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const result = await fetchDramaPage({
        page_num: pageNum,
        page_size: pageSize,
        keyword: keyword || undefined,
        status: status || undefined,
      });
      setData(result.list);
      setTotal(result.total);
    } catch {
      // handled by interceptor
    } finally {
      setLoading(false);
    }
  }, [pageNum, pageSize, keyword, status]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handlePublish = async (record: MediaDrama, published: boolean) => {
    try {
      await publishDrama(record.id!, published);
      message.success(published ? '发布成功' : '已取消发布');
      loadData();
    } catch {
      // handled
    }
  };

  const handleDelete = (record: MediaDrama) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除短剧「${record.title}」吗？此操作不可恢复。`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        await deleteDrama(record.id!);
        message.success('删除成功');
        loadData();
      },
    });
  };

  const columns: ColumnsType<MediaDrama> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '标题', dataIndex: 'title', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (val: string) => statusTag(val),
    },
    { title: '来源', dataIndex: 'source', width: 100 },
    {
      title: '封面',
      dataIndex: 'cover_url',
      width: 80,
      render: (url: string) =>
        url ? (
          <img
            src={url}
            alt="封面"
            style={{ width: 40, height: 56, objectFit: 'cover' }}
          />
        ) : (
          '-'
        ),
    },
    {
      title: '创建时间',
      dataIndex: 'created_at',
      width: 180,
      render: (val: string) => (val ? new Date(val).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '操作',
      key: 'actions',
      width: 280,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small" wrap>
          <Button type="link" size="small" onClick={() => navigate(`/dramas/${record.id}`)}>
            详情
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => navigate(`/dramas/${record.id}/edit`)}
          >
            编辑
          </Button>
          {record.status === 'published' ? (
            <Button
              type="link"
              size="small"
              onClick={() => handlePublish(record, false)}
            >
              取消发布
            </Button>
          ) : (
            <Button
              type="link"
              size="small"
              onClick={() => handlePublish(record, true)}
            >
              发布
            </Button>
          )}
          <Button
            type="link"
            size="small"
            danger
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Card
      title="短剧列表"
      extra={
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => navigate('/dramas/create')}
        >
          新建短剧
        </Button>
      }
    >
      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          placeholder="搜索标题"
          allowClear
          style={{ width: 240 }}
          onSearch={(val) => {
            setKeyword(val);
            setPageNum(1);
          }}
        />
        <Select
          value={status}
          options={STATUS_OPTIONS}
          style={{ width: 140 }}
          onChange={(val) => {
            setStatus(val);
            setPageNum(1);
          }}
        />
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
