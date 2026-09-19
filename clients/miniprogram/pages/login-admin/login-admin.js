var api = require('../../utils/api.js');

Page({
  data: {
    username: 'admin',
    password: 'admin123',
    loading: false,
    error: '',
  },
  onUser: function (e) {
    this.setData({ username: e.detail.value });
  },
  onPass: function (e) {
    this.setData({ password: e.detail.value });
  },
  onSubmit: function () {
    var self = this;
    self.setData({ loading: true, error: '' });
    api
      .loginPassword('admin', self.data.username, self.data.password)
      .then(function (body) {
        self.setData({ loading: false });
        if (!body || !body.success) {
          self.setData({
            error: (body && body.message) || '登录失败（需 admin 后端可访问）',
          });
          return;
        }
        var result = body.result || {};
        var token = result.token || result.jwttoken || result.jwt_token;
        getApp().setSession('admin', token, result);
        wx.redirectTo({ url: '/pages/home-admin/home-admin' });
      })
      .catch(function () {
        self.setData({
          loading: false,
          error: '网络失败：检查 config/env.js 的 adminApiBase 与合法域名',
        });
      });
  },
  openH5: function () {
    var url = encodeURIComponent(api.env.adminH5Login);
    wx.navigateTo({ url: '/pages/webview/webview?url=' + url + '&title=管理H5' });
  },
});
