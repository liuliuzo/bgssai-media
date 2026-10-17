# bgssai-media-pc：OPC规划功能需求与验收补充

版本：2026-10-17。来源：用户本轮完整规划；存量需求基线：`2b21ee58264ab026aa1b6ca5243501ccdfb98c83`。

本次交付是用户要求的需求与测试设计，未实现、部署或执行这些功能。下列条目将规划细化为可验证目标；不表示现有产品已经具备。既有详细需求继续有效，明确冲突按本轮用户规划修正，未涉及字段和接口不在本文件重新定义。原型、详设、代码、数据库由相应功能实施任务按仓库流程推进，本次不将规划状态改为已实现。

## 产品职责

视频发布和在线观看平台，对标YouTube/YouTube Shorts；PC另承担VLC式本地主流格式播放。

参考：VLC；YouTube；YouTube Shorts。参考用于产品方向，不表示复制实现或已经具备参考产品全部功能。

区域/身份范围：境内；模型仅Tokenhub CN；微信/抖音/百度/支付宝。Website公开官网沿用无用户登录；Bot嵌入Chat；Build Add API key使用Developer凭证，公开客户端授权与API Key路径分别验收。

## 功能与验收条件

| 需求ID | 能力 | 功能要求 | 通过/失败判定 | 用例 |
| --- | --- | --- | --- | --- |
| REQ-MEDIA-PC-PRODUCT-001 | 创作者中心发布 | 各端均提供创作者中心，支持上传、填写作品信息、查看处理状态和发布结果。 | 作品属于当前创作者；处理、发布状态真实；详情使用本次视频与元数据。 异常：明确失败或不支持；不可播放作品不冒充已就绪；重复请求不产生重复作品。 | [TC-MEDIA-PC-PRODUCT-001-P / TC-MEDIA-PC-PRODUCT-001-N](../test/TC-opc-product-20261017.md) |
| REQ-MEDIA-PC-PRODUCT-002 | 正片与剧集播放 | 平台支持视频目录、详情、正片和有序分集播放，并记录继续观看位置。 | 正确播放对应集；恢复位置与进度一致；集顺序和标题正确。 异常：明确不可用并允许恢复或返回目录；不播放其他作品替代。 | [TC-MEDIA-PC-PRODUCT-002-P / TC-MEDIA-PC-PRODUCT-002-N](../test/TC-opc-product-20261017.md) |
| REQ-MEDIA-PC-PRODUCT-003 | Shorts连续播放 | 短视频流支持连续浏览，切换后释放上一视频播放并记录实际观看行为。 | 当前视频与元数据一致；不叠加播放声音；观看状态对应真实行为。 异常：不串音、不错误标记观看完成；显示加载、重试或空状态。 | [TC-MEDIA-PC-PRODUCT-003-P / TC-MEDIA-PC-PRODUCT-003-N](../test/TC-opc-product-20261017.md) |
| REQ-MEDIA-PC-PRODUCT-004 | 作品管理与访问控制 | 创作者可管理自己的作品，修改元信息或下架，其他用户只能访问有权公开的内容。 | 修改持久化；下架后公开目录和直接播放访问按可见性收回。 异常：拒绝所有越权操作，甲作品不变，私有地址不泄露。 | [TC-MEDIA-PC-PRODUCT-004-P / TC-MEDIA-PC-PRODUCT-004-N](../test/TC-opc-product-20261017.md) |
| REQ-MEDIA-PC-PRODUCT-005 | Short与Long交接 | 接收Short短剧与Long长剧成片，保留来源、版本、剧集信息和发布回执。 | 两类成片正确进入短视频或剧集目录；版本、顺序和实际文件一致。 异常：不重复发布；显示具体失败或不完整状态，不声称整部交付成功。 | [TC-MEDIA-PC-PRODUCT-005-P / TC-MEDIA-PC-PRODUCT-005-N](../test/TC-opc-product-20261017.md) |
| REQ-MEDIA-PC-PRODUCT-006 | 平台播放能力矩阵 | 各端按容器、编码、音轨、字幕和系统版本登记真实可播能力；PC本地播放与Web/移动在线播放分开。 | 每个格式有可复现证据；在线播放不宣称等同PC全格式能力。 异常：明确受限或失败原因，不崩溃、不把黑屏计作可播放。 | [TC-MEDIA-PC-PRODUCT-006-P / TC-MEDIA-PC-PRODUCT-006-N](../test/TC-opc-product-20261017.md) |
| REQ-MEDIA-PC-PRODUCT-007 | PC本地播放器 | PC支持打开本地主流格式文件、播放列表、暂停定位、音轨字幕选择和全屏，能力参考VLC并由样本矩阵验证。 | 文件可播放；音画和字幕同步；播放列表及控制响应正确。 异常：错误清晰且不崩溃；保留其他有效播放项；不将未支持格式声称已覆盖。 | [TC-MEDIA-PC-PRODUCT-007-P / TC-MEDIA-PC-PRODUCT-007-N](../test/TC-opc-product-20261017.md) |
| REQ-MEDIA-PC-COMMON-001 | 自助注册与本地账号 | OPC 用户可自行注册使用产品；本地账号与 Chat 第三方账号按显式绑定关系关联，不静默合并同名用户。 | 注册无需平台管理员代办；登录身份正确；业务记录归属该用户，重开可读取。 异常：错误密码不签发会话；重复注册有明确提示；不同主体不会自动获取已有用户的数据。 | [TC-MEDIA-PC-COMMON-001-P / TC-MEDIA-PC-COMMON-001-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-COMMON-002 | 邮箱与手机验证码 | 邮箱验证码和手机验证码均可登录；验证码真实投递、限时、单次使用并绑定接收人及用途。 | 两种方式均能建立正确用户会话；投递与消费有可追踪记录，客户端不直接返回验证码。 异常：所有无效验证码均拒绝且不建立会话；达到限额后提示稍后重试；真实短信或邮件未就绪记阻塞。 | [TC-MEDIA-PC-COMMON-002-P / TC-MEDIA-PC-COMMON-002-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-COMMON-003 | 平台管理权限隔离 | 平台 admin 是内部后台账户，不开放注册、不接受用户侧第三方登录；业务组织成员仍属于 user 域。 | 管理员只凭内部管理身份访问管理功能；用户只访问授权的业务数据。 异常：不创建平台管理员；拒绝用户令牌越权；不得仅靠隐藏页面控制权限。 | [TC-MEDIA-PC-COMMON-003-P / TC-MEDIA-PC-COMMON-003-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-COMMON-004 | 第三方授权独立会话 | 应用接受所在区域 Chat 作为第三方身份提供方，授权后签发应用自己的用户会话；不以共享全线会话替代授权。 | 每个应用均有自己的授权记录和会话；本应用获得的用户身份与授权一致，不获得平台管理权限。 异常：取消或无效回调不建立会话；授权码不重复消费；已撤销授权不可继续调用，未授权应用不可借用会话。 | [TC-MEDIA-PC-COMMON-004-P / TC-MEDIA-PC-COMMON-004-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-COMMON-005 | 数据归属与失效会话 | 用户数据按所有者或业务组织权限隔离，注销或失效会话不可继续操作。 | 持久化数据与授权范围一致；重新登录可以恢复自己的记录。 异常：所有越权读写被拒绝，不泄露记录内容；失效会话要求重新登录；无权修改不产生业务变更。 | [TC-MEDIA-PC-COMMON-005-P / TC-MEDIA-PC-COMMON-005-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-COMMON-006 | 微信区域登录 | 本产品用户端支持微信第三方登录；回调校验、取消、绑定及账号隔离遵循现有授权契约。 | 登录到相同的已绑定业务用户；应用会话独立，授权不产生平台admin身份。 异常：取消与无效回调不建立会话；不同身份不误合并；缺真实渠道配置则记阻塞。 | [TC-MEDIA-PC-COMMON-006-P / TC-MEDIA-PC-COMMON-006-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-COMMON-007 | 抖音区域登录 | 本产品用户端支持抖音第三方登录；回调校验、取消、绑定及账号隔离遵循现有授权契约。 | 登录到相同的已绑定业务用户；应用会话独立，授权不产生平台admin身份。 异常：取消与无效回调不建立会话；不同身份不误合并；缺真实渠道配置则记阻塞。 | [TC-MEDIA-PC-COMMON-007-P / TC-MEDIA-PC-COMMON-007-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-COMMON-008 | 百度区域登录 | 本产品用户端支持百度第三方登录；回调校验、取消、绑定及账号隔离遵循现有授权契约。 | 登录到相同的已绑定业务用户；应用会话独立，授权不产生平台admin身份。 异常：取消与无效回调不建立会话；不同身份不误合并；缺真实渠道配置则记阻塞。 | [TC-MEDIA-PC-COMMON-008-P / TC-MEDIA-PC-COMMON-008-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-COMMON-009 | 支付宝区域登录 | 本产品用户端支持支付宝第三方登录；回调校验、取消、绑定及账号隔离遵循现有授权契约。 | 登录到相同的已绑定业务用户；应用会话独立，授权不产生平台admin身份。 异常：取消与无效回调不建立会话；不同身份不误合并；缺真实渠道配置则记阻塞。 | [TC-MEDIA-PC-COMMON-009-P / TC-MEDIA-PC-COMMON-009-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-COMMON-010 | 同区模型原生能力 | 境内应用仅通过 Tokenhub CN、境外仅通过 Tokenhub Global 调用模型；Short/Long 固定 CN，不因故障跨区回退。 | 结果与实际模型响应一致；请求只经过当前区域中枢，记录真实用量和任务标识。 异常：明确返回不可用或失败；不静默跨区、不返回演示内容冒充真实生成、不重复执行同一任务。 | [TC-MEDIA-PC-COMMON-010-P / TC-MEDIA-PC-COMMON-010-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-COMMON-011 | 无感选模与倍率 | 普通应用只向用户展示模型名称、能力、可用性和0.5倍/1倍/1.5倍额度倍率，不要求用户理解或另行配置 Tokenhub。 | 三种倍率在提交前可见并与目录一致；普通业务流程无需单独开通中枢或填写厂商 Key。 异常：提交前重新校验或明确提示变化；不按隐藏新倍率扣费，不展示可选但无法识别的模型。 | [TC-MEDIA-PC-COMMON-011-P / TC-MEDIA-PC-COMMON-011-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-COMMON-012 | 额度与用量可追溯 | BGSSAI额度套餐须展示余额和消耗规则；厂商账户余额与用户额度分开；具体计量、舍入、失败退款和跨应用钱包规则沿用已确认规则，未定义项不能默认补造。 | 在该测试规则下额度消耗分别为50、100、150；流水包含任务、模型、倍率和规则版本；实际生产计量必须有已确认口径。 异常：不重复扣减、不出现无来源流水；余额不足有清晰拒绝；按明确规则结算失败任务；缺失规则的场景记阻塞，不自行认定免费或扣费。 | [TC-MEDIA-PC-COMMON-012-P / TC-MEDIA-PC-COMMON-012-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-COMMON-013 | 语言与真实状态 | 用户端支持中英文，境内默认中文、境外默认英文；切换后主要业务、错误和法律入口保持一致，规划能力与已可用能力分开显示。 | 默认语言符合区域；切换后持久化；核心流程和错误可理解；法律文本可打开，未上线能力不显示已完成。 异常：显示失败、空状态或未发布说明，不用样例掩盖；公共法律入口不要求登录。 | [TC-MEDIA-PC-COMMON-013-P / TC-MEDIA-PC-COMMON-013-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-COMMON-014 | Bot下载与安装信息 | 所有应用提供 Bot 下载入口，平台、架构、版本和安装包状态真实可见；Bot制品不能冒充本应用制品。 | 下载得到对应系统真实安装包且能启动；未发布版本明确标注；应用安装包和 Bot 安装包分别标识。 异常：不返回伪成功或错误平台包；显示不可用及实际限制；下载入口不泄露内部存储凭据。 | [TC-MEDIA-PC-COMMON-014-P / TC-MEDIA-PC-COMMON-014-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-COMMON-015 | Bot最小授权托管 | 产品开放可被 Bot 调用的业务能力；用户明确授权后执行自己的业务任务，并可撤销。Support采用独立内部权限。 | 实际业务变化与指令一致，结果属于甲；记录授权范围、调用任务和结果；不凭文本回答认定已操作。 异常：越权与已撤销调用被拒绝；相同任务不重复产生业务副作用；内部权限不由用户授权提升。 | [TC-MEDIA-PC-COMMON-015-P / TC-MEDIA-PC-COMMON-015-N](../test/TC-opc-common-20261017.md) |
| REQ-MEDIA-PC-PLATFORM-001 | Windows核心交付 | 本产品在Windows提供独立可安装用户端；核心流程与服务端权限一致，移动端沿用原生界面要求。 | 制品可安装启动，名称与平台正确；不能用Bot安装包或仅有打包工程替代；业务结果持久化且与其他已授权端一致；保留实际设备和制品证据。 异常：网络和会话错误可恢复，无重复业务副作用；升级不丢已保存数据；缺真实设备/制品时记阻塞，不能标通过。 | [TC-MEDIA-PC-PLATFORM-001-P / TC-MEDIA-PC-PLATFORM-001-N](../test/TC-opc-platform-20261017.md) |
| REQ-MEDIA-PC-PLATFORM-002 | macOS核心交付 | 本产品在macOS提供独立可安装用户端；核心流程与服务端权限一致，移动端沿用原生界面要求。 | 制品可安装启动，名称与平台正确；不能用Bot安装包或仅有打包工程替代；业务结果持久化且与其他已授权端一致；保留实际设备和制品证据。 异常：网络和会话错误可恢复，无重复业务副作用；升级不丢已保存数据；缺真实设备/制品时记阻塞，不能标通过。 | [TC-MEDIA-PC-PLATFORM-002-P / TC-MEDIA-PC-PLATFORM-002-N](../test/TC-opc-platform-20261017.md) |
| REQ-MEDIA-PC-PLATFORM-003 | Linux核心交付 | 本产品在Linux提供独立可安装用户端；核心流程与服务端权限一致，移动端沿用原生界面要求。 | 制品可安装启动，名称与平台正确；不能用Bot安装包或仅有打包工程替代；业务结果持久化且与其他已授权端一致；保留实际设备和制品证据。 异常：网络和会话错误可恢复，无重复业务副作用；升级不丢已保存数据；缺真实设备/制品时记阻塞，不能标通过。 | [TC-MEDIA-PC-PLATFORM-003-P / TC-MEDIA-PC-PLATFORM-003-N](../test/TC-opc-platform-20261017.md) |
| REQ-MEDIA-PC-REGRESSION-001 | 单会话失效 | 沿用后登踢先登：新登录更新当前会话；旧会话收到约定失效提示，所有登录方式一致。 | 乙会话有效；账号当前会话与乙一致。 异常：返回HTTP401及既有code=2003等本仓约定的失效语义，提示账号已在其他设备登录；不产生写入。 | [TC-MEDIA-PC-REGRESSION-001-P / TC-MEDIA-PC-REGRESSION-001-N](../test/TC-feature-gaps-20261017.md) |
| REQ-MEDIA-PC-REGRESSION-002 | 登录页单一入口 | 登录页去重，按当前产品显示一次语言选择、协议和登录方式；窄屏仍可操作。 | 同一功能不重复出现；字段、提交、语言及协议可见可达。 异常：错误定位正确；不残留旧表单信息；关键按钮不被遮挡或溢出。 | [TC-MEDIA-PC-REGRESSION-002-P / TC-MEDIA-PC-REGRESSION-002-N](../test/TC-feature-gaps-20261017.md) |
| REQ-MEDIA-PC-REGRESSION-003 | 法律入口去重 | 五份法律文本在规定入口可匿名打开，同一页面不重复堆叠协议块，不改变已有同意状态。 | 入口唯一且文本对应当前产品语言，原表单可继续，未虚构证照或审定状态。 异常：缺失明确；不靠重复入口替代正确文本；按现有同意契约校验。 | [TC-MEDIA-PC-REGRESSION-003-P / TC-MEDIA-PC-REGRESSION-003-N](../test/TC-feature-gaps-20261017.md) |
| REQ-MEDIA-PC-REGRESSION-004 | 动态效果与可访问性 | 动态视觉不阻挡核心流程，尊重减少动画设置，低性能和无动画时功能仍可使用。 | 页面内容与操作不依赖动画完成；减少动画时静止或简化，交互仍可用。 异常：不遮挡按钮或重复发请求，不因特效错误导致白屏。 | [TC-MEDIA-PC-REGRESSION-004-P / TC-MEDIA-PC-REGRESSION-004-N](../test/TC-feature-gaps-20261017.md) |
| REQ-MEDIA-PC-REGRESSION-005 | 在线客服与工单入口 | 应用提供连接内部Support的用户咨询与工单入口，用户能查看本人会话和处理结果。 | 来源产品和用户正确，回复实际送达，工单状态可追踪。 异常：不丢消息或重复建单，越权拒绝，失败不显示已处理。 | [TC-MEDIA-PC-REGRESSION-005-P / TC-MEDIA-PC-REGRESSION-005-N](../test/TC-feature-gaps-20261017.md) |
| REQ-MEDIA-PC-REGRESSION-006 | 片库就绪与格式路由 | READY短剧长剧可交错浏览，发布重放返回同media_id及format_hook；Web格式决策与PC播放能力分开。 | 分别PLAY/REJECT/PLAY/UNKNOWN；重放相同media_id；READY的桌面容器可入库但Web拒绝，不自动将NOT_VERIFIED改通过。 异常：片库过滤与冲突校验生效；不假报播放成功，实际样本必须独立验收。 | [TC-MEDIA-PC-REGRESSION-006-P / TC-MEDIA-PC-REGRESSION-006-N](../test/TC-feature-gaps-20261017.md) |

## 规则边界与依赖

- 正常、失败、边界、权限和重复执行分别验证；历史用例和历史执行结果保留。新增用例统一待测。
- 所有应用最终覆盖Windows、macOS、Linux、iOS、Android，Support除外；共享/管理仓不计为应用。Media由PC承担桌面三端、Android/iOS各自承担移动端，Web提供浏览器平台，不重复要求每个Media仓另造五端。Web/桌面/CLI是Build的产品形态，不能替代操作系统交付矩阵。
- 主流模型、Office版本、招聘平台API等开放集合，须维护逐项能力、限制、样例和实测证据；缺凭据、设备、接口或样本记阻塞，不能以有限样例宣称全部支持。
- 模型实际路由严格同区，故障不跨区回退。普通应用不要求用户配置中枢；Tokenhub本身和Developer门户可展示API管理概念。
- 倍率目标为0.5/1/1.5。已确认的本仓计量与价格规则继续有效；未确认的基础单位、舍入、退款和跨应用钱包不得由测试推定。100基础用量仅为可复现测试夹具，不是生产定价。
- 用户授权只作用于user；admin不注册、不接用户第三方授权。Support内部自动化不能借用user授权。
- dev/prod均部署develop，分别使用application-dev.properties/application-prod.properties。本次不触发部署、真实支付、真实邮件或外部投稿。

## 存量需求与追溯

[需求文件覆盖索引](../test/REQUIREMENT-COVERAGE.md)列出原有需求、历史套件及本轮补充入口；[本轮逐需求矩阵](../test/TRACEABILITY-20261017.md)列出本文件所有验收项与正/负用例。覆盖索引属于测试设计导航，不是运行通过证明。
