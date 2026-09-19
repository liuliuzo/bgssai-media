const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const www = path.join(root, 'www');
fs.mkdirSync(www, { recursive: true });
for (const name of ['index.html', 'styles.css', 'app.js', 'config.js']) {
  fs.copyFileSync(path.join(root, 'src', name), path.join(www, name));
}
console.log('www synced from src/');
