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
import { useShellMode } from '@/shell/useShellMode';
import LegalLinks from '@/legal/LegalLinks';

const { Header, Content } = Layout;
const { Text } = Typography;

const NAV_ITEMS = [
  { key: '/', icon: <HomeOutlined />, label: '首页', to: '/' },
  { key: '/shorts', icon: <PlayCircleOutlined />, label: 'Short 发布', to: '/shorts' },
  { key: '/continue', icon: <HistoryOutlined />, label: '继续观看', to: '/continue' },
  { key: '/player', icon: <PlayCircleOutlined />, label: '通用播放器', to: '/player' },
  { key: '/formats', icon: <ProfileOutlined />, label: '格式支持', to: '/formats' },
  { key: '/settings', icon: <SettingOutlined />, label: '设置', to: '/settings' },
];

const SHELL_TABS = [
  { key: '/', icon: <HomeOutlined />, label: '首页', to: '/' },
  { key: '/shorts', icon: <PlayCircleOutlined />, label: 'Short', to: '/shorts' },
  { key: '/continue', icon: <HistoryOutlined />, label: '继续', to: '/continue' },
  { key: '/player', icon: <PlayCircleOutlined />, label: '播放', to: '/player' },
  { key: '/settings', icon: <SettingOutlined />, label: '设置', to: '/settings' },
];

function useSelectedKey(pathname: string, inShell: boolean): string {
  if (pathname.startsWith('/shorts')) return '/shorts';
  if (pathname.startsWith('/continue')) return '/continue';
  if (pathname.startsWith('/player')) return '/player';
  if (inShell && pathname.startsWith('/play/')) return '/player';
  if (pathname.startsWith('/formats')) return '/formats';
  if (pathname.startsWith('/settings')) return '/settings';
  return '/';
}

export default function AppLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { username, logout } = useAuthStore();
  const { inShell } = useShellMode();
  const selectedKey = useSelectedKey(location.pathname, inShell);

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  if (inShell) {
    return (
      <div className="shell-app">
        <header className="shell-topbar">
          <span className="shell-brand">BGSSAI 媒体</span>
          <span className="shell-user">{username}</span>
          <Button type="text" icon={<LogoutOutlined />} onClick={handleLogout}>
            退出
          </Button>
        </header>
        <main className="shell-content">
          <Outlet />
          <LegalLinks variant="footer" />
        </main>
        <nav className="shell-tabbar" aria-label="壳内主导航">
          {SHELL_TABS.map((item) => (
            <Link
              key={item.key}
              to={item.to}
              className={selectedKey === item.key ? 'shell-tab shell-tab--active' : 'shell-tab'}
            >
              {item.icon}
              <span>{item.label}</span>
            </Link>
          ))}
        </nav>
      </div>
    );
  }

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
            items={NAV_ITEMS.map((item) => ({
              key: item.key,
              icon: item.icon,
              label: <Link to={item.to}>{item.label}</Link>,
            }))}
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
      <LegalLinks variant="footer" />
    </Layout>
  );
}
