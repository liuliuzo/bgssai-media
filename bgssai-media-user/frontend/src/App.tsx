import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import AppRouter from '@/router';
import { useShellMode } from '@/shell/useShellMode';

export default function App() {
  const { inShell } = useShellMode();
  return (
    <ConfigProvider locale={zhCN} componentSize={inShell ? 'large' : 'middle'}>
      <AppRouter />
    </ConfigProvider>
  );
}
