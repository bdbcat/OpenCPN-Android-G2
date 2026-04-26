package org.opencpn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;

import com.caverock.androidsvg.SVG;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class AndroidGraphicsUtil {

    public static String buildSVGIcon(Context context,
                                      String inFile,
                                      String outFile,
                                      int width,
                                      int height) {

        int rwidth = width;
        int rheight = height;

        File file = new File(inFile);
        if (!file.exists()) {
            Log.i("OpenCPN", "FileNotFound: " + inFile);
            return "FileNotFound";
        }

        SVG svg;
        try (FileInputStream inStream = new FileInputStream(inFile)) {
            svg = SVG.getFromInputStream(inStream);
        } catch (Exception e) {
            Log.i("OpenCPN", "SVG Parse Exception", e);
            return "NOK";
        }

        try {
            if (width < 0 || height < 0) {
                rwidth = Math.round(svg.getDocumentWidth());
                rheight = Math.round(svg.getDocumentHeight());
            }

            svg.setDocumentWidth(rwidth);
            svg.setDocumentHeight(rheight);

        } catch (Exception e) {
            Log.i("OpenCPN", "SVG Size Exception", e);
            return "NOK";
        }

        try {
            Bitmap bmp = Bitmap.createBitmap(rwidth, rheight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);

            svg.renderToCanvas(canvas);

            try (FileOutputStream out = new FileOutputStream(outFile)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            }

        } catch (Exception e) {
            Log.i("OpenCPN", "SVG Render Exception", e);
            return "NOK";
        }

        return "OK";
    }
}