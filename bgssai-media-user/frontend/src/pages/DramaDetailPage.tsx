import { useEffect, useState } from 'react';
import {
  Breadcrumb,
  Button,
  Card,
  Descriptions,
  List,
  Spin,
  Typography,
  Image,
  message,
} from 'antd';
import { Link, useParams } from 'react-router-dom';
import { fetchDramaDetail } from '@/api/drama';
import type { MediaDrama, MediaEpisode } from '@/types/api';

const { Title, Paragraph } = Typography;

export default function DramaDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [drama, setDrama] = useState<MediaDrama | null>(null);
  const [episodes, setEpisodes] = useState<MediaEpisode[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    fetchDramaDetail(Number(id))
      .then((result) => {
        setDrama(result.drama);
        setEpisodes(result.episodes || []);
      })
      .catch((err) => {
        message.error(err instanceof Error ? err.message : '加载失败');
      })
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return <Spin size="large" style={{ display: 'block', margin: '48px auto' }} />;
  }

  if (!drama) {
    return <Typography.Text type="danger">短剧不存在</Typography.Text>;
  }

  return (
    <div>
      <Breadcrumb
        style={{ marginBottom: 16 }}
        items={[
          { title: <Link to="/">首页</Link> },
          { title: drama.title },
        ]}
      />
      <Card>
        <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
          {drama.cover_url && (
            <Image
              src={drama.cover_url}
              alt={drama.title}
              width={200}
              style={{ borderRadius: 8 }}
            />
          )}
          <div style={{ flex: 1, minWidth: 240 }}>
            <Title level={3}>{drama.title}</Title>
            <Paragraph type="secondary">{drama.description || '暂无简介'}</Paragraph>
            <Button type="link">
              <Link to="/continue">继续观看（查看历史进度）</Link>
            </Button>
          </div>
        </div>
      </Card>

      <Title level={4} style={{ marginTop: 24 }}>剧集列表</Title>
      <List
        bordered
        dataSource={episodes}
        renderItem={(ep) => (
          <List.Item
            actions={[
              <Link key="play" to={`/play/episode/${ep.id}`}>
                播放
              </Link>,
            ]}
          >
            <List.Item.Meta
              title={`第 ${ep.ep_no} 集 · ${ep.title}`}
              description={
                <Descriptions size="small" column={2}>
                  <Descriptions.Item label="时长">
                    {ep.duration_sec ? `${ep.duration_sec} 秒` : '未知'}
                  </Descriptions.Item>
                  <Descriptions.Item label="状态">{ep.status}</Descriptions.Item>
                </Descriptions>
              }
            />
          </List.Item>
        )}
      />
    </div>
  );
}
