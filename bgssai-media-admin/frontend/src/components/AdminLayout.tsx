import { Layout, Menu, Button, theme } from 'antd';
import {
  VideoCameraOutlined,
  FileTextOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

const { Header, Sider, Content } = Layout;

export default function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = theme.useToken();
  const logout = useAuthStore((s) => s.logout);
  const username = useAuthStore((s) => s.username);

  const menuItems = [
    {
      key: '/dramas',
      icon: <VideoCameraOutlined />,
      label: '短剧管理',
    },
    {
      key: '/ingest-logs',
      icon: <FileTextOutlined />,
      label: '入库日志',
    },
  ];

  const selectedKey =
    menuItems.find((item) => location.pathname.startsWith(item.key))?.key ||
    '/dramas';

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        width={220}
        style={{
          background: token.colorBgContainer,
          borderRight: `1px solid ${token.colorBorderSecondary}`,
        }}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 600,
            fontSize: 16,
            borderBottom: `1px solid ${token.colorBorderSecondary}`,
          }}
        >
          BGSSAI 媒体后台
        </div>
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ borderInlineEnd: 'none' }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: token.colorBgContainer,
            padding: '0 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            borderBottom: `1px solid ${token.colorBorderSecondary}`,
          }}
        >
          <span style={{ marginRight: 16, color: token.colorTextSecondary }}>
            {username || '管理员'}
          </span>
          <Button type="text" icon={<LogoutOutlined />} onClick={handleLogout}>
            退出登录
          </Button>
        </Header>
        <Content style={{ margin: 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
