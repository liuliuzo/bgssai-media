import { BrowserRouter } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import AppRouter from './router';
import LegalFooter from './legal/LegalFooter';

export default function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <BrowserRouter>
        {/* 页脚挂在 App 级，覆盖含 /login 在内的所有路由（Standards 第 15.2 节） */}
        <div className="app-root">
          <div className="app-root__main">
            <AppRouter />
          </div>
          <LegalFooter />
        </div>
      </BrowserRouter>
    </ConfigProvider>
  );
}
