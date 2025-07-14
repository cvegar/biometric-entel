var BiometricCordova = {
    // Lanza el escaneo criptográfico
    launchScanCrypto: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BiometricCordova', // Nombre del plugin (debe coincidir con config.xml)
            'launchScanCrypto',
            [] // Sin parámetros
        );
    },

    captureFingerprint: function(instructions, rightFinger, leftFinger, successCallback, errorCallback) {
        cordova.exec(
            function(result) {
                // Procesamiento estándar del resultado
                if (typeof result === 'string') {
                    try {
                        result = JSON.parse(result); // Para casos donde Cordova serializa el JSON
                    } catch (e) {
                        errorCallback("Formato de respuesta inválido");
                        return;
                    }
                }
                
                // Validación de estructura
                if (result && typeof result.success !== 'undefined') {
                    successCallback(result);
                } else {
                    errorCallback("Respuesta inesperada del plugin");
                }
            },
            errorCallback,
            'biometric-entel', // Nombre del plugin (debe coincidir con config.xml)
            'captureFingerprint',
            [
                instructions || "Coloca tu dedo en el lector",
                rightFinger || "thumb_right",
                leftFinger || "index_left"
            ]
        );
    },

    /**
     * Versión con Promesas (opcional)
     */
    captureFingerprintPromise: function(instructions, rightFinger, leftFinger) {
        return new Promise((resolve, reject) => {
            this.captureFingerprint(
                instructions,
                rightFinger,
                leftFinger,
                resolve,
                reject
            );
        });
    }
};

// Registro automático en window.plugins si Cordova está disponible
if (typeof window !== 'undefined' && window.cordova) {
    if (!window.plugins) window.plugins = {};
    window.plugins.BiometricCordova = BiometricCordova;
}

// Exportación para módulos (CommonJS/ES6)
if (typeof module !== 'undefined' && module.exports) {
    module.exports = BiometricCordova;
}