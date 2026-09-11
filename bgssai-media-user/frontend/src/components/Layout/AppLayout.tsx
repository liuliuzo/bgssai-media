import { Layout, Menu, Button, Typography, Space } from 'antd';
import {
  HomeOutlined,
  PlayCircleOutlined,
  HistoryOutlined,
  LogoutOutlined,
  ProfileOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';

const { Header, Content } = Layout;
const { Text } = Typography;

export default function AppLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { username, logout } = useAuthStore();

  const selectedKey = (() => {
    if (location.pathname.startsWith('/shorts')) return '/shorts';
    if (location.pathname.startsWith('/continue')) return '/continue';
    if (location.pathname.startsWith('/player')) return '/player';
    if (location.pathname.startsWith('/formats')) return '/formats';
    if (location.pathname.startsWith('/settings')) return '/settings';
    return '/';
  })();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 24px',
        }}
      >
        <Space size="large">
          <Text style={{ color: '#fff', fontSize: 18, fontWeight: 600 }}>
            BGSSAI 媒体
          </Text>
          <Menu
            theme="dark"
            mode="horizontal"
            selectedKeys={[selectedKey]}
            items={[
              {
                key: '/',
                icon: <HomeOutlined />,
                label: <Link to="/">首页</Link>,
              },
              {
                key: '/shorts',
                icon: <PlayCircleOutlined />,
                label: <Link to="/shorts">Short 发布</Link>,
              },
              {
                key: '/continue',
                icon: <HistoryOutlined />,
                label: <Link to="/continue">继续观看</Link>,
              },
              {
                key: '/player',
                icon: <PlayCircleOutlined />,
                label: <Link to="/player">通用播放器</Link>,
              },
              {
                key: '/formats',
                icon: <ProfileOutlined />,
                label: <Link to="/formats">格式支持</Link>,
              },
              {
                key: '/settings',
                icon: <SettingOutlined />,
                label: <Link to="/settings">设置</Link>,
              },
            ]}
          />
        </Space>
        <Space>
          <Text style={{ color: '#fff' }}>{username}</Text>
          <Button
            type="text"
            icon={<LogoutOutlined />}
            style={{ color: '#fff' }}
            onClick={handleLogout}
          >
            退出
          </Button>
        </Space>
      </Header>
      <Content style={{ padding: '24px', maxWidth: 1200, margin: '0 auto', width: '100%' }}>
        <Outlet />
      </Content>
    </Layout>
  );
}
