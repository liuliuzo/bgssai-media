import { useEffect, useState } from 'react';
import { Button, Card, Form, Input, Select, Space, message } from 'antd';
import { useNavigate, useParams } from 'react-router-dom';
import {
  createDrama,
  fetchDramaDetail,
  updateDrama,
} from '../api/drama';
import type { MediaDrama } from '../types';

const STATUS_OPTIONS = [
  { label: '草稿', value: 'draft' },
  { label: '已发布', value: 'published' },
];

export default function DramaForm() {
  const { id } = useParams<{ id: string }>();
  const isEdit = !!id;
  const navigate = useNavigate();
  const [form] = Form.useForm<MediaDrama>();
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!isEdit) return;
    setLoading(true);
    fetchDramaDetail(Number(id))
      .then((res) => {
        form.setFieldsValue(res.drama);
      })
      .finally(() => setLoading(false));
  }, [id, isEdit, form]);

  const onFinish = async (values: MediaDrama) => {
    setSubmitting(true);
    try {
      if (isEdit) {
        await updateDrama({ ...values, id: Number(id) });
        message.success('更新成功');
        navigate(`/dramas/${id}`);
      } else {
        const result = await createDrama(values);
        message.success('创建成功');
        navigate(`/dramas/${result.id}`);
      }
    } catch {
      // handled
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card
      title={isEdit ? '编辑短剧' : '新建短剧'}
      loading={loading}
      extra={
        <Button onClick={() => navigate(isEdit ? `/dramas/${id}` : '/dramas')}>
          返回
        </Button>
      }
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        initialValues={{ status: 'draft', source: 'manual' }}
        style={{ maxWidth: 640 }}
      >
        <Form.Item
          label="标题"
          name="title"
          rules={[{ required: true, message: '请输入标题' }]}
        >
          <Input placeholder="短剧标题" />
        </Form.Item>
        <Form.Item label="封面地址" name="cover_url">
          <Input placeholder="封面图片 URL" />
        </Form.Item>
        <Form.Item label="简介" name="description">
          <Input.TextArea rows={4} placeholder="短剧简介" />
        </Form.Item>
        <Form.Item label="状态" name="status">
          <Select options={STATUS_OPTIONS} />
        </Form.Item>
        <Form.Item label="来源" name="source">
          <Input placeholder="来源标识" />
        </Form.Item>
        <Form.Item label="外部引用" name="external_ref">
          <Input placeholder="外部系统引用 ID" />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={submitting}>
              {isEdit ? '保存' : '创建'}
            </Button>
            <Button onClick={() => navigate('/dramas')}>取消</Button>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  );
}
