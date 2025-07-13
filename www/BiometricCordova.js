function BiometricCordova() {}

// Método para lanzar MainActivity
BiometricCordova.prototype.launchMainActivity = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'BiometricCordova', 'launchMainActivity', []);
};

// Método para lanzar ScanActionCryptoActivity
BiometricCordova.prototype.launchScanCrypto = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'BiometricCordova', 'launchScanCrypto', []);
};

// Función de instalación
BiometricCordova.install = function() {
    if (!window.plugins) {
        window.plugins = {};
    }
    window.plugins.BiometricCordova = new BiometricCordova();
    return window.plugins.BiometricCordova;
};

// Registro del constructor
cordova.addConstructor(BiometricCordova.install);