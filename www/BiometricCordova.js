var exec = require('cordova/exec');

exports.launchMainActivity = function (success, error) {
    exec(success, error, "BiometricCordova", "launchMainActivity", []);
};

exports.launchScanCrypto = function (success, error) {
    exec(success, error, "BiometricCordova", "launchScanCrypto", []);
};