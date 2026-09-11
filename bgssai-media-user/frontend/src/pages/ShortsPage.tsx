import { useEffect, useState } from 'react';
import { Card, Col, Empty, Input, Pagination, Row, Spin, Tag, Typography, Image } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import { fetchShorts } from '@/api/shorts';
import type { ShortPlayItem } from '@/types/api';

const { Title, Paragraph } = Typography;

export default function ShortsPage() {
  const [items, setItems] = useState<ShortPlayItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const pageSize = 12;

  const load = async (nextPage: number, q?: string) => {
    setLoading(true);
    try {
      const result = await fetchShorts({
        page: nextPage,
        page_size: pageSize,
        q: q || undefined,
      });
      setItems(result.list || []);
      setTotal(result.total || 0);
    } catch {
      setItems([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(page, keyword);
  }, [page, keyword]);

  return (
    <div>
      <Title level={3}>Short 发布短剧</Title>
      <Paragraph type="secondary">
        来自 bgssai-short 的成品摄入。仅 READY 且带播放地址的条目可播。
      </Paragraph>
      <Input.Search
        placeholder="搜索已发布短剧"
        allowClear
        enterButton={<SearchOutlined />}
        style={{ maxWidth: 400, marginBottom: 24 }}
        onSearch={(value) => {
          setKeyword(value);
          setPage(1);
        }}
      />
      <Spin spinning={loading}>
        {items.length === 0 && !loading ? (
          <Empty description="暂无已摄入短剧。请用 short 发布契约或 scripts/publish-short-smoke.sh 写入。" />
        ) : (
          <Row gutter={[16, 16]}>
            {items.map((item) => (
              <Col key={item.media_id} xs={12} sm={8} md={6} lg={6}>
                <Link to={`/shorts/${encodeURIComponent(item.media_id)}`}>
                  <Card
                    hoverable
                    cover={
                      item.cover_url ? (
                        <Image
                          alt={item.title || item.media_id}
                          src={item.cover_url}
                          preview={false}
                          style={{ height: 220, objectFit: 'cover' }}
                        />
                      ) : (
                        <div
                          style={{
                            height: 220,
                            background: '#f5f5f5',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: '#999',
                          }}
                        >
                          暂无封面
                        </div>
                      )
                    }
                  >
                    <Card.Meta
                      title={item.title || item.media_id}
                      description={
                        <>
                          <Tag color={item.playable ? 'green' : 'default'}>
                            {item.status || 'UNKNOWN'}
                          </Tag>
                          <Paragraph ellipsis={{ rows: 2 }} type="secondary" style={{ marginBottom: 0 }}>
                            {item.source_system || 'bgssai-short'}
                          </Paragraph>
                        </>
                      }
                    />
                  </Card>
                </Link>
              </Col>
            ))}
          </Row>
        )}
        {total > pageSize && (
          <Pagination
            style={{ marginTop: 24, textAlign: 'center' }}
            current={page}
            pageSize={pageSize}
            total={total}
            onChange={(next) => setPage(next)}
            showSizeChanger={false}
          />
        )}
      </Spin>
    </div>
  );
}
