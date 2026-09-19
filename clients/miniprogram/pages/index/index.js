var env = require('../../config/env.js');

Page({
  data: {
    productName: env.productName,
  },
  enterUser: function () {
    wx.navigateTo({ url: '/pages/login-user/login-user' });
  },
  enterAdmin: function () {
    wx.navigateTo({ url: '/pages/login-admin/login-admin' });
  },
});
