import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Alert, Button, Card, Space, Table, Typography } from 'antd'
import { ArrowLeftOutlined, DownloadOutlined } from '@ant-design/icons'
import client from '../../api/client'
import { useAuthStore } from '../../stores/authStore'

const { Title, Paragraph, Text } = Typography

function formatSize(bytes: number | null | undefined) {
  if (bytes == null) return '-'
  const mb = bytes / 1024 / 1024
  return mb >= 1 ? `${mb.toFixed(1)} MB` : `${(bytes / 1024).toFixed(0)} KB`
}

type BotItem = {
  file_name: string
  platform?: string
  size_bytes?: number
  updated_at?: string
  url?: string
}

export default function BotDownloadPage() {
  const navigate = useNavigate()
  const token = useAuthStore((s) => s.token)
  const [items, setItems] = useState<BotItem[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let alive = true
    client
      .get('/download/bot')
      .then((res) => {
        const list = res.data || []
        if (alive) setItems(Array.isArray(list) ? list : [])
      })
      .catch(() => {
        if (alive) setItems([])
      })
      .finally(() => {
        if (alive) setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [])

  const columns = [
    {
      title: '安装包',
      dataIndex: 'file_name',
      render: (name: string) => <Text strong>{name}</Text>,
    },
    { title: '平台', dataIndex: 'platform', width: 120, render: (value: string) => value || '-' },
    { title: '大小', dataIndex: 'size_bytes', width: 120, render: formatSize },
    {
      title: '更新时间',
      dataIndex: 'updated_at',
      width: 200,
      render: (value: string) => (value ? new Date(value).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '操作',
      dataIndex: 'url',
      width: 140,
      render: (url: string) => (
        <Button type="primary" icon={<DownloadOutlined />} href={url} download>
          下载
        </Button>
      ),
    },
  ]

  return (
    <div style={{ maxWidth: 880, margin: '0 auto', padding: '32px 20px 60px' }}>
      <Space style={{ marginBottom: 20 }}>
        <Button
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate(token ? '/' : '/login')}
        >
          {token ? '返回首页' : '前往登录页'}
        </Button>
      </Space>

      <Title level={3} style={{ marginTop: 0 }}>下载 BGSSAI BOT</Title>
      <Paragraph type="secondary">
        BGSSAI BOT 对标 Grok Bot。装好之后可以把本应用托管给 Bot 直接操作。
        本页不要求登录。安装包由本站提供，不上官网。
      </Paragraph>

      {items.length === 0 && !loading ? (
        <Alert
          type="info"
          showIcon
          message="本站尚未发布安装包"
          description="目录为空时不会列出假文件。发布时把 BGSSAI-Bot-Setup.exe 放到本应用的 Bot 下载目录即可。"
          style={{ marginBottom: 24 }}
        />
      ) : (
        <Card>
          <Table
            rowKey="file_name"
            loading={loading}
            columns={columns}
            dataSource={items}
            pagination={false}
          />
        </Card>
      )}

      <Paragraph style={{ marginTop: 24 }}>
        <Link to="/login">登录本应用</Link>
      </Paragraph>
    </div>
  )
}
