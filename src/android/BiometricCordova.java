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

    private static final int CAPTURE_REQUEST = 1;
    private static final int REQUEST_CODE_SCAN_CRYPTO = 1010;
    private CallbackContext callbackContextGlobal;


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (action.equals("captureFingerprint")) {
            String instructions = args.getString(0);
            String rightFinger = args.getString(1);
            String leftFinger = args.getString(2);

            Intent intent = new Intent(cordova.getActivity(), CaptureFingerprintActivity.class);
            intent.putExtra("instructions", instructions);
            intent.putExtra("right_finger", rightFinger);
            intent.putExtra("left_finger", leftFinger);

            cordova.startActivityForResult(this, intent, CAPTURE_REQUEST);
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                String fingerprintData = data.getStringExtra("finger");
                String serialNumber = data.getStringExtra("serialnumber");
                
                JSONObject result = new JSONObject();
                try {
                    result.put("fingerprint", fingerprintData);
                    result.put("serialNumber", serialNumber);
                    callbackContext.success(result);
                } catch (JSONException e) {
                    callbackContext.error("Error al procesar los datos.");
                }
            } else {
                callbackContext.error("Captura cancelada o fallida.");
            }
        }
    }
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("launchMainActivity".equals(action)) {
            launchMainActivity(callbackContext);
            return true;
        }

        if ("launchScanCrypto".equals(action)) {
            this.callbackContextGlobal = callbackContext;
            cordova.setActivityResultCallback(this);

            Intent intent = new Intent(this.cordova.getActivity(), ScanActionCryptoActivity.class);
            cordova.getActivity().startActivityForResult(intent, REQUEST_CODE_SCAN_CRYPTO);
            return true;
        }

        callbackContext.error("Acción no reconocida: " + action);
        return false;
    }

    private void launchMainActivity(CallbackContext callbackContext) {
        try {
            Intent intent = new Intent(this.cordova.getActivity(), MainActivity.class);
            this.cordova.getActivity().startActivity(intent);
            callbackContext.success("MainActivity lanzada correctamente");
        } catch (Exception e) {
            callbackContext.error("Error al lanzar MainActivity: " + e.getMessage());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE_SCAN_CRYPTO) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                JSONObject result = new JSONObject();
                try {
                    result.put("huellab64", intent.getStringExtra("huellab64"));
                    result.put("serialnumber", intent.getStringExtra("serialnumber"));
                    result.put("fingerprint_brand", intent.getStringExtra("fingerprint_brand"));
                    result.put("bioversion", intent.getStringExtra("bioversion"));

                    callbackContextGlobal.success(result);
                } catch (JSONException e) {
                    callbackContextGlobal.error("Error al construir respuesta JSON: " + e.getMessage());
                }
            } else {
                callbackContextGlobal.error("ScanActionCryptoActivity cancelada o sin datos.");
            }
        }
    }
}
