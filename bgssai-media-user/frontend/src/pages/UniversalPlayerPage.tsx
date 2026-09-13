import { useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  Space,
  Typography,
  Upload,
  List,
  message,
} from 'antd';
import { UploadOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import UniversalPlayer from '@/components/UniversalPlayer';
import type { PlaylistItem } from '@/types/api';
import { useShellMode } from '@/shell/useShellMode';

const { Title, Text } = Typography;

export default function UniversalPlayerPage() {
  const { inShell } = useShellMode();
  const [urlInput, setUrlInput] = useState('');
  const [titleInput, setTitleInput] = useState('');
  const [playlist, setPlaylist] = useState<PlaylistItem[]>([]);
  const [activeSrc, setActiveSrc] = useState<string | undefined>();
  const [activeTitle, setActiveTitle] = useState<string | undefined>();

  const addUrlToPlaylist = () => {
    if (!urlInput.trim()) {
      message.warning('请输入视频地址');
      return;
    }
    const item: PlaylistItem = {
      title: titleInput.trim() || urlInput.trim(),
      url: urlInput.trim(),
    };
    setPlaylist((prev) => [...prev, item]);
    if (!activeSrc) {
      setActiveSrc(item.url);
      setActiveTitle(item.title);
    }
    setUrlInput('');
    setTitleInput('');
  };

  const handleLocalFile = (file: File) => {
    const blobUrl = URL.createObjectURL(file);
    const item: PlaylistItem = {
      title: file.name,
      url: blobUrl,
    };
    setPlaylist((prev) => [...prev, item]);
    if (!activeSrc) {
      setActiveSrc(item.url);
      setActiveTitle(item.title);
    }
    message.success(`已添加本地文件：${file.name}`);
    return false;
  };

  const removeItem = (index: number) => {
    setPlaylist((prev) => prev.filter((_, i) => i !== index));
  };

  const playItem = (item: PlaylistItem) => {
    setActiveSrc(item.url);
    setActiveTitle(item.title);
  };

  return (
    <div>
      <Title level={3}>通用播放器</Title>
      <Text type="secondary">支持打开网络地址或本地文件，可管理播放列表队列。</Text>

      <Card style={{ marginTop: 16 }} title="添加视频">
        <Form layout="vertical">
          <Form.Item label="视频标题（可选）">
            <Input
              value={titleInput}
              onChange={(e) => setTitleInput(e.target.value)}
              placeholder="自定义标题"
            />
          </Form.Item>
          <Form.Item label="视频地址">
            {inShell ? (
              <Space direction="vertical" style={{ width: '100%' }} size={8}>
                <Input
                  value={urlInput}
                  onChange={(e) => setUrlInput(e.target.value)}
                  placeholder="https://example.com/video.mp4 或 .m3u8"
                />
                <Button type="primary" block icon={<PlusOutlined />} onClick={addUrlToPlaylist}>
                  添加到列表
                </Button>
              </Space>
            ) : (
              <Space.Compact style={{ width: '100%' }}>
                <Input
                  value={urlInput}
                  onChange={(e) => setUrlInput(e.target.value)}
                  placeholder="https://example.com/video.mp4 或 .m3u8"
                />
                <Button type="primary" icon={<PlusOutlined />} onClick={addUrlToPlaylist}>
                  添加到列表
                </Button>
              </Space.Compact>
            )}
          </Form.Item>
          <Form.Item label="本地文件">
            <Upload beforeUpload={handleLocalFile} showUploadList={false} accept="video/*">
              <Button icon={<UploadOutlined />}>选择本地视频</Button>
            </Upload>
          </Form.Item>
        </Form>
      </Card>

      {playlist.length > 0 && (
        <Card style={{ marginTop: 16 }} title="队列管理" size="small">
          <List
            size="small"
            dataSource={playlist}
            renderItem={(item, index) => (
              <List.Item
                actions={[
                  <Button type="link" size="small" onClick={() => playItem(item)}>
                    播放
                  </Button>,
                  <Button
                    type="link"
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    onClick={() => removeItem(index)}
                  />,
                ]}
              >
                {item.title}
              </List.Item>
            )}
          />
        </Card>
      )}

      <div style={{ marginTop: 24 }}>
        {activeSrc ? (
          <UniversalPlayer
            mode="file"
            src={activeSrc}
            title={activeTitle}
            playlist={playlist.length > 1 ? playlist : undefined}
          />
        ) : (
          <Card>
            <Text type="secondary">请添加视频地址或选择本地文件开始播放</Text>
          </Card>
        )}
      </div>
    </div>
  );
}
