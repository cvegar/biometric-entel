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
        this.callbackContext = callbackContext; // Guarda el callback (IMPORTANTE)
        Log.d(TAG, "Ejecutando acción: " + action);

        try {
            if (action == null || action.isEmpty()) {
                callbackContext.error("Acción no válida");
                return false;
            }

            switch (action) {
                case "launchScanCrypto":
                    return handleLaunchScanCrypto();
                    
                case "captureFingerprint":
                    // Guarda el callback antes de iniciar la actividad
                    this.captureCallbackContext = callbackContext; // Nuevo campo necesario
                    return handleCaptureFingerprint(args);
                    
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


    private boolean handleCaptureFingerprint(JSONArray args) throws JSONException {
        try {
            String instructions = args.optString(0, "Coloca tu dedo en el lector");
            String rightFinger = args.optString(1, "thumb_right");
            String leftFinger = args.optString(2, "index_left");

            Context context = cordova.getActivity().getApplicationContext();
            Intent intent = new Intent(context, CaptureFingerprintActivity.class);
            intent.putExtra("instructions", instructions);
            intent.putExtra("right_finger", rightFinger);
            intent.putExtra("left_finger", leftFinger);

            // Configura el callback para el resultado (FALTABA)
            cordova.setActivityResultCallback(this);
            
            // Usa el método de Cordova para iniciar la actividad (Recomendado)
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

    private boolean handleLaunchScanCrypto() {
        try {
            Log.d(TAG, "Lanzando escaneo criptográfico");
            Context appContext = cordova.getActivity().getApplicationContext();
            Intent intent = new Intent(appContext, CaptureFingerprintActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            cordova.getActivity().startActivity(intent);
            callbackContext.success();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error al lanzar escaneo cripto: " + e.getMessage());
            callbackContext.error(e.getMessage());
            return false;
        }
    }

     

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_REQUEST) {
            try {
                JSONObject result = new JSONObject();
                
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String wsqBase64 = data.getStringExtra("finger");
                    String serialNumber = data.getStringExtra("serialnumber");
                    
                    if (wsqBase64 != null && !wsqBase64.isEmpty()) {
                        result.put("success", true);
                        result.put("wsq", wsqBase64);
                        result.put("serialNumber", serialNumber);
                        result.put("message", "Captura exitosa");
                    } else {
                        result.put("success", false);
                        result.put("message", "Datos WSQ no recibidos");
                    }
                } else {
                    result.put("success", false);
                    result.put("message", resultCode == Activity.RESULT_CANCELED ? 
                        "Captura cancelada" : "Error en el dispositivo");
                }
                
                callbackContext.success(result);
            } catch (Exception e) {
                callbackContext.error("Error procesando resultado: " + e.getMessage());
            }
        }
    }
}
