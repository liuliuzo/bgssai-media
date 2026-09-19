var api = require('../../utils/api.js');

Page({
  data: {
    username: 'demo',
    password: 'user123',
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
      .loginPassword('user', self.data.username, self.data.password)
      .then(function (body) {
        self.setData({ loading: false });
        if (!body || !body.success) {
          self.setData({
            error: (body && body.message) || '登录失败（需 user 后端可访问）',
          });
          return;
        }
        var result = body.result || {};
        var token = result.token || result.jwttoken || result.jwt_token;
        getApp().setSession('user', token, result);
        wx.redirectTo({ url: '/pages/home-user/home-user' });
      })
      .catch(function () {
        self.setData({
          loading: false,
          error: '网络失败：检查 config/env.js 的 userApiBase 与合法域名',
        });
      });
  },
  openH5: function () {
    var url = encodeURIComponent(api.env.userH5Login);
    wx.navigateTo({ url: '/pages/webview/webview?url=' + url + '&title=用户H5' });
  },
});
