const fs = require('fs');

function extractJs(filename) {
    let content = fs.readFileSync('app/src/main/java/com/groovernapp/mosqueathan/server/' + filename, 'utf8');
    let match = content.match(/\"\"\"([\s\S]*?)\"\"\"/);
    return match ? match[1] : '';
}

let combined = 
    extractJs('WebPageJsCore.kt') + '\n' +
    extractJs('WebPageJsConnection.kt') + '\n' +
    extractJs('WebPageJsOffline.kt') + '\n' +
    extractJs('WebPageJsUI.kt') + '\n' +
    extractJs('WebPageJsActions.kt') + '\n' +
    "setLanguage(currentLang);\ninitializeOfflineState();\nconnect();";

try {
    new Function(combined);
    console.log('Concatenated JS is valid!');
} catch(e) {
    console.log('Syntax Error in combined JS: ' + e.message);
    
    // Find approximate line number
    try {
        let lines = combined.split('\n');
        for (let i = 0; i < lines.length; i++) {
            try {
                new Function(lines.slice(0, i + 1).join('\n'));
            } catch (err) {
                if (err.message.includes("Unexpected end of input")) continue;
                console.log("Error likely near line " + i + ": " + lines[i]);
                break;
            }
        }
    } catch (ignore) {}
}
