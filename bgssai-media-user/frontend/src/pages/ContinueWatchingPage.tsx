import { useEffect, useState } from 'react';
import { Card, Empty, List, Spin, Typography, message } from 'antd';
import { Link } from 'react-router-dom';
import { fetchContinueWatching } from '@/api/watch';
import type { MediaWatchProgress } from '@/types/api';

const { Title, Text } = Typography;

function formatPosition(sec: number): string {
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}分${s}秒`;
}

export default function ContinueWatchingPage() {
  const [items, setItems] = useState<MediaWatchProgress[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchContinueWatching()
      .then(setItems)
      .catch((err) => {
        message.error(err instanceof Error ? err.message : '加载失败');
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <Title level={3}>继续观看</Title>
      <Spin spinning={loading}>
        {items.length === 0 && !loading ? (
          <Empty description="暂无观看记录" />
        ) : (
          <List
            grid={{ gutter: 16, xs: 1, sm: 1, md: 2, lg: 2 }}
            dataSource={items}
            renderItem={(item) => (
              <List.Item>
                <Card
                  title={`短剧 #${item.drama_id}`}
                  extra={
                    <Link to={`/play/episode/${item.episode_id}`}>继续播放</Link>
                  }
                >
                  <Text>剧集 ID：{item.episode_id}</Text>
                  <br />
                  <Text type="secondary">
                    上次进度：{formatPosition(item.position_sec)}
                  </Text>
                  {item.updated_at && (
                    <>
                      <br />
                      <Text type="secondary">更新时间：{item.updated_at}</Text>
                    </>
                  )}
                </Card>
              </List.Item>
            )}
          />
        )}
      </Spin>
    </div>
  );
}
