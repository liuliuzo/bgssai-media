(function () {
  var cfg = window.BGSSAI_MEDIA_SHELL || {};

  function go(url) {
    if (!url) {
      return;
    }
    window.location.href = url;
  }

  var userBtn = document.getElementById('enter-user');
  var adminBtn = document.getElementById('enter-admin');
  if (userBtn) {
    userBtn.addEventListener('click', function () {
      go(cfg.userUrl);
    });
  }
  if (adminBtn) {
    adminBtn.addEventListener('click', function () {
      go(cfg.adminUrl);
    });
  }
})();
