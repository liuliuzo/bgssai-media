Page({
  backGate: function () {
    wx.reLaunch({ url: '/pages/index/index' });
  },
  logout: function () {
    getApp().clearSession();
    wx.reLaunch({ url: '/pages/index/index' });
  },
});
