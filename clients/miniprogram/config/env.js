/**
 * 单小程序双入口配置。
 * appid 写在 project.config.json（占位 touristappid）；上线前换成正式 AppID。
 * 业务 API 默认指向本机 user/admin 后端；真机/体验版需换成可访问域名并在小程序后台配置 request 合法域名。
 */
module.exports = {
  productName: 'BGSSAI Media',
  /** 占位；与 project.config.json 的 appid 同步维护 */
  appId: 'touristappid',
  userApiBase: 'http://127.0.0.1:8081',
  adminApiBase: 'http://127.0.0.1:8080',
  /** 可选：用 web-view 承载已有 H5（需业务域名） */
  userH5Login: 'http://127.0.0.1:3002/login',
  adminH5Login: 'http://127.0.0.1:3001/login',
};
