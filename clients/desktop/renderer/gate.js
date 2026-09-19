(function () {
  var shell = window.bgssaiMediaShell;
  var userBtn = document.getElementById('enter-user');
  var adminBtn = document.getElementById('enter-admin');

  if (userBtn) {
    userBtn.addEventListener('click', function () {
      if (shell && shell.enterUser) {
        shell.enterUser();
      }
    });
  }
  if (adminBtn) {
    adminBtn.addEventListener('click', function () {
      if (shell && shell.enterAdmin) {
        shell.enterAdmin();
      }
    });
  }
})();
