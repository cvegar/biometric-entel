package biometric.entel;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.content.Context;

import org.apache.cordova.CordovaPlugin;

//import javax.naming.Context;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class BiometricCordova extends CordovaPlugin {

   private static final String TAG = "BiometricCordova";
    private static final int CAPTURE_REQUEST = 1001;
    private CallbackContext currentCallbackContext; // Unificado para manejar todos los callbacks

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.currentCallbackContext = callbackContext;
        Log.d(TAG, "Ejecutando acción: " + action);

        try {
            if (action == null || action.isEmpty()) {
                callbackContext.error("Acción no válida");
                return false;
            }

            // Verificar permisos primero
            if (!hasBiometricPermission()) {
                callbackContext.error("Permisos biométricos no concedidos");
                return false;
            }

            switch (action) {
                case "launchScanCrypto":
                    return handleLaunchScanCrypto(callbackContext);
                    
                case "captureFingerprint":
                    return handleCaptureFingerprint(args, callbackContext);
                    
                default:
                    callbackContext.error("Acción no reconocida: " + action);
                    return false;
            }
        } catch (JSONException e) {
            callbackContext.error("Error en parámetros: " + e.getMessage());
            return false;
        } catch (Exception e) {
            callbackContext.error("Error inesperado: " + e.getMessage());
            return false;
        }
    }

    private boolean hasBiometricPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return cordova.getActivity().checkSelfPermission(android.Manifest.permission.USE_BIOMETRIC) 
                == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean handleCaptureFingerprint(JSONArray args, CallbackContext callbackContext) throws JSONException {
        try {
            String instructions = args.optString(0, "Coloca tu dedo en el lector");
            String rightFinger = args.optString(1, "thumb_right");
            String leftFinger = args.optString(2, "index_left");

            Context context = cordova.getActivity().getApplicationContext();
            Intent intent = new Intent(context, CaptureFingerprintActivity.class);
            intent.putExtra("instructions", instructions);
            intent.putExtra("right_finger", rightFinger);
            intent.putExtra("left_finger", leftFinger);

            cordova.setActivityResultCallback(this);
            cordova.startActivityForResult(this, intent, CAPTURE_REQUEST);

            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            
            return true;
        } catch (Exception e) {
            callbackContext.error("Error iniciando captura: " + e.getMessage());
            return false;
        }
    }

    private boolean handleLaunchScanCrypto(CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Lanzando escaneo criptográfico");
            Context context = cordova.getActivity().getApplicationContext();
            Intent intent = new Intent(context, ScanActionCryptoActivity.class);
            
            // Usar el mismo método que para captureFingerprint
            cordova.setActivityResultCallback(this);
            cordova.startActivityForResult(this, intent, CAPTURE_REQUEST);
            
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error al lanzar escaneo cripto: " + e.getMessage());
            callbackContext.error(e.getMessage());
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (currentCallbackContext == null) {
            Log.e(TAG, "CallbackContext es nulo");
            return;
        }

        try {
            JSONObject result = new JSONObject();
            
            if (requestCode == CAPTURE_REQUEST) {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String wsqBase64 = data.getStringExtra("finger");
                   String serialNumber = data.getStringExtra("serialnumber");
                    if (serialNumber == null) {
                        serialNumber = "N/A";
                    }
                    if (wsqBase64 != null && !wsqBase64.isEmpty()) {
                        result.put("success", true);
                        result.put("wsq", wsqBase64);
                        result.put("serialNumber", serialNumber);
                        result.put("message", "Captura exitosa");
                        currentCallbackContext.success(result);
                    } else {
                        result.put("success", false);
                        result.put("message", "Datos WSQ no recibidos");
                        currentCallbackContext.error(result);
                    }
                } else {
                    result.put("success", false);
                    result.put("message", resultCode == Activity.RESULT_CANCELED ? 
                        "Captura cancelada" : "Error en el dispositivo");
                    currentCallbackContext.error(result);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error procesando resultado: " + e.getMessage());
            currentCallbackContext.error("Error procesando resultado: " + e.getMessage());
        } finally {
            currentCallbackContext = null; // Limpiar después de usar
        }
    }
}
