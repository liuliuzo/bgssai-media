import { useEffect, useState } from 'react';
import { Breadcrumb, Descriptions, Spin, Tag, Typography, message } from 'antd';
import { Link, useParams } from 'react-router-dom';
import UniversalPlayer from '@/components/UniversalPlayer';
import { fetchLongDetail } from '@/api/longs';
import type { ShortPlayItem } from '@/types/api';

const { Title } = Typography;

export default function LongPlayerPage() {
  const { mediaId } = useParams<{ mediaId: string }>();
  const [item, setItem] = useState<ShortPlayItem | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!mediaId) return;
    setLoading(true);
    fetchLongDetail(mediaId)
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
    return <Typography.Text type="danger">长剧不存在或尚未 READY</Typography.Text>;
  }

  const playUrl = item.playable ? item.play_url || item.video_url || '' : '';

  return (
    <div>
      <Breadcrumb
        style={{ marginBottom: 16 }}
        items={[
          { title: <Link to="/library">片库</Link> },
          { title: <Link to="/library?kind=long">Long</Link> },
          { title: item.title || item.media_id },
        ]}
      />
      <Title level={4}>{item.title || item.media_id}</Title>
      <Descriptions size="small" column={2} style={{ marginBottom: 16 }}>
        <Descriptions.Item label="状态">
          <Tag color={item.playable ? 'green' : 'default'}>{item.status}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="来源">{item.source_system || 'bgssai-long'}</Descriptions.Item>
        <Descriptions.Item label="media_id">{item.media_id}</Descriptions.Item>
        <Descriptions.Item label="时长">
          {item.duration_sec ? `${item.duration_sec} 秒` : '未知'}
        </Descriptions.Item>
      </Descriptions>
      {playUrl ? (
        <UniversalPlayer mode="file" src={playUrl} title={item.title} autoPlay />
      ) : (
        <Typography.Text type="warning">
          该条目不可播（非 READY 或缺少 play_url）。不会伪造可播地址。
        </Typography.Text>
      )}
    </div>
  );
}
