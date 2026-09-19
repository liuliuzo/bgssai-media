var env = require('../config/env.js');

function request(options) {
  var role = options.role || 'user';
  var base = role === 'admin' ? env.adminApiBase : env.userApiBase;
  var header = Object.assign(
    {
      'Content-Type': 'application/json',
    },
    options.header || {}
  );
  var app = getApp();
  if (app && app.globalData && app.globalData.token) {
    header.Jwttoken = app.globalData.token;
  }
  return new Promise(function (resolve, reject) {
    wx.request({
      url: base + options.path,
      method: options.method || 'GET',
      data: options.data,
      header: header,
      success: function (res) {
        resolve(res.data);
      },
      fail: reject,
    });
  });
}

function loginPassword(role, username, password) {
  return request({
    role: role,
    method: 'POST',
    path: '/api/auth/login',
    data: {
      username: username,
      password: password,
    },
  });
}

module.exports = {
  env: env,
  request: request,
  loginPassword: loginPassword,
};
