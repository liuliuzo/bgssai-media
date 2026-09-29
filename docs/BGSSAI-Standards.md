# BGSSAI 编码规范（媒体仓工作副本）

权威以 `bgssai-skeleton/docs/BGSSAI-Standards.md` 为准。本仓强制遵循摘要：

1. **配置**：环境差异下沉 `application-*.properties`；禁止 `${}` 占位符；中间件连接参数只写 properties。
2. **Controller**：单入口风格（按资源一个 Controller，方法扁平）；统一响应 `{ code, message, success, result }`。
3. **JSON**：snake_case。
4. **持久化**：MyBatis Example + PageHelper 物理分页；禁止 Lombok。
5. **鉴权**：JWT 请求头 `Jwttoken`；业务接口 `@NeedAop`；角色 `PLATFORM_ADMIN` / `USER`。
6. **登录**：user 支持密码 + 邮箱 OTP + 手机 OTP（可 stub）；admin 仅种子账号，不接 Chat。
7. **文档**：产品愿景/README/功能说明中文。
8. **禁止**：emoji、装饰性符号。
9. **业务第三方凭证**：Admin 维护 + 库表 + DML 种子（本 MVP 摄入令牌属服务间配置，写 properties 示例）。

## 15. 法务入口与同意提示统一口径（全产品线，强制）

2026-09-29 根据用户反馈修订：同一页面不得在登录卡、全局页脚和设置区重复列出同一组协议。取代 2026-09-23 同时要求多处链接的旧规则。

### 15.1 入口归属与同意提示

- Web 用户端和管理端由 App 级全局页脚统一提供一组「用户协议 · 隐私政策」。子页面不再重复挂载。不同路由共用同一页脚。
- 登录/注册卡只保留一段同意提示：`登录或注册即表示已阅读并同意页脚所列协议与隐私条款。`；英文为 `By signing in or signing up, you agree to the agreements and privacy terms listed in the footer.`。
- 已有注册同意勾选框必须保留原校验，文案为 `我已阅读并同意页脚所列协议与隐私条款。` / `I have read and agree to the agreements and privacy terms listed in the footer.`，不再叠加第二段提示。
- 找回密码、OAuth 回调、管理端登录不放用户同意提示。第三方 OAuth 应用自身的协议 URL 不属于重复的 BGSSAI 法务入口。
- 设置/关于/我的页面已有全局页脚时仅可补充「法律信息」中心入口。没有全局页脚的原生或桌面端保留当前页面唯一一组可访问链接，不能移除唯一入口。
- 中文协议指向 `https://www.bgssai.com/terms-of-service/`、`https://www.bgssai.com/privacy-policy/`，英文指向 `/en/` 同名页面。新标签页带 `rel="noopener noreferrer"`，原生端在系统浏览器打开。

### 15.2 页脚与备案

- 全局页脚只挂载一次，居中、灰字；桌面一行，窄屏正常换行，不用固定高度或浮层遮挡正文。
- 境内为 `© 当年 昆山兵贵神速智能科技有限公司 · 用户协议 · 隐私政策（· 已获备案号）`；境外为 `© 当年 BGSSAI · Terms · Privacy`；随界面语言渲染，不中英并列。
- 备案链接 `https://beian.miit.gov.cn/`。已有号：website `-1`、blog `-2`、marklens `-3`、publish `-4`、magic `-5`、voiceunion `-6`、geo-cn `-7`、wiki `-8`、HR `-9`，主体为 `苏ICP备2024070062号`。未获备案的产品不得借用其他产品编号。
- 页脚不放下载按钮、五份协议清单、地址电话或内部审阅备注。个人信息收集清单、第三方共享清单、权限说明统一由官网 `/legal/` 及隐私正文承载。
- 官网自身页脚保留服务条款、隐私政策、法律信息与备案及语言切换；法律中心目录和文档正文的正常引用不视作页面导航重复。

### 15.3 内部备注不上界面

「需法务审阅」「法务正文以官网为准」「待法务审阅」「Needs legal review」只作内部文档备注，产品端不渲染，也不写入 DOM 属性。不要用隐藏重复节点实现去重。

### 15.4 验收

- 登录、注册、设置和普通页面同一 BGSSAI 协议最多一个可见导航入口，路由切换不累积页脚。
- 保留注册勾选校验、服务端同意记录和文档 URL；管理端不新增用户同意提示。
- 桌面与窄屏检查链接、语言、换行、备案号及浮动按钮避让。
- 无页脚原生端保留唯一入口；真机、线上与本地验证分别报告。
