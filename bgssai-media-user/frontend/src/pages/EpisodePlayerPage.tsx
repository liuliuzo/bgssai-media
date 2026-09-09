import { useEffect, useState } from 'react';
import { Breadcrumb, Spin, Typography, message } from 'antd';
import { Link, useParams } from 'react-router-dom';
import UniversalPlayer from '@/components/UniversalPlayer';
import { fetchEpisode } from '@/api/drama';
import type { MediaEpisode } from '@/types/api';

const { Title } = Typography;

export default function EpisodePlayerPage() {
  const { id } = useParams<{ id: string }>();
  const [episode, setEpisode] = useState<MediaEpisode | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    fetchEpisode(Number(id))
      .then(setEpisode)
      .catch((err) => {
        message.error(err instanceof Error ? err.message : '加载失败');
      })
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return <Spin size="large" style={{ display: 'block', margin: '48px auto' }} />;
  }

  if (!episode) {
    return <Typography.Text type="danger">剧集不存在</Typography.Text>;
  }

  const mediaUrl = episode.media_url || '';

  return (
    <div>
      <Breadcrumb
        style={{ marginBottom: 16 }}
        items={[
          { title: <Link to="/">首页</Link> },
          { title: <Link to={`/drama/${episode.drama_id}`}>短剧详情</Link> },
          { title: episode.title },
        ]}
      />
      <Title level={4}>
        第 {episode.ep_no} 集 · {episode.title}
      </Title>
      {mediaUrl ? (
        <UniversalPlayer
          mode="short"
          src={mediaUrl}
          title={episode.title}
          autoPlay
        />
      ) : (
        <Typography.Text type="warning">暂无播放地址</Typography.Text>
      )}
    </div>
  );
}
