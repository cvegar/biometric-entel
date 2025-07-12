package biometric.entel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.digitalpersona.uareu.Compression;
import com.digitalpersona.uareu.Fid;
import com.digitalpersona.uareu.Quality;
import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.Reader.Priority;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.dpfj.CompressionImpl;
import com.digitalpersona.uareu.jni.DpfjQuality;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import biometric.entel.R;
import biometric.entel.util.Globals;
import biometric.entel.util.Utils;

public class CaptureFingerprintActivity extends Activity implements OnItemSelectedListener {
    private final static String LOG_TAG = "Capture";
    private Button m_back;
    private String m_deviceName = "";
    private String hright = null;
    private String hleft = null;

    private Reader m_reader = null;
    private int m_DPI = 0;
    private Bitmap m_bitmap = null;
    private ImageView m_imgView;
    private TextView m_selectedDevice;
    private TextView m_title;
    private TextView m_login_version;
    private TextView tvFingers;
    private LinearLayout llBestFingers;
    private boolean m_reset = false;
    private String m_text_conclusionString;
    private Reader.CaptureResult cap_result = null;

    private String instructions;

    private Spinner m_spinner = null;
    private HashMap<String, Reader.ImageProcessing> m_imgProcMap = null;

    private boolean bFirstTime = true;
    String finalwsq;
    String serialnumber;

    private void initializeActivity() {
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        m_title = findViewById(R.id.title);
        m_title.setText("Capture");
        m_selectedDevice = findViewById(R.id.selected_device);
        llBestFingers = findViewById(R.id.llBestFingers);
        tvFingers = findViewById(R.id.tvFinger);
        m_deviceName = getIntent().getExtras().getString("device_name");
        instructions = getIntent().getExtras().getString("instructions");
        hright = getIntent().getExtras().getString("right_finger");
        hleft = getIntent().getExtras().getString("left_finger");
        m_login_version = findViewById(R.id.login_version);
        m_login_version.setText(Utils.fnVersion(this));
        m_selectedDevice.setText("Device: " + m_deviceName);

        m_bitmap = Globals.GetLastBitmap();
        if (m_bitmap == null)
            m_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.black);
        m_imgView = findViewById(R.id.bitmap_image);
        m_imgView.setImageBitmap(m_bitmap);
        m_back = findViewById(R.id.back);

        m_back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //onBackPressed ();

                try {

                    if (cap_result != null) {
                        if (cap_result.image == null) {
                            Log.i(LOG_TAG, "Image is null");
                            onBackPressed();
                        } else {
                            Log.i(LOG_TAG, "Returning image");
                            returnByteArray();
                        }
                    } else {
                        Log.i(LOG_TAG, "Capture result is null");
                        Toast.makeText(getApplicationContext(), "No se ha capturado ninguna huella, intenta nuevamente", Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {

                    Toast.makeText(getApplicationContext(), "Try Again", Toast.LENGTH_SHORT).show();

                }

            }
        });

        m_spinner = findViewById(R.id.imgproc);
        m_spinner.setOnItemSelectedListener(this);

        m_imgProcMap = new HashMap<String, Reader.ImageProcessing>();
        m_imgProcMap.put("DEFAULT", Reader.ImageProcessing.IMG_PROC_DEFAULT);
        m_imgProcMap.put("PIV", Reader.ImageProcessing.IMG_PROC_PIV);
        m_imgProcMap.put("ENHANCED", Reader.ImageProcessing.IMG_PROC_ENHANCED);
        m_imgProcMap.put("ENHANCED_2", Reader.ImageProcessing.IMG_PROC_ENHANCED_2);

        Globals.DefaultImageProcessing = Reader.ImageProcessing.IMG_PROC_DEFAULT;

        if (hleft != null && hright != null) {
            llBestFingers.setVisibility(View.VISIBLE);
            String fingers = Utils.getFingerName(hleft) + " o " + Utils.getFingerName(hright);
            tvFingers.setText(fingers);
        }
    }

    private void populateSpinner() {
        if (m_reader != null) {

            /*

                VID			PID			DEVICE			IMAGE PROCESSING
                ---			----		------			----------------
                0x05ba		0x000a		4500			DEFAULT
                0x05ba		0x000b		51XX,4500UID	DEFAULT, PIV, ENHANCED
                0x05ba		0x000c		5301			DEFAULT
                0x05ba		0x000d		5200			DEFAULT, PIV, ENHANCED, ENHANCED2
                0x05ba		0x000e		5300			DEFAULT, PIV, ENHANCED, ENHANCED2
                0x080b		0x010b		Drax30			DEFAULT, PIV, ENHANCED
                0x080b		0x0109		Pocket30		DEFAULT, PIV, ENHANCED
                0x05ba		0x7340		Pocket30-prot	DEFAULT, PIV, ENHANCED
             */

            List<String> options = new ArrayList<String>();

            int vid = m_reader.GetDescription().id.vendor_id;
            int pid = m_reader.GetDescription().id.product_id;

            if (vid == 0x05ba && pid == 0x000a) {
                // device: 4500
                options.add("DEFAULT");
            } else if (vid == 0x05ba && pid == 0x000b) {
                // device: 51XX,4500UID
                options.add("DEFAULT");
                options.add("PIV");
                options.add("ENHANCED");
            } else if (vid == 0x05ba && pid == 0x000c) {
                // device: 5301
                options.add("DEFAULT");
            } else if (vid == 0x05ba && pid == 0x000d) {
                // device: 5200
                options.add("DEFAULT");
                options.add("PIV");
                options.add("ENHANCED");
                options.add("ENHANCED_2");
            } else if (vid == 0x05ba && pid == 0x000e) {
                // device: 5300
                options.add("DEFAULT");
                options.add("PIV");
                options.add("ENHANCED");
                options.add("ENHANCED_2");
            } else if ((vid == 0x080b && (pid == 0x010b || pid == 0x0109)) || (vid == 0x05ba && pid == 0x7340)) {
                // device: Drax30 or Pocket30
                options.add("DEFAULT");
                options.add("PIV");
                options.add("ENHANCED");
            } else {
                options.add("DEFAULT");
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
            m_spinner.setAdapter(adapter);
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)

        // get the current selection from the spinner
        String selection = (String) parent.getItemAtPosition(pos);

        // lookup the image processing mode and set
        Globals.DefaultImageProcessing = m_imgProcMap.get(selection);

        try {
            // abort the current capture (if any)
            if (!bFirstTime) {
                m_reader.CancelCapture();
            }
            bFirstTime = false;
        } catch (UareUException ex) {
            // ignore all exceptions
        }

        // show a Toast to notify the user of the switch
        Toast.makeText(parent.getContext(), "OnItemSelectedListener : " + parent.getItemAtPosition(pos).toString(), Toast.LENGTH_SHORT).show();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_stream);

        initializeActivity();

        // initialize dp sdk
        try {
            Context applContext = getApplicationContext();
            m_reader = Globals.getInstance().getReader(m_deviceName, applContext);
            m_reader.Open(Priority.EXCLUSIVE);
            m_DPI = Globals.GetFirstDPI(m_reader);

            serialnumber = new String("N/A");

//check PTAPI GUID
            byte[] guid = m_reader.GetParameter(Reader.ParamId.PARMID_PTAPI_GET_GUID);

            if (16 == guid.length) {
                final char[] hexArray = "0123456789ABCDEF".toCharArray();
                char[] hexChars = new char[guid.length * 2];

                for (int j = 0; j < guid.length; j++) {
                    int v = guid[j] & 0xFF;
                    hexChars[j * 2] = hexArray[v >>> 4];
                    hexChars[j * 2 + 1] = hexArray[v & 0x0F];
                }

                serialnumber = new String(hexChars);

            }


            //Toast.makeText(this, "Serial number: " + serialnumber, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.w("UareUSampleJava", "error during init of reader");
            m_deviceName = "";
            onBackPressed();
            return;
        }

        // populate the spinner widget
        populateSpinner();

        // loop capture on a separate thread to avoid freezing the UI
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    m_reset = false;
                    while (!m_reset) {
                        // capture the image
                        cap_result = m_reader.Capture(Fid.Format.ANSI_381_2004, Globals.DefaultImageProcessing, m_DPI, -1);

                        // an error occurred
                        if (cap_result == null) continue;

                        if (cap_result.image != null) {
                            // save bitmap image locally
                            m_bitmap = Globals.GetBitmapFromRaw(cap_result.image.getViews()[0].getImageData(), cap_result.image.getViews()[0].getWidth(), cap_result.image.getViews()[0].getHeight());

                            // calculate nfiq score
                            DpfjQuality quality = new DpfjQuality();
                            int nfiqScore = quality.nfiq_raw(cap_result.image.getViews()[0].getImageData(),    // raw image data
                                    cap_result.image.getViews()[0].getWidth(),        // image width
                                    cap_result.image.getViews()[0].getHeight(),        // image height
                                    m_DPI,                                            // device DPI
                                    cap_result.image.getBpp(),                        // image bpp
                                    Quality.QualityAlgorithm.QUALITY_NFIQ_NIST        // qual. algo.
                            );

                            // log NFIQ score
                            Log.i("UareUSampleJava", "capture result nfiq score: " + nfiqScore);

                            // update ui string
                            m_text_conclusionString = Globals.QualityToString(cap_result);
                            m_text_conclusionString += " (NFIQ score: " + nfiqScore + ")";

                            //Returns first image captured-
                            //returnByteArray();
                        } else {
                            m_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.black);
                            // update ui string
                            m_text_conclusionString = Globals.QualityToString(cap_result);
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                UpdateGUI();
                            }
                        });
                    }
                } catch (Exception e) {
                    if (!m_reset) {
                        Log.w("UareUSampleJava", "error during capture: " + e);
                        m_deviceName = "";
                        onBackPressed();
                    }
                }
            }
        }).start();
    }

    private void returnByteArray() {

        //WSQ IN B64
        //grab capture result and convert it into wsq as bytearray

        Fid ISOFid = cap_result.image;
        byte[] wsqRawCompress = processImage(ISOFid.getViews()[0].getData(), ISOFid.getViews()[0].getWidth(), ISOFid.getViews()[0].getHeight());
        String wsqBase64 = Utils.formatWsqToBase64(wsqRawCompress);

        Intent i = new Intent();
        i.putExtra("m_deviceName", m_deviceName);
        if (wsqBase64 != null) {

            Log.i(LOG_TAG, "wsqBase64: " + wsqBase64);
            //i.putExtra("wsqBase64", wsqBase64);

            try {
                m_reader.Close();
            } catch (UareUException e) {
                e.printStackTrace();
            }

            try {

                if (instructions.isEmpty() || instructions == "" || instructions == null) {

                    Log.i(LOG_TAG, "Used from activity_secugen - Not Saving Image");
                    Toast.makeText(getApplicationContext(), "Correcto", Toast.LENGTH_SHORT).show();
                    i.putExtra("finger", finalwsq);
                    setResult(Activity.RESULT_OK, i);
                    finish();

                } else {
                    finalwsq = wsqBase64;
                    Toast.makeText(getApplicationContext(), "Huella capturada", Toast.LENGTH_SHORT).show();

                    onBackPressed();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Ocurrió un error capturando la huella");
                Toast.makeText(getApplicationContext(), "Ocurrió un error capturando la huella", Toast.LENGTH_SHORT).show();
                i.putExtra("finger", finalwsq);
                setResult(Activity.RESULT_CANCELED, i);
                finish();
            }
        } else {

            Log.i(LOG_TAG, "Ocurrió un error al transformar la huella");
            i.putExtra("finger", finalwsq);
            setResult(Activity.RESULT_CANCELED, i);
            finish();

        }
    }

    public byte[] processImage(byte[] img, int width, int height) {

        Bitmap bmWSQ = null;
        bmWSQ = getBitmapAlpha8FromRaw(img, width, height);

        byte[] arrayT = null;

        Bitmap redimWSQ = overlay(bmWSQ);
        int numOfbytes = redimWSQ.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(numOfbytes);
        redimWSQ.copyPixelsToBuffer(buffer);
        arrayT = buffer.array();

        int v1 = 1;
        for (int i = 0; i < arrayT.length; i++) {
            if (i < 40448) { // 79
                arrayT[i] = (byte) 255;
            } else if (i >= 40448 && i <= 221696) {

                if (v1 < 132) {
                    arrayT[i] = (byte) 255;
                } else if (v1 > 382) {
                    arrayT[i] = (byte) 255;
                }
                if (v1 == 512) {
                    v1 = 0;
                }
                v1++;
            } else if (i > 221696) { // 433
                arrayT[i] = (byte) 255;
            }

        }

        CompressionImpl comp = new CompressionImpl();
        try {
            comp.Start();
            comp.SetWsqBitrate(200, 1);

            byte[] rawCompress = comp.CompressRaw(arrayT, 512, 512, 500, 8, Compression.CompressionAlgorithm.COMPRESSION_WSQ_NIST);

            comp.Finish();
            Log.i("Util", "getting WSQ...");
            return rawCompress;

        } catch (UareUException e) {
            Log.e("Util", "UareUException..." + e);
            return null;
        } catch (Exception e) {
            Log.e("Util", "Exception..." + e);
            return null;
        }


    }

    private Bitmap overlay(Bitmap bmp) {
        Bitmap bmOverlay = Bitmap.createBitmap(512, 512, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp, 512 / 2 - bmp.getWidth() / 2, 512 / 2 - bmp.getHeight() / 2, null);
        canvas.save();
        return bmOverlay;
    }

    private Bitmap getBitmapAlpha8FromRaw(byte[] Src, int width, int height) {
        byte[] Bits = new byte[Src.length];
        int i = 0;
        for (i = 0; i < Src.length; i++) {
            Bits[i] = Src[i];
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));

        return bitmap;
    }


    public void UpdateGUI() {
        m_imgView.setImageBitmap(m_bitmap);
        m_imgView.invalidate();
        //m_text_conclusion.setText(m_text_conclusionString);
        Log.i("CaptureFingerprintAct", m_text_conclusionString);
    }

    @Override
    public void onBackPressed() {
        try {
            m_reset = true;
            try {
                m_reader.CancelCapture();
            } catch (Exception e) {

            }
            m_reader.Close();

            Intent i = new Intent();
            i.putExtra("device_name", m_deviceName);
            i.putExtra("finger", finalwsq);
            i.putExtra("serialnumber", serialnumber);
            setResult(Activity.RESULT_OK, i);
            finish();

        } catch (Exception e) {
            Log.w("UareUSampleJava", "error during reader shutdown");
            //Utils.saveErrorInStorage("Error during reader shutdown");

            Intent i = new Intent();
            i.putExtra("device_name", m_deviceName);
            i.putExtra("finger", finalwsq);
            i.putExtra("serialnumber", serialnumber);
            setResult(Activity.RESULT_OK, i);
            finish();
        }

    }

    // called when orientation has changed to manually destroy and recreate activity
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_capture_stream);
        initializeActivity();
        populateSpinner();
    }
}
