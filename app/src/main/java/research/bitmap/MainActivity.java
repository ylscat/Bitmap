package research.bitmap;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity implements
        AdapterView.OnItemSelectedListener,
        SeekBar.OnSeekBarChangeListener,
        Runnable {
    private ImageView mImageView;
    private TextView mTextView, mProgress;
    private SeekBar mSeekBar;
    private Bitmap mBitmap;
    private ByteArrayOutputStream mBuffer = new ByteArrayOutputStream();
    private String mBasicInfo;
    private Task mTask;

    private Bitmap.CompressFormat mFormat = Bitmap.CompressFormat.JPEG;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AssetManager am = getAssets();
        int size;
        String type;
        try {
            InputStream is = am.open("fractal.png");

            size = is.available();
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap source = BitmapFactory.decodeStream(is, null, options);
            is.close();
            type = options.outMimeType;

            Bitmap b = Bitmap.createBitmap(source.getWidth(), source.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            c.drawColor(Color.WHITE);
            c.drawBitmap(source, 0, 0, null);
            source.recycle();

            mBitmap = b;
        } catch (IOException e) {
            return;
        }

        mImageView = (ImageView) findViewById(R.id.dst);
        mTextView = (TextView) findViewById(R.id.text);
        SeekBar sb = (SeekBar) findViewById(R.id.seek);
        sb.setOnSeekBarChangeListener(this);
        mSeekBar = sb;
        Spinner sp = (Spinner) findViewById(R.id.spinner);
        sp.setOnItemSelectedListener(this);

        ImageView iv = (ImageView)findViewById(R.id.src);
        iv.setImageBitmap(mBitmap);

        Matrix matrix = new Matrix();
        matrix.setScale(3.5f, 3.5f);
        iv.setImageMatrix(matrix);
        mImageView.setImageMatrix(matrix);


        mProgress = (TextView)findViewById(R.id.progress);
        mBasicInfo = String.format("%s / %s [%s]",
                formatSize(mBitmap.getByteCount()),
                formatSize(size), type);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0:
                mFormat = Bitmap.CompressFormat.JPEG;
                break;
            case 1:
                mFormat = Bitmap.CompressFormat.PNG;
                break;
            case 2:
                mFormat = Bitmap.CompressFormat.WEBP;
                break;
        }
        run();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Rect bound = seekBar.getThumb().getBounds();
        int offset = bound.centerX() - mProgress.getWidth()/2;
        mProgress.setTranslationX(offset);
        mProgress.setText(String.valueOf(progress));
        seekBar.postDelayed(this, 500);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Rect bound = seekBar.getThumb().getBounds();
        int offset = bound.centerX() - mProgress.getWidth()/2;
        mProgress.setTranslationX(offset);
        mProgress.setText(String.valueOf(seekBar.getProgress()));
        mProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mProgress.setVisibility(View.INVISIBLE);
    }

    @Override
    public void run() {
        if(mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED)
            mTask.cancel(true);
        mTask = new Task();
        mTask.execute();
    }

    @SuppressWarnings("WrongThread")
    class Task extends AsyncTask<Object, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Object... params) {
            int quality = mSeekBar.getProgress();
            mBuffer.reset();
            mBitmap.compress(mFormat, quality, mBuffer);
            return BitmapFactory.decodeByteArray(mBuffer.toByteArray(), 0, mBuffer.size());
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mImageView.setImageBitmap(bitmap);
            String info = String.format("\n%s / %s",
                    formatSize(bitmap.getByteCount()),
            formatSize(mBuffer.size()));
            mTextView.setText(mBasicInfo + info);
        }
    }

    private static String formatSize(long size) {
        if(size > 0.8*1024*1024) {
            return String.format("%.2fMB", size/1024f/1024);
        }
        else if(size > 0.8*1024) {
            return String.format("%.2fKB", size/1024f);
        }
        else {
            return size+"B";
        }
    }
}
