import { useEffect, useState } from 'react';
import { Card, Col, Empty, Input, Pagination, Radio, Row, Spin, Tag, Typography, Image } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { Link, useSearchParams } from 'react-router-dom';
import { fetchLibrary } from '@/api/library';
import type { LibraryItem } from '@/types/api';

const { Title, Paragraph } = Typography;

export default function LibraryPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const kindParam = searchParams.get('kind') || 'all';
  const kind = kindParam === 'short' || kindParam === 'long' ? kindParam : 'all';
  const [items, setItems] = useState<LibraryItem[]>([]);
  const [recommend, setRecommend] = useState<LibraryItem[]>([]);
  const [basis, setBasis] = useState('');
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [capped, setCapped] = useState(false);
  const pageSize = 12;

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetchLibrary({
      page,
      page_size: pageSize,
      q: keyword || undefined,
      kind,
    })
      .then((result) => {
        if (cancelled) return;
        setItems(result.list || []);
        setRecommend(result.recommend || []);
        setBasis(result.recommend_basis || '');
        setTotal(result.total || 0);
        setCapped(Boolean(result.capped));
      })
      .catch(() => {
        if (cancelled) return;
        setItems([]);
        setRecommend([]);
        setTotal(0);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [page, keyword, kind]);

  const setKind = (next: string) => {
    const params = new URLSearchParams(searchParams);
    if (next === 'all') params.delete('kind');
    else params.set('kind', next);
    setSearchParams(params);
    setPage(1);
  };

  return (
    <div>
      <Title level={3}>片库</Title>
      <Paragraph type="secondary">
        已摄入且 READY 的 short / long 成片。推荐是最近入库交错，不是排序模型。同一幂等键不会出现第二条目录。
      </Paragraph>
      <Radio.Group
        value={kind}
        onChange={(event) => setKind(event.target.value)}
        style={{ marginBottom: 16 }}
      >
        <Radio.Button value="all">全部</Radio.Button>
        <Radio.Button value="short">Short</Radio.Button>
        <Radio.Button value="long">Long</Radio.Button>
      </Radio.Group>
      <Input.Search
        placeholder="搜索已入库成片"
        allowClear
        enterButton={<SearchOutlined />}
        style={{ maxWidth: 400, marginBottom: 24, display: 'block' }}
        onSearch={(value) => {
          setKeyword(value);
          setPage(1);
        }}
      />
      {basis === 'recent_ready_ingest' && recommend.length > 0 && (
        <Paragraph>
          最近入库：
          {recommend.map((item) => (
            <Link key={`${item.catalog}-${item.media_id}`} to={item.play_path || '/library'} style={{ marginRight: 12 }}>
              {item.title || item.media_id}
            </Link>
          ))}
        </Paragraph>
      )}
      <Spin spinning={loading}>
        {items.length === 0 && !loading ? (
          <Empty description="片库还没有 READY 成片。short / long 可通过摄入契约写入；MEDIA 主机未部署时只能打本机。" />
        ) : (
          <Row gutter={[16, 16]}>
            {items.map((item) => (
              <Col key={`${item.catalog}-${item.media_id}`} xs={12} sm={8} md={6} lg={6}>
                <Link to={item.play_path || (item.catalog === 'long' ? `/longs/${item.media_id}` : `/shorts/${item.media_id}`)}>
                  <Card
                    hoverable
                    cover={
                      item.cover_url ? (
                        <Image
                          alt={item.title || item.media_id}
                          src={item.cover_url}
                          preview={false}
                          style={{ height: 180, objectFit: 'cover' }}
                        />
                      ) : (
                        <div
                          style={{
                            height: 180,
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
                          <Tag>{item.catalog === 'long' ? 'Long' : 'Short'}</Tag>
                          {item.format_hook?.web_decision === 'REJECT' && <Tag>浏览器不可播</Tag>}
                          <Paragraph ellipsis={{ rows: 2 }} type="secondary" style={{ marginBottom: 0 }}>
                            {item.source_system || item.catalog}
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
        {capped && (
          <Paragraph type="secondary" style={{ marginTop: 16 }}>
            目录超过单次读取上限，更早的成片可能没有出现在本页。
          </Paragraph>
        )}
      </Spin>
    </div>
  );
}
