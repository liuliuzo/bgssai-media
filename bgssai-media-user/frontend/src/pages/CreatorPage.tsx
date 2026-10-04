import { useCallback, useEffect, useState } from 'react';
import { Alert, Button, Card, Form, Input, List, Space, Tag, Typography, Upload } from 'antd';
import type { UploadFile } from 'antd/es/upload/interface';
import {
  addVideoComment,
  fetchCreatorJob,
  fetchCreatorPlayback,
  fetchMyChannel,
  fetchMyVideos,
  fetchVideoComments,
  retryCreatorJob,
  saveMyChannel,
  uploadCreatorVideo,
  type CreatorChannel,
  type CreatorComment,
  type CreatorPlayback,
  type CreatorVideo,
} from '@/api/creator';

const { Title, Paragraph, Text } = Typography;

const ACCEPT = '.mp4,.webm,.mkv,.mov,.flv';

function statusColor(status: string) {
  if (status === 'READY') return 'green';
  if (status === 'FAILED') return 'red';
  if (status === 'RUNNING') return 'blue';
  return 'default';
}

export default function CreatorPage() {
  const [channel, setChannel] = useState<CreatorChannel | null>(null);
  const [videos, setVideos] = useState<CreatorVideo[]>([]);
  const [selected, setSelected] = useState<CreatorVideo | null>(null);
  const [playback, setPlayback] = useState<CreatorPlayback | null>(null);
  const [comments, setComments] = useState<CreatorComment[]>([]);
  const [commentText, setCommentText] = useState('');
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setError('');
    try {
      const [mine, list] = await Promise.all([fetchMyChannel(), fetchMyVideos()]);
      setChannel(mine);
      setVideos(list || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载创作者数据失败');
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function onSaveChannel(values: { display_name?: string; handle?: string; bio?: string }) {
    setBusy(true);
    setError('');
    try {
      const saved = await saveMyChannel(values);
      setChannel(saved);
    } catch (err) {
      setError(err instanceof Error ? err.message : '保存频道失败');
    } finally {
      setBusy(false);
    }
  }

  async function onUpload() {
    const file = fileList[0]?.originFileObj;
    if (!file) {
      setError('请选择 mp4、webm、mkv、mov 或 flv 文件');
      return;
    }
    setBusy(true);
    setError('');
    setPlayback(null);
    try {
      const uploaded = await uploadCreatorVideo({
        file,
        title: title.trim() || undefined,
        description: description.trim() || undefined,
      });
      setSelected(uploaded);
      setTitle('');
      setDescription('');
      setFileList([]);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : '上传失败');
    } finally {
      setBusy(false);
    }
  }

  async function openVideo(video: CreatorVideo) {
    setSelected(video);
    setPlayback(null);
    setComments([]);
    setError('');
    if (video.status !== 'READY') return;
    try {
      const [play, rows] = await Promise.all([
        fetchCreatorPlayback(video.id),
        fetchVideoComments(video.id),
      ]);
      setPlayback(play);
      setComments(rows || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : '读取播放地址失败');
    }
  }

  async function refreshJob() {
    const jobId = selected?.job?.id;
    if (!jobId) return;
    setBusy(true);
    setError('');
    try {
      const job = await fetchCreatorJob(jobId);
      setSelected((prev) => (prev ? { ...prev, status: job.status, job } : prev));
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : '刷新任务失败');
    } finally {
      setBusy(false);
    }
  }

  async function onRetry() {
    const jobId = selected?.job?.id;
    if (!jobId) return;
    setBusy(true);
    setError('');
    try {
      const job = await retryCreatorJob(jobId);
      setSelected((prev) => (prev ? { ...prev, status: job.status, job } : prev));
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : '重试失败');
    } finally {
      setBusy(false);
    }
  }

  async function onComment() {
    if (!selected || selected.status !== 'READY' || !commentText.trim()) return;
    setBusy(true);
    setError('');
    try {
      await addVideoComment(selected.id, commentText.trim());
      setCommentText('');
      setComments(await fetchVideoComments(selected.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : '评论失败');
    } finally {
      setBusy(false);
    }
  }

  const playable = (playback?.renditions || []).filter((row) => row.play_url);

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <div>
        <Title level={3}>创作者</Title>
        <Paragraph type="secondary">
          上传、频道和评论都走已有接口。转码未完成或失败时不提供播放地址。
        </Paragraph>
      </div>
      {error ? <Alert type="error" message={error} showIcon /> : null}

      <Card title="我的频道">
        <Form
          layout="vertical"
          key={channel?.id || 'channel'}
          initialValues={{
            display_name: channel?.display_name || '',
            handle: channel?.handle || '',
            bio: channel?.bio || '',
          }}
          onFinish={onSaveChannel}
        >
          <Form.Item label="显示名" name="display_name">
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item label="handle" name="handle">
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item label="简介" name="bio">
            <Input.TextArea rows={3} maxLength={500} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={busy}>
            保存频道
          </Button>
        </Form>
      </Card>

      <Card title="上传视频">
        <Space direction="vertical" style={{ width: '100%' }}>
          <Input placeholder="标题" value={title} onChange={(e) => setTitle(e.target.value)} />
          <Input.TextArea
            placeholder="简介"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={2}
          />
          <Upload
            accept={ACCEPT}
            maxCount={1}
            fileList={fileList}
            beforeUpload={() => false}
            onChange={({ fileList: next }) => setFileList(next.slice(-1))}
          >
            <Button>选择 mp4 / webm / mkv / mov / flv</Button>
          </Upload>
          <Button type="primary" onClick={() => void onUpload()} loading={busy}>
            上传并转码
          </Button>
        </Space>
      </Card>

      <Card title="我的视频">
        <List
          dataSource={videos}
          locale={{ emptyText: '还没有上传' }}
          renderItem={(video) => (
            <List.Item
              actions={[
                <Button key="open" type="link" onClick={() => void openVideo(video)}>
                  查看
                </Button>,
              ]}
            >
              <List.Item.Meta
                title={video.title}
                description={
                  <Space>
                    <Tag color={statusColor(video.status)}>{video.status}</Tag>
                    {video.job?.failure_reason ? <Text type="danger">{video.job.failure_reason}</Text> : null}
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      </Card>

      {selected ? (
        <Card title={selected.title}>
          <Space direction="vertical" style={{ width: '100%' }}>
            <Space>
              <Tag color={statusColor(selected.status)}>{selected.status}</Tag>
              <Button onClick={() => void refreshJob()} disabled={!selected.job?.id || busy}>
                刷新任务
              </Button>
              {selected.status === 'FAILED' || selected.job?.status === 'FAILED' ? (
                <Button onClick={() => void onRetry()} loading={busy}>
                  重试转码
                </Button>
              ) : null}
            </Space>
            {selected.job?.failure_reason ? (
              <Alert type="warning" message={selected.job.failure_reason} showIcon />
            ) : null}
            {selected.status === 'READY' && playback?.cover_url ? (
              <img src={playback.cover_url} alt="" style={{ maxWidth: 320 }} />
            ) : null}
            {selected.status === 'READY' && playable.length > 0 ? (
              <video
                key={playable[0].play_url}
                controls
                src={playable[0].play_url}
                style={{ width: '100%', maxHeight: 420, background: '#000' }}
              />
            ) : (
              <Text type="secondary">当前没有可播放地址。</Text>
            )}
            {selected.status === 'READY' ? (
              <>
                <Input.TextArea
                  rows={2}
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  placeholder="评论"
                />
                <Button onClick={() => void onComment()} disabled={!commentText.trim()} loading={busy}>
                  发表评论
                </Button>
                <List
                  dataSource={comments}
                  locale={{ emptyText: '暂无评论' }}
                  renderItem={(row) => (
                    <List.Item>
                      <Text>{row.content}</Text>
                    </List.Item>
                  )}
                />
              </>
            ) : null}
          </Space>
        </Card>
      ) : null}
    </Space>
  );
}
