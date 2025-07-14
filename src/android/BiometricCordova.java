package biometric.entel;

import android.app.Activity;
import android.content.Intent;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import biometric.entel.MainActivity;
import biometric.entel.ScanActionCryptoActivity;



/**
 * This class echoes a string called from JavaScript.
 */
public class BiometricCordova extends CordovaPlugin {

    private static final String TAG = "BiometricCordova";
    private static final int CAPTURE_REQUEST = 1001;
    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        Log.d(TAG, "Ejecutando acción: " + action + " con args: " + args.toString());

        try {
            if (action == null || action.isEmpty()) {
                Log.e(TAG, "Acción no especificada");
                callbackContext.error("Acción no válida");
                return false;
            }

            switch (action) {
                case "launchMainActivity":
                    return handleLaunchMainActivity();
                    
                case "launchScanCrypto":
                    return handleLaunchScanCrypto();
                    
                case "captureFingerprint":
                    return handleCaptureFingerprint(args);
                    
                case "getDeviceInfo":
                    return handleGetDeviceInfo();
                    
                case "checkCompatibility":
                    return handleCheckCompatibility();
                    
                default:
                    Log.w(TAG, "Acción no reconocida: " + action);
                    callbackContext.error("Acción no reconocida");
                    return false;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error JSON: " + e.getMessage());
            callbackContext.error("Error en parámetros: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error inesperado: " + e.getMessage());
            callbackContext.error("Error inesperado: " + e.getMessage());
            return false;
        }
    }

    // Métodos handlers para cada acción
    private boolean handleLaunchMainActivity() {
        try {
            Log.d(TAG, "Lanzando actividad principal");
            Intent intent = new Intent(cordova.getActivity(), MainBiometricActivity.class);
            cordova.getActivity().startActivity(intent);
            callbackContext.success();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error al lanzar actividad principal: " + e.getMessage());
            callbackContext.error(e.getMessage());
            return false;
        }
    }

    private boolean handleLaunchScanCrypto() {
        try {
            Log.d(TAG, "Lanzando escaneo criptográfico");
            Intent intent = new Intent(cordova.getActivity(), CryptoScanActivity.class);
            cordova.getActivity().startActivity(intent);
            callbackContext.success();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error al lanzar escaneo cripto: " + e.getMessage());
            callbackContext.error(e.getMessage());
            return false;
        }
    }

    private boolean handleCaptureFingerprint(JSONArray args) throws JSONException {
        try {
            String instructions = args.optString(0, "Coloca tu dedo en el lector");
            String rightFinger = args.optString(1, "thumb_right");
            String leftFinger = args.optString(2, "index_right");

            Log.d(TAG, "Preparando captura con params - Instrucciones: " + instructions + 
                ", Dedo derecho: " + rightFinger + ", Dedo izquierdo: " + leftFinger);

            Intent intent = new Intent(cordova.getActivity(), CaptureFingerprintActivity.class);
            intent.putExtra("instructions", instructions);
            intent.putExtra("right_finger", rightFinger);
            intent.putExtra("left_finger", leftFinger);

            cordova.setActivityResultCallback(this);
            cordova.getActivity().startActivityForResult(intent, CAPTURE_REQUEST);
            
            // Mantener el callback activo para la respuesta asíncrona
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error en captura de huella: " + e.getMessage());
            callbackContext.error("Error en captura: " + e.getMessage());
            return false;
        }
    }

    private boolean handleGetDeviceInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("model", Build.MODEL);
            info.put("sdkVersion", Build.VERSION.SDK_INT);
            
            callbackContext.success(info);
            return true;
        } catch (Exception e) {
            callbackContext.error("Error obteniendo info dispositivo");
            return false;
        }
    }

    private boolean handleCheckCompatibility() {
        try {
            PackageManager pm = cordova.getActivity().getPackageManager();
            boolean hasFeature = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
            
            JSONObject result = new JSONObject();
            result.put("compatible", hasFeature);
            result.put("message", hasFeature ? "Dispositivo compatible" : "No soporta huellas dactilares");
            
            callbackContext.success(result);
            return true;
        } catch (Exception e) {
            callbackContext.error("Error verificando compatibilidad");
            return false;
        }
    }
}
