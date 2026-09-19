Page({
  data: {
    src: '',
  },
  onLoad: function (query) {
    var src = query && query.url ? decodeURIComponent(query.url) : '';
    var title = query && query.title ? decodeURIComponent(query.title) : 'H5';
    if (title) {
      wx.setNavigationBarTitle({ title: title });
    }
    this.setData({ src: src });
  },
});
