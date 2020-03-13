/*
 * Created on 11/15/17.
 * Written by Islam Salah with assistance from members of Blink22.com
 */

package android.print;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.aussendiensterfassung.SignOrder;

import java.io.File;
import java.util.ArrayList;

/**
 * Converts HTML to PDF.
 * <p>
 * Can convert only one task at a time, any requests to do more conversions before
 * ending the current task are ignored.
 */
public class PdfConverter implements Runnable {

    private static final String TAG = "PdfConverter";
    private static PdfConverter sInstance;

    private Context mContext;
    private String mHtmlString;
    private File mPdfFile;
    private PrintAttributes mPdfPrintAttrs;
    private boolean mIsCurrentlyConverting;
    private WebView mWebView;
    private Listener mListener;
    private ArrayList<String> mHtmlStrings;
    private ArrayList<File> mFiles;

    private PdfConverter() {
        mHtmlStrings = new ArrayList<>();
        mFiles = new ArrayList<>();
    }

    public static synchronized PdfConverter getInstance() {
        if (sInstance == null)
            sInstance = new PdfConverter();

        return sInstance;
    }

    public interface Listener {
        public void onFinishing();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void run() {
        mWebView = new WebView(mContext);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintDocumentAdapter documentAdapter = mWebView.createPrintDocumentAdapter();
                documentAdapter.onLayout(null, getPdfPrintAttrs(), null, new PrintDocumentAdapter.LayoutResultCallback() {}, null);
                documentAdapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, getOutputFileDescriptor(), null, new PrintDocumentAdapter.WriteResultCallback() {
                    @Override
                    public void onWriteFinished(PageRange[] pages) {
                        mHtmlStrings.remove(0);
                        mFiles.remove(0);
                        destroy();
                    }
                });
            }
        });
        mWebView.loadData(mHtmlString, "text/HTML", "UTF-8");
    }

    public PrintAttributes getPdfPrintAttrs() {
        return mPdfPrintAttrs != null ? mPdfPrintAttrs : getDefaultPrintAttrs();
    }

    public void setPdfPrintAttrs(PrintAttributes printAttrs) {
        this.mPdfPrintAttrs = printAttrs;
    }

    public void convert(Context context, String htmlString, File file) {
        if (context == null)
            throw new IllegalArgumentException("context can't be null");
        if (htmlString == null)
            throw new IllegalArgumentException("htmlString can't be null");
        if (file == null)
            throw new IllegalArgumentException("file can't be null");

        if (mIsCurrentlyConverting)
            return;

        mContext = context;
        mHtmlString = htmlString;
        mPdfFile = file;
        mIsCurrentlyConverting = true;
        runOnUiThread(this);
    }

    public void convertMultiple(Context context, ArrayList<String> htmlStrings, ArrayList<File> files) {
        mHtmlStrings = htmlStrings;
        mFiles = files;
        mContext = context;

        destroy();
    }

    private ParcelFileDescriptor getOutputFileDescriptor() {
        try {
            mPdfFile.createNewFile();
            return ParcelFileDescriptor.open(mPdfFile, ParcelFileDescriptor.MODE_TRUNCATE | ParcelFileDescriptor.MODE_READ_WRITE);
        } catch (Exception e) {
            Log.d(TAG, "Failed to open ParcelFileDescriptor", e);
        }
        return null;
    }

    private PrintAttributes getDefaultPrintAttrs() {
        return new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.NA_GOVT_LETTER)
                .setResolution(new PrintAttributes.Resolution("RESOLUTION_ID", "RESOLUTION_ID", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
    }

    private void runOnUiThread(Runnable runnable) {
        Handler handler = new Handler(mContext.getMainLooper());
        handler.post(runnable);
    }

    private void destroy() {
        mIsCurrentlyConverting = false;
        if (mHtmlStrings.size() > 0) {
            convert(mContext, mHtmlStrings.get(0), mFiles.get(0));
        } else {
            mHtmlString = null;
            mPdfFile = null;
            mPdfPrintAttrs = null;
            mWebView = null;
            mContext = null;
            mListener.onFinishing();
        }
    }
}