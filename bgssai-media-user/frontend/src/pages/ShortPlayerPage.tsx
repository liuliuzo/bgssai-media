import { useEffect, useState } from 'react';
import { Breadcrumb, Descriptions, Spin, Tag, Typography, message } from 'antd';
import { Link, useParams } from 'react-router-dom';
import UniversalPlayer from '@/components/UniversalPlayer';
import { fetchShortDetail } from '@/api/shorts';
import type { ShortPlayItem } from '@/types/api';

const { Title } = Typography;

export default function ShortPlayerPage() {
  const { mediaId } = useParams<{ mediaId: string }>();
  const [item, setItem] = useState<ShortPlayItem | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!mediaId) return;
    setLoading(true);
    fetchShortDetail(mediaId)
      .then(setItem)
      .catch((err) => {
        message.error(err instanceof Error ? err.message : '加载失败');
      })
      .finally(() => setLoading(false));
  }, [mediaId]);

  if (loading) {
    return <Spin size="large" style={{ display: 'block', margin: '48px auto' }} />;
  }

  if (!item) {
    return <Typography.Text type="danger">短剧不存在或尚未 READY</Typography.Text>;
  }

  const playUrl = item.playable ? item.play_url || item.video_url || '' : '';

  return (
    <div>
      <Breadcrumb
        style={{ marginBottom: 16 }}
        items={[
          { title: <Link to="/">首页</Link> },
          { title: <Link to="/shorts">Short 发布</Link> },
          { title: item.title || item.media_id },
        ]}
      />
      <Title level={4}>{item.title || item.media_id}</Title>
      <Descriptions size="small" column={2} style={{ marginBottom: 16 }}>
        <Descriptions.Item label="状态">
          <Tag color={item.playable ? 'green' : 'default'}>{item.status}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="来源">{item.source_system || 'bgssai-short'}</Descriptions.Item>
        <Descriptions.Item label="media_id">{item.media_id}</Descriptions.Item>
        <Descriptions.Item label="时长">
          {item.duration_sec ? `${item.duration_sec} 秒` : '未知'}
        </Descriptions.Item>
      </Descriptions>
      {playUrl ? (
        <UniversalPlayer mode="short" src={playUrl} title={item.title} autoPlay />
      ) : (
        <Typography.Text type="warning">
          该条目不可播（非 READY 或缺少 play_url）。不会伪造可播地址。
        </Typography.Text>
      )}
    </div>
  );
}
