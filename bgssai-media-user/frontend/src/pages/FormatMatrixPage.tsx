import { Card, Descriptions, Typography, Alert } from 'antd';
import data from '@/data/format-matrix.json';

export default function FormatMatrixPage() {
  return (
    <div style={{ padding: 24, maxWidth: 960, margin: '0 auto' }}>
      <Typography.Title level={3}>格式支持矩阵</Typography.Title>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="Web 短剧端只覆盖在线 MP4/WebM/HLS；完整本地格式请使用桌面播放器 bgssai-media-desktop（libVLC）。"
      />
      <Card title={data.web.label} style={{ marginBottom: 16 }}>
        <Descriptions column={1} size="small">
          <Descriptions.Item label="视频容器">{data.web.video_containers.join(' ')}</Descriptions.Item>
          <Descriptions.Item label="说明">{data.web.notes}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title={data.desktop_libvlc.label}>
        <Descriptions column={1} size="small">
          <Descriptions.Item label="视频容器">{data.desktop_libvlc.video_containers.join(' ')}</Descriptions.Item>
          <Descriptions.Item label="音频容器">{data.desktop_libvlc.audio_containers.join(' ')}</Descriptions.Item>
          <Descriptions.Item label="视频编码">{data.desktop_libvlc.video_codecs.join(' / ')}</Descriptions.Item>
          <Descriptions.Item label="音频编码">{data.desktop_libvlc.audio_codecs.join(' / ')}</Descriptions.Item>
          <Descriptions.Item label="说明">{data.desktop_libvlc.notes}</Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  );
}
