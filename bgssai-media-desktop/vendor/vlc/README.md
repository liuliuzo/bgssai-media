Optional bundled VLC engine.

Put a portable VLC here (vlc.exe / VLC.app / vlc) before `npm run dist` and electron-builder ships it as resources/vlc. The locator tries this folder before PATH, registry and Program Files. Empty by default: the product flow is "install system VLC".
