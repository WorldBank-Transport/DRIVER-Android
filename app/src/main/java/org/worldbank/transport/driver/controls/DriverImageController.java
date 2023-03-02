package org.worldbank.transport.driver.controls;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

import com.azavea.androidvalidatedforms.controllers.ImageController;
import com.azavea.androidvalidatedforms.tasks.ResizeImageTask;

import org.jsonschema2pojo.media.SerializableMedia;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * Subclass image controller to get and set path on holder class used for Gson serialization.
 *
 * Created by kathrynkillebrew on 2/4/16.
 */
public class DriverImageController extends ImageController {
    public DriverImageController(Context ctx, String name, String labelText, boolean isRequired) {
        super(ctx, name, labelText, isRequired);
    }

    @Override
    protected Object getModelValue() {
        Object current = super.getModelValue();

        if (current != null && current.getClass().equals(SerializableMedia.class)) {
            return ((SerializableMedia)current).path;
        }

        return null;
    }

    @Override
    protected void setModelValue(String newImagePath) {
        SerializableMedia media = null;

        if (newImagePath != null && !newImagePath.isEmpty()) {
            // Using custom resize Function
            Bitmap bitmap = resizeBitMapImage(newImagePath, 768, 768);
            Bitmap rotatedBitmap = rotateImage(bitmap, newImagePath);
            File rescaledImageFile = storeImage(rotatedBitmap);
            media = new SerializableMedia();
            media.path = rescaledImageFile.getAbsolutePath();
        }

        getModel().setValue(getName(), media);
    }

    private Bitmap rotateImage(Bitmap bitmap, String path) {
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.setScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setRotate(180);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.setRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(-90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(-90);
                    break;
                default:
                    // no rotation needed
                    return bitmap;
            }

            // rotate image
            Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return oriented;

        } catch (IOException e) {
            Log.e("TAG", "Failed to get EXIF information to rotate image");
            e.printStackTrace();
            return bitmap;
        } catch (OutOfMemoryError e) {
            Log.e("TAG", "Ran out of memory rotating image! Need to downscale further?");
            e.printStackTrace();
            return bitmap;
        }
    }


    private Bitmap resizeBitMapImage(String filePath, int targetWidth, int targetHeight) {
        Bitmap outputImage = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        outputImage = BitmapFactory.decodeFile(filePath, options);
        int width = outputImage.getWidth();
        int height = outputImage.getHeight();
        if(width<= targetWidth && height<=targetHeight){
            return outputImage;
        }
        if (width > height) { // LANDSCAPE IMAGE
            Float widthRatio = (float)width/(float)targetWidth;
            targetHeight = (int)(height/widthRatio);
        } else { // PORTRAIT IMAGE
            Float heightRatio = ((float)height/(float)targetHeight);
            targetWidth = (int)(width/heightRatio);
        }
        outputImage = Bitmap.createScaledBitmap(outputImage, targetWidth, targetHeight, true);
        return outputImage;
    }

    private File storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d("TAG",
                    "Error creating media file, check storage permissions: ");// e.getMessage());
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e("TAG", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e("TAG", "Error accessing file: " + e.getMessage());
        }
        return pictureFile;
    }

    /** Create a File for saving an image or video */
    private  File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + this.getContext().getPackageName()
                + "/Files");

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName="MI_"+ timeStamp +".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }
}
