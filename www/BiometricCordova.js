var exec = require('cordova/exec');

var BiometricCordova = {
    launchMainActivity: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "BiometricCordova", "launchMainActivity", []);
    },

    launchScanCrypto: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "BiometricCordova", "launchScanCrypto", []);
    }
};

module.exports = BiometricCordova;