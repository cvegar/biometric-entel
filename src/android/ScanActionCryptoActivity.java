package biometric.entel;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbHost;
import com.rsa.CryptoUtil;
import com.zytrust.android.lib.bio.morpho.ui.BioCapture;
import com.zytrust.android.lib.bio.morpho.ui.IBioCapture;
import com.zytrust.android.lib.bio.morpho.ui.ZyRequest;
import com.zytrust.android.lib.bio.morpho.ui.ZyResponse;

import SecuGen.FDxSDKPro.JSGFPLib;
import SecuGen.FDxSDKPro.SGFDxDeviceName;
import SecuGen.FDxSDKPro.SGFDxErrorCode;
//import biometric.entel.R;
import biometric.entel.util.Globals;
import biometric.entel.util.Utils;
import com.outsystemsenterprise.entel.PEMayorista.R;

public class ScanActionCryptoActivity extends Activity {

    private String instructions;

    private static final String TAG = "ScanActionActivity";

    private static final String ACTION_USB_PERMISSION = "com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION";

    private String m_deviceName = "";
    private int eikon_step = 0;
    private JSGFPLib sgfplib;
    private String fingerprintBrand;
    private String hright = null;
    private String hleft = null;
    private Reader m_reader;

    private String bioversion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sgfplib = new JSGFPLib((UsbManager) getSystemService(Context.USB_SERVICE));
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_scan);

        Intent intent = getIntent();
        instructions = intent.getStringExtra("file");

        instructions = instructions.substring(2, instructions.length() - 2);

        fingerprintBrand = null;
        bioversion=Utils.fnVersion(this);

        if (!getIntent().getBooleanExtra("op", false)) {
            //called from oustystems from callactivity
            hright = getIntent().getExtras().getString("hright").replace("[", "")
                    .replace("]", "").replace("\"", "");
            hleft = getIntent().getExtras().getString("hleft").replace("[", "")
                    .replace("]", "").replace("\"", "");

            Log.d(TAG, "ded: " + hright + hleft);
        }

        Log.v("XXX", instructions);
        /*
        long error = sgfplib.Init(SGFDxDeviceName.SG_DEV_AUTO);

        if (error == SGFDxErrorCode.SGFDX_ERROR_NONE) {
            initalizeSecugen();
        } else {
            initializeMorpho();
        }
        */
        initializeEikon();
    }


    private void initializeMorpho() {
        fingerprintBrand = "Morpho";
        Toast toast = Toast.makeText(ScanActionCryptoActivity.this, "Ingrese el dedo indicado o el " + Utils.getFingerName(hleft), Toast.LENGTH_SHORT);

        IBioCapture iBioCapture = new BioCapture(this, new IBioCapture.ICallback() {
            @Override
            public void onStart() {
                if (hleft != null) {
                    toast.show();
                }
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onSuccess(ZyResponse zyResponse) {
                toast.cancel();
                if (zyResponse.getWsq() != null) {
                    //Toast.makeText(getApplicationContext(), "Software versiòn: "+zyResponse.getSoftwareVersion(), Toast.LENGTH_SHORT).show();
                    Toast.makeText(getApplicationContext(), "Huella capturada", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    CryptoUtil.loadKeys();
                    String encriptedBase64 = null;
                    try {
                        encriptedBase64 = CryptoUtil.encrypt_(Utils.formatWsqToBase64(zyResponse.getWsq()));
                        System.out.println("Encrypted Data: " + encriptedBase64);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    intent.putExtra("huellab64", encriptedBase64);
                    intent.putExtra("serialnumber", zyResponse.getSoftwareVersion());
                    intent.putExtra("fingerprint_brand", fingerprintBrand);
                    intent.putExtra("bioversion", bioversion);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            }

            @Override
            public void onError(ZyResponse obj) {
                toast.cancel();
                if (obj.getDeError().contains("19005")) {
                    Toast.makeText(getApplicationContext(), "Using digital", Toast.LENGTH_SHORT).show();
                } else if (obj.getDeError().contains("-1000")) {
                    Toast.makeText(getApplicationContext(), "Intentando con huellero Eikon", Toast.LENGTH_SHORT).show();
                    initializeEikon();
                } else {
                    Toast.makeText(getApplicationContext(), obj.getDeError(), Toast.LENGTH_SHORT).show();
                    initializeEikon();
                    //finish();
                }
            }
        });

        ZyRequest zyRequest = new ZyRequest();
        zyRequest.setIdDedo(hright);
        zyRequest.setTimeout(30);
        iBioCapture.capturar(zyRequest);
    }

    private void initializeEikon() {
        fingerprintBrand = "Eikon";

        if (eikon_step == 0) {
            Intent i = new Intent(ScanActionCryptoActivity.this, GetReaderActivity.class);
            i.putExtra("device_name", m_deviceName);
            i.putExtra("parent_activity", "ScanActionActivity");
            startActivityForResult(i, eikon_step);
        } else if (eikon_step == 1) {
            Intent i = new Intent(ScanActionCryptoActivity.this, CaptureFingerprintActivity.class);
            i.putExtra("device_name", m_deviceName);
            i.putExtra("instructions", instructions);
            i.putExtra("right_finger", hright);
            i.putExtra("left_finger", hleft);
            startActivityForResult(i, eikon_step);
        }
    }

    private void initalizeSecugen() {
        fingerprintBrand = "Secugen";

        Intent intent = new Intent(ScanActionCryptoActivity.this, JSGDActivity.class);
        intent.putExtra("device_name", m_deviceName);
        intent.putExtra("instructions", instructions);
        intent.putExtra("right_finger", hright);
        intent.putExtra("left_finger", hleft);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            Toast.makeText(getApplicationContext(), "No data on activity result", Toast.LENGTH_SHORT).show();
            return;
        }

        Globals.ClearLastBitmap();
        m_deviceName = (String) data.getExtras().get("device_name");


        switch (requestCode) {
            case 0:

                if ((m_deviceName != null) && !m_deviceName.isEmpty()) {
                    try {
                        Context applContext = getApplicationContext();
                        m_reader = Globals.getInstance().getReader(m_deviceName, applContext);

                        {
                            PendingIntent mPermissionIntent;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
                            } else {
                                mPermissionIntent = PendingIntent.getBroadcast(applContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
                            }
                            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                            applContext.registerReceiver(mUsbReceiver, filter);

                            if (DPFPDDUsbHost.DPFPDDUsbCheckAndRequestPermissions(applContext, mPermissionIntent, m_deviceName)) {
                                //CheckDevice();
                                eikon_step = 1;
                                initializeEikon();
                            }
                        }
                    } catch (UareUException e1) {
                        Toast.makeText(getApplicationContext(), e1.toString(), Toast.LENGTH_SHORT).show();
                    } catch (DPFPDDUsbException e) {
                        Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "El lector no ha sido detectado o no se ha otorgado los permisos USB,conectar el lector e intentar la operación nuevamente.", Toast.LENGTH_SHORT).show();
                    finish();
                }

                break;
            case 1:
                Log.i(TAG, "ON RESULT OF SCAN");

                Intent intent = new Intent();

                CryptoUtil.loadKeys();
                String encriptedBase64;
                try {

                    encriptedBase64 = CryptoUtil.encrypt_(data.getStringExtra("finger"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                intent.putExtra("huellab64", encriptedBase64);
                intent.putExtra("serialnumber", data.getStringExtra("serialnumber"));
                intent.putExtra("fingerprint_brand", fingerprintBrand);
                intent.putExtra("bioversion", bioversion);
                setResult(Activity.RESULT_OK, intent);
                finish();
                break;
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                            //CheckDevice();
                        }
                    } else {
                        //setButtonsEnabled(false);
                    }
                }
            }
        }
    };
}
