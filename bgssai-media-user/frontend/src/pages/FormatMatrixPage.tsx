import { Alert, Table, Typography } from 'antd';
import data from '@/data/format-matrix.json';

const { Title, Paragraph } = Typography;

type Row = {
  surface: string;
  kind: string;
  id: string;
  label: string;
  extensions?: string[];
  declared: string;
  verification: string;
  web_decision?: string;
  evidence: string;
};

const rows = (data.verified_rows || []) as Row[];

export default function FormatMatrixPage() {
  const host = data.deployment?.media_user_host || '';
  return (
    <div className="format-matrix-page" style={{ padding: 24, maxWidth: 1100, margin: '0 auto' }}>
      <Title level={3}>格式支持矩阵</Title>
      <Alert
        type="warning"
        showIcon
        style={{ marginBottom: 16 }}
        message={`部署状态 ${data.deployment?.status || 'UNKNOWN'}`}
        description={
          data.deployment?.note ||
          'MEDIA 主机未分配。本表记录声明与验证状态，不把声明写成已通过。'
        }
      />
      <Paragraph type="secondary">
        MEDIA_USER_HOST={host || '(empty)'} · MEDIA_ADMIN_HOST=
        {data.deployment?.media_admin_host || '(empty)'}。验证列保持 NOT_VERIFIED，直到对应表面打开过样本并留下记录。
      </Paragraph>
      <Table<Row>
        rowKey="id"
        size="small"
        pagination={false}
        scroll={{ x: 960 }}
        dataSource={rows}
        columns={[
          { title: '表面', dataIndex: 'surface', width: 120 },
          { title: '类型', dataIndex: 'kind', width: 100 },
          { title: '名称', dataIndex: 'label', width: 180 },
          {
            title: '扩展名',
            width: 140,
            render: (_, row) => (row.extensions && row.extensions.length ? row.extensions.join(' ') : '—'),
          },
          { title: '声明', dataIndex: 'declared', width: 120 },
          { title: '验证', dataIndex: 'verification', width: 140 },
          { title: 'Web 钩子', dataIndex: 'web_decision', width: 110 },
          { title: '依据', dataIndex: 'evidence' },
        ]}
      />
    </div>
  );
}
