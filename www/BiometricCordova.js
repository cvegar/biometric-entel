var exec = require('cordova/exec');

var BiometricCordova = {
    // Métodos existentes
    launchMainActivity: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "BiometricCordova", "launchMainActivity", []);
    },

    launchScanCrypto: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "BiometricCordova", "launchScanCrypto", []);
    },

    // Nuevo método mejorado para captura de huella
    captureFingerprint: function(params) {
        return new Promise(function(resolve, reject) {
            var instructions = params.instructions || "Coloca tu dedo en el lector";
            var rightFinger = params.rightFinger || "thumb_right";
            var leftFinger = params.leftFinger || "index_right";
            
            exec(
                function(result) {
                    if (typeof result === 'string') {
                        try {
                            result = JSON.parse(result);
                        } catch (e) {
                            return reject("Formato de respuesta inválido");
                        }
                    }
                    resolve(result);
                },
                function(error) {
                    reject(error || "Error desconocido en captura de huella");
                },
                "BiometricCordova",
                "captureFingerprint", 
                [instructions, rightFinger, leftFinger]
            );
        });
    },

    // Versión simplificada con callback
    simpleCapture: function(successCallback, errorCallback) {
        this.captureFingerprint({})
            .then(successCallback)
            .catch(errorCallback);
    }
};

// Exportación para CommonJS y navegador
if (typeof module !== 'undefined' && module.exports) {
    module.exports = BiometricCordova;
}
if (typeof window !== 'undefined') {
    window.BiometricCordova = BiometricCordova;
}