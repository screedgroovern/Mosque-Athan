const fs = require('fs');

const core = fs.readFileSync('app/src/main/java/com/groovernapp/mosqueathan/server/WebPageJsCore.kt', 'utf8');
const connection = fs.readFileSync('app/src/main/java/com/groovernapp/mosqueathan/server/WebPageJsConnection.kt', 'utf8');
const offline = fs.readFileSync('app/src/main/java/com/groovernapp/mosqueathan/server/WebPageJsOffline.kt', 'utf8');
const ui = fs.readFileSync('app/src/main/java/com/groovernapp/mosqueathan/server/WebPageJsUI.kt', 'utf8');
const actions = fs.readFileSync('app/src/main/java/com/groovernapp/mosqueathan/server/WebPageJsActions.kt', 'utf8');

function extractJs(kotlinCode) {
    const start = kotlinCode.indexOf('"""');
    const end = kotlinCode.lastIndexOf('"""');
    if (start !== -1 && end !== -1 && start !== end) {
        return kotlinCode.substring(start + 3, end);
    }
    return '';
}

const js = extractJs(core) + "\n" + extractJs(connection) + "\n" + extractJs(offline) + "\n" + extractJs(ui) + "\n" + extractJs(actions);
fs.writeFileSync('test_webpage_full.js', js);
console.log("Written to test_webpage_full.js");
