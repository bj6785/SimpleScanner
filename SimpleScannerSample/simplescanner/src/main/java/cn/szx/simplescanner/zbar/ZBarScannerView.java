package cn.szx.simplescanner.zbar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import cn.szx.simplescanner.base.BarcodeScannerView;
import cn.szx.simplescanner.base.IViewFinder;

import com.socsi.exception.SDKException;
import com.socsi.smartposapi.terminal.TerminalManager;
import com.verifone.kail1.libyuv.YuvUtil;

/**
 * zbar扫码视图，继承自基本扫码视图BarcodeScannerView
 * <p>
 * BarcodeScannerView内含CameraPreview（相机预览）和ViewFinderView（扫码框、阴影遮罩等）
 */
public class ZBarScannerView extends BarcodeScannerView {
    private static final String TAG = "ZBarScannerView";
    private ImageScanner imageScanner;
    private List<BarcodeFormat> formats;
    private ResultHandler resultHandler;
    private Camera camera;

    public interface ResultHandler {
        void handleResult(Result rawResult);
    }

    /*
     * 加载zbar动态库
     * zbar.jar中的类会用到
     */
    static {
        System.loadLibrary("iconv");
    }

    public ZBarScannerView(@NonNull Context context, @NonNull IViewFinder viewFinderView) {
        super(context, viewFinderView);
        setupScanner();//创建ImageScanner（zbar扫码器）并进行基本设置（如支持的码格式）
    }

    /**
     * 创建ImageScanner并进行基本设置（如支持的码格式）
     */
    public void setupScanner() {
        imageScanner = new ImageScanner();

        imageScanner.setConfig(0, Config.X_DENSITY, 3);
        imageScanner.setConfig(0, Config.Y_DENSITY, 3);

        imageScanner.setConfig(Symbol.NONE, Config.ENABLE, 0);

        for (BarcodeFormat format : getFormats()) {//设置支持的码格式
            imageScanner.setConfig(format.getId(), Config.ENABLE, 1);
        }
    }

    int num = 0;

    /**
     * Called as preview frames are displayed.<br/>
     * This callback is invoked on the event thread open(int) was called from.<br/>
     * (此方法与Camera.open运行于同一线程，在本项目中，就是CameraHandlerThread线程)
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        this.camera = camera;
        if (resultHandler == null) return;
//        long startTime = System.currentTimeMillis();

        try {
            Camera.Parameters parameters = camera.getParameters();
            int previewWidth = parameters.getPreviewSize().width;
            int previewHeight = parameters.getPreviewSize().height;

            Log.d(TAG, "" + previewWidth + " " + previewHeight);

            //根据ViewFinderView和preview的尺寸之比，缩放扫码区域
            Rect rect = getScaledRect(previewWidth, previewHeight);
            Log.d(TAG, "" + rect.left + "---" + rect.top + "---" + rect.width() + "---" + rect.height());

            /*
             * 方案一：旋转图像数据
             */
            //int rotationCount = getRotationCount();//相机图像需要被顺时针旋转几次（每次90度）
            //if (rotationCount == 1 || rotationCount == 3) {//相机图像需要顺时针旋转90度或270度
            //    //交换宽高
            //    int tmp = previewWidth;
            //    previewWidth = previewHeight;
            //    previewHeight = tmp;
            //}
            ////旋转数据
            //data = rotateData(data, camera);

            /*
             * 方案二：旋转截取区域
             */
            rect = getRotatedRect(previewWidth, previewHeight, rect);
            Log.d(TAG, "" + rect.left + "---" + rect.top + "---" + rect.width() + "---" + rect.height());

            //进行yuv数据裁剪的操作
//            final byte[] i420cropData = new byte[rect.width() * rect.height() * 3 / 2];
//            YuvUtil.cropYUV(data, previewWidth, previewHeight, i420cropData, rect.width(), rect.height(), rect.left, rect.top, YuvUtil.rotationModeEnum.ROTATE0.getDegree());

            //宽高都扩大2倍
//            final byte[] i420ScaleData = new byte[rect.width() * rect.height() * 3 * 2];
//            YuvUtil.yuvI420Scale(i420cropData, rect.width(), rect.height(), i420ScaleData, rect.width() * 2, rect.height() * 2, 0);

//            final byte[] n21Data = new byte[rect.width() * rect.height() * 3 * 2];
//            YuvUtil.yuvI420ToNV21(i420ScaleData, n21Data, rect.width() * 2, rect.height() * 2);

            // 转换bitmap
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            YuvImage yuvImage = new YuvImage(n21Data, ImageFormat.NV21, rect.width() * 2, rect.height() * 2, null);
//            yuvImage.compressToJpeg(new Rect(0, 0, rect.width() * 2, rect.height() * 2), 100, out);
//            byte[] imageBytes = out.toByteArray();
//            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            // 保存二维码图片
//            try {
//                String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Qrcode/";
//                File root = new File(rootPath);
//                if (!root.exists()) {
//                    root.mkdirs();
//                }
//                File f = new File(rootPath + "Qrcode" + (num++) + ".jpg");
//                if (f.exists()) {
//                    f.delete();
//                }
//                f.createNewFile();
//
//                FileOutputStream outfile = new FileOutputStream(f);
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outfile);
//                out.flush();
//                out.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            //从preView的图像中截取扫码区域
//            Image barcode = new Image(rect.width() * 2, rect.height() * 2, "NV21");
//            barcode.setData(n21Data);
//            barcode = barcode.convert("Y800");
            Image barcode = new Image(previewWidth, previewHeight, "NV21");
            barcode.setData(data);
            barcode = barcode.convert("Y800");
            barcode.setCrop(rect.left, rect.top, rect.width(), rect.height());

            //使用zbar库识别扫码区域
            int result = imageScanner.scanImage(barcode);

            if (result != 0) {//识别成功
                SymbolSet syms = imageScanner.getResults();
                final Result rawResult = new Result();
                for (Symbol sym : syms) {
                    // In order to retreive QR codes containing null bytes we need to
                    // use getDataBytes() rather than getData() which uses C strings.
                    // Weirdly ZBar transforms all data to UTF-8, even the data returned
                    // by getDataBytes() so we have to decode it as UTF-8.
                    String symData;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        symData = new String(sym.getDataBytes(), StandardCharsets.UTF_8);
                    } else {
                        symData = sym.getData();
                    }
                    if (!TextUtils.isEmpty(symData)) {
                        rawResult.setContents(symData);
                        rawResult.setBarcodeFormat(BarcodeFormat.getFormatById(sym.getType()));
                        break;//识别成功一个就跳出循环
                    }
                }

//                Log.e(TAG, String.format("图像处理及识别耗时: %d ms", System.currentTimeMillis() - startTime));
                try {
                    TerminalManager.getInstance().beep(TerminalManager.BEEP_MODE_SUCCESS);
                } catch (SDKException e) {
                    e.printStackTrace();
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {//切换到主线程
                    @Override
                    public void run() {
                        if (resultHandler != null) {
                            resultHandler.handleResult(rawResult);
                        }
                    }
                });
            } else {//识别失败
                getOneMoreFrame();//再获取一帧图像数据进行识别（会再次触发onPreviewFrame方法）
            }
        } catch (RuntimeException e) {
            Log.e(TAG, e.toString(), e);
        }
    }

//--------------------------------------------------------------------------------------------------

    /**
     * 设置支持的码格式
     */
    public void setFormats(@NonNull List<BarcodeFormat> formats) {
        this.formats = formats;
        setupScanner();
    }

    public Collection<BarcodeFormat> getFormats() {
        if (formats == null) {
            return BarcodeFormat.ALL_FORMATS;
        }
        return formats;
    }

    public void setResultHandler(@NonNull ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
    }

    /**
     * 再获取一帧图像数据进行识别（会再次触发onPreviewFrame方法）
     */
    public void getOneMoreFrame() {
        camera.setOneShotPreviewCallback(this);
    }
}