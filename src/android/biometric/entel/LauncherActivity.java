package biometric.entel;


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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbHost;
import com.zytrust.android.lib.bio.morpho.ui.BioCapture;
import com.zytrust.android.lib.bio.morpho.ui.IBioCapture;
import com.zytrust.android.lib.bio.morpho.ui.ZyRequest;
import com.zytrust.android.lib.bio.morpho.ui.ZyResponse;

import biometric.entel.R;
import biometric.entel.util.Globals;
import biometric.entel.util.Utils;



public class LauncherActivity extends Activity {

    private Button m_morpho;
    private Button m_eikon;
    private Button m_secugen;
    private TextView m_login_version;

    private static final String TAG = "LauncherActivity";
    private static final String OUTPUT_FILE_EXTENSION = ".txt";
    private static final String ACTION_USB_PERMISSION = "com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION";
    private String m_deviceName = "";
    private int eikon_step = 0;
    Reader m_reader;
    private final int CONSMENCONFIGURACION = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_launcher);

        m_morpho = findViewById(R.id.launch_morpho);
        m_eikon = findViewById(R.id.launch_eikon);
        m_secugen = findViewById(R.id.launch_secugen);

        m_login_version = findViewById(R.id.login_version);


        m_login_version.setText(Utils.fnVersion(this));

        m_morpho.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                initializeMorpho();
            }
        });

        m_eikon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initializeEikon();
            }
        });

        m_secugen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initalizeSecugen();
            }
        });
    }

    private void initializeEikon() {

        if (eikon_step == 0) {
            Intent i = new Intent(LauncherActivity.this, GetReaderActivity.class);
            i.putExtra("device_name", m_deviceName);
            i.putExtra("parent_activity", "ScanActionActivity");
            startActivityForResult(i, eikon_step);
        } else if (eikon_step == 1) {
            Intent i = new Intent(LauncherActivity.this, CaptureFingerprintActivity.class);
            i.putExtra("device_name", m_deviceName);
            i.putExtra("instructions", "eikon");
            startActivityForResult(i, eikon_step);
        }

    }

    private void initializeMorpho() {
        IBioCapture iBioCapture = new BioCapture(this, new IBioCapture.ICallback() {
            @Override
            public void onStart() {
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onSuccess(ZyResponse zyResponse) {
                Toast.makeText(getApplicationContext(), "Huella capturada", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onError(ZyResponse obj) {
                //Toast.makeText(getApplicationContext(), "Software versiòn: "+obj.getSoftwareVersion(), Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), obj.getDeError(), Toast.LENGTH_SHORT).show();
                if (obj.getDeError().contains("19005")) {
                    //scan_step = 1;
                    //initializeEikon();

                }
            }
        });
        ZyRequest zyRequest = new ZyRequest();
        iBioCapture.capturar(zyRequest);
    }

    private void initalizeSecugen() {

        Intent intent = new Intent(LauncherActivity.this, JSGDActivity.class);
        intent.putExtra("device_name", m_deviceName);
        intent.putExtra("instructions", "secugen");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            //displayReaderNotFound();
            return;
        }

        Globals.ClearLastBitmap();
        m_deviceName = (String) data.getExtras().get("device_name");
        switch (requestCode) {
            case 0:

                if ((m_deviceName != null) && !m_deviceName.isEmpty()) {
                    //m_selectedDevice.setText("Device: " + m_deviceName);

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
                                m_eikon.setText("Eikon 1");
                                initializeEikon();
                            }
                        }
                    } catch (UareUException e1) {
                        //displayReaderNotFound();
                    } catch (DPFPDDUsbException e) {
                        //displayReaderNotFound();
                    }

                } else {
                    //displayReaderNotFound();
                }

                break;
            case 1:
                Log.i(TAG, "ON RESULT OF SCAN");

                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "RESULT OF SCAN STATUS - OK");

                    String wsqBase64 = data.getStringExtra("wsqBase64");

                    Log.i(TAG, "wsqBase64: " + wsqBase64);

                    try {
                        //saveWSQ(wsqBase64);
                        eikon_step = 0;
                        m_eikon.setText("Eikon");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i(TAG, "RESULT OF SCAN STATUS - FAILED: " + e);
                    }
                } else {
                    Log.i(TAG, "RESULT OF SCAN STATUS - FAILED from capture method");
                }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, CONSMENCONFIGURACION, 0, "Administración").setIcon(R.drawable.ic_my_library_books);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CONSMENCONFIGURACION:
                Toast.makeText(this, Utils.cualBuild(), Toast.LENGTH_SHORT).show();
                break;

            default:
                break;
        }
        return false;
    }
}