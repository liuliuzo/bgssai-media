import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate, useParams } from 'react-router-dom';
import {
  createEpisode,
  deleteEpisode,
  fetchDramaDetail,
  updateEpisode,
} from '../api/drama';
import type { MediaDrama, MediaEpisode } from '../types';

const STATUS_OPTIONS = [
  { label: '草稿', value: 'draft' },
  { label: '已发布', value: 'published' },
];

const statusTag = (status?: string) => {
  if (status === 'published') {
    return <Tag color="green">已发布</Tag>;
  }
  return <Tag>草稿</Tag>;
};

export default function DramaDetail() {
  const { id } = useParams<{ id: string }>();
  const dramaId = Number(id);
  const navigate = useNavigate();

  const [loading, setLoading] = useState(false);
  const [drama, setDrama] = useState<MediaDrama | null>(null);
  const [episodes, setEpisodes] = useState<MediaEpisode[]>([]);

  const [modalOpen, setModalOpen] = useState(false);
  const [editingEpisode, setEditingEpisode] = useState<MediaEpisode | null>(null);
  const [form] = Form.useForm<MediaEpisode>();
  const [submitting, setSubmitting] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const result = await fetchDramaDetail(dramaId);
      setDrama(result.drama);
      setEpisodes(result.episodes);
    } catch {
      // handled
    } finally {
      setLoading(false);
    }
  }, [dramaId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const openCreateModal = () => {
    setEditingEpisode(null);
    form.resetFields();
    form.setFieldsValue({ drama_id: dramaId, status: 'draft' });
    setModalOpen(true);
  };

  const openEditModal = (episode: MediaEpisode) => {
    setEditingEpisode(episode);
    form.setFieldsValue(episode);
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      if (editingEpisode) {
        await updateEpisode({ ...values, id: editingEpisode.id, drama_id: dramaId });
        message.success('剧集更新成功');
      } else {
        await createEpisode({ ...values, drama_id: dramaId });
        message.success('剧集创建成功');
      }
      setModalOpen(false);
      loadData();
    } catch {
      // handled
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = (episode: MediaEpisode) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除第 ${episode.ep_no} 集「${episode.title}」吗？`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        await deleteEpisode(episode.id!);
        message.success('删除成功');
        loadData();
      },
    });
  };

  const columns: ColumnsType<MediaEpisode> = [
    { title: '集号', dataIndex: 'ep_no', width: 80 },
    { title: '标题', dataIndex: 'title', ellipsis: true },
    {
      title: '时长(秒)',
      dataIndex: 'duration_sec',
      width: 100,
      render: (val: number) => val ?? '-',
    },
    {
      title: '媒体地址',
      dataIndex: 'media_url',
      ellipsis: true,
      render: (url: string) =>
        url ? (
          <a href={url} target="_blank" rel="noreferrer">
            {url}
          </a>
        ) : (
          '-'
        ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (val: string) => statusTag(val),
    },
    {
      title: '操作',
      key: 'actions',
      width: 140,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEditModal(record)}>
            编辑
          </Button>
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
    <>
      <Card
        title="短剧详情"
        loading={loading}
        extra={
          <Space>
            <Button onClick={() => navigate('/dramas')}>返回列表</Button>
            <Button onClick={() => navigate(`/dramas/${id}/edit`)}>编辑短剧</Button>
          </Space>
        }
        style={{ marginBottom: 24 }}
      >
        {drama && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="ID">{drama.id}</Descriptions.Item>
            <Descriptions.Item label="标题">{drama.title}</Descriptions.Item>
            <Descriptions.Item label="状态">{statusTag(drama.status)}</Descriptions.Item>
            <Descriptions.Item label="来源">{drama.source || '-'}</Descriptions.Item>
            <Descriptions.Item label="封面" span={2}>
              {drama.cover_url ? (
                <img
                  src={drama.cover_url}
                  alt="封面"
                  style={{ width: 80, height: 112, objectFit: 'cover' }}
                />
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="简介" span={2}>
              {drama.description || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="外部引用">
              {drama.external_ref || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {drama.created_at
                ? new Date(drama.created_at).toLocaleString('zh-CN')
                : '-'}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Card>

      <Card
        title="剧集管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
            添加剧集
          </Button>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={episodes}
          pagination={false}
          scroll={{ x: 800 }}
        />
      </Card>

      <Modal
        title={editingEpisode ? '编辑剧集' : '添加剧集'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="drama_id" hidden>
            <Input />
          </Form.Item>
          <Form.Item
            label="集号"
            name="ep_no"
            rules={[{ required: true, message: '请输入集号' }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            label="标题"
            name="title"
            rules={[{ required: true, message: '请输入标题' }]}
          >
            <Input placeholder="剧集标题" />
          </Form.Item>
          <Form.Item label="时长(秒)" name="duration_sec">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="媒体地址" name="media_url">
            <Input placeholder="视频媒体 URL" />
          </Form.Item>
          <Form.Item label="状态" name="status">
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
