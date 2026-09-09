import { useEffect, useState } from 'react';
import { Card, Col, Empty, Input, Pagination, Row, Spin, Typography, Image } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import { fetchFeed } from '@/api/drama';
import type { MediaDrama } from '@/types/api';

const { Title, Paragraph } = Typography;

export default function HomePage() {
  const [dramas, setDramas] = useState<MediaDrama[]>([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [pageNum, setPageNum] = useState(1);
  const [total, setTotal] = useState(0);
  const pageSize = 12;

  const loadFeed = async (page: number, kw?: string) => {
    setLoading(true);
    try {
      const result = await fetchFeed({
        page_num: page,
        page_size: pageSize,
        keyword: kw || undefined,
      });
      setDramas(result.list || []);
      setTotal(result.total || 0);
    } catch {
      setDramas([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFeed(pageNum, keyword);
  }, [pageNum, keyword]);

  const handleSearch = (value: string) => {
    setKeyword(value);
    setPageNum(1);
  };

  return (
    <div>
      <Title level={3}>短剧推荐</Title>
      <Input.Search
        placeholder="搜索短剧"
        allowClear
        enterButton={<SearchOutlined />}
        style={{ maxWidth: 400, marginBottom: 24 }}
        onSearch={handleSearch}
      />
      <Spin spinning={loading}>
        {dramas.length === 0 && !loading ? (
          <Empty description="暂无短剧" />
        ) : (
          <Row gutter={[16, 16]}>
            {dramas.map((drama) => (
              <Col key={drama.id} xs={12} sm={8} md={6} lg={6}>
                <Link to={`/drama/${drama.id}`}>
                  <Card
                    hoverable
                    cover={
                      drama.cover_url ? (
                        <Image
                          alt={drama.title}
                          src={drama.cover_url}
                          preview={false}
                          style={{ height: 200, objectFit: 'cover' }}
                        />
                      ) : (
                        <div
                          style={{
                            height: 200,
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
                      title={drama.title}
                      description={
                        <Paragraph ellipsis={{ rows: 2 }} type="secondary" style={{ marginBottom: 0 }}>
                          {drama.description || '暂无简介'}
                        </Paragraph>
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
            current={pageNum}
            pageSize={pageSize}
            total={total}
            onChange={(page) => setPageNum(page)}
            showSizeChanger={false}
          />
        )}
      </Spin>
    </div>
  );
}
