import { useCallback, useEffect, useState } from 'react';
import { Card, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { fetchIngestLogs } from '../api/ingest';
import type { MediaIngestLog } from '../types';

const statusTag = (status?: string) => {
  if (status === 'READY' || status === 'success') {
    return <Tag color="green">READY</Tag>;
  }
  if (status === 'PENDING') {
    return <Tag color="gold">PENDING</Tag>;
  }
  if (status === 'FAILED' || status === 'failed' || status === 'error') {
    return <Tag color="red">FAILED</Tag>;
  }
  return <Tag>{status || '未知'}</Tag>;
};

export default function IngestLogs() {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<MediaIngestLog[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const result = await fetchIngestLogs({
        page_num: pageNum,
        page_size: pageSize,
      });
      setData(result.list);
      setTotal(result.total);
    } catch {
      // handled
    } finally {
      setLoading(false);
    }
  }, [pageNum, pageSize]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const columns: ColumnsType<MediaIngestLog> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '外部引用', dataIndex: 'external_ref', width: 160, ellipsis: true },
    { title: '短剧ID', dataIndex: 'drama_id', width: 100 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (val: string) => statusTag(val),
    },
    { title: '消息', dataIndex: 'message', ellipsis: true },
    {
      title: '载荷',
      dataIndex: 'payload_json',
      ellipsis: true,
      render: (val: string) => val || '-',
    },
    {
      title: '创建时间',
      dataIndex: 'created_at',
      width: 180,
      render: (val: string) => (val ? new Date(val).toLocaleString('zh-CN') : '-'),
    },
  ];

  return (
    <Card title="入库日志" extra="short → media 契约摄入（READY 才可播）">
      <Table
        rowKey="id"
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
