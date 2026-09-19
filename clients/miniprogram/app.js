App({
  globalData: {
    role: null,
    token: null,
    userInfo: null,
  },
  onLaunch: function () {
    try {
      var role = wx.getStorageSync('bgssai_media_role');
      var token = wx.getStorageSync('bgssai_media_token');
      if (role) {
        this.globalData.role = role;
      }
      if (token) {
        this.globalData.token = token;
      }
    } catch (e) {
      // ignore storage errors in scaffold
    }
  },
  setSession: function (role, token, userInfo) {
    this.globalData.role = role;
    this.globalData.token = token || null;
    this.globalData.userInfo = userInfo || null;
    try {
      wx.setStorageSync('bgssai_media_role', role);
      if (token) {
        wx.setStorageSync('bgssai_media_token', token);
      }
    } catch (e) {
      // ignore
    }
  },
  clearSession: function () {
    this.globalData.role = null;
    this.globalData.token = null;
    this.globalData.userInfo = null;
    try {
      wx.removeStorageSync('bgssai_media_role');
      wx.removeStorageSync('bgssai_media_token');
    } catch (e) {
      // ignore
    }
  },
});
