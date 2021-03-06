package com.smart.tablet.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.smart.tablet.entities.Media;
import com.smart.tablet.helpers.DatabaseHelper;
import com.smart.tablet.listeners.AsyncResultBag;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class StoreMedia extends AsyncTask<Void, Void, Boolean> {
    private DatabaseHelper _db;
    private AsyncResultBag.Error _errorCallback;
    private AsyncResultBag.Before _beforeCallback;
    private AsyncResultBag.Success _successCallback;
    private Progress _progressCallback;
    private Media[] _media;
    private Object error;
    private String _filepath;
    private int _totalMedia;
    private int _downloaded;
    private int _last_progress;
    private ArrayList<Integer> _files;

    public StoreMedia(Context context, String filepath, Media... media) {
        _db = DatabaseHelper.getInstance(context);
        _media = media;
        _filepath = filepath;
        _totalMedia = media.length;
        _downloaded = 0;
        _last_progress = 0;
        _files = new ArrayList<>();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (_beforeCallback != null)
            _beforeCallback.beforeExecuting();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {

            int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();

            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    NUMBER_OF_CORES * 2,
                    Integer.MAX_VALUE, //NUMBER_OF_CORES * 2
                    Long.MAX_VALUE,//60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>()
            );

            for (int i = 0; i < _media.length; i++) {
                executor.execute(new LongThread(i));
            }

            while (_downloaded < (_totalMedia)) {

                _downloaded = 0;

                for (Media m : _media) {
                    if (m.getPath() != null)
                        _downloaded++;
                }

                //Log.d("StoreMedia", "" + _downloaded + " < " + _totalMedia);
                Thread.sleep(1000);

                if (_downloaded > _last_progress && _progressCallback != null) {
                    _progressCallback.onProgress(100 - (((_totalMedia - _downloaded) * 100) / _totalMedia), _files);
                }
            }

            _db.getAppDatabase().mediaDao().insertAll(_media);

        } catch (Exception e) {
            error = e;
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if (error == null && _successCallback != null)
            _successCallback.onSuccess(result);

        if (error != null && _errorCallback != null)
            _errorCallback.onError(error);
    }

    public com.smart.tablet.tasks.StoreMedia onError(AsyncResultBag.Error callback) {
        _errorCallback = callback;
        return this;
    }

    public com.smart.tablet.tasks.StoreMedia onProgress(Progress callback) {
        _progressCallback = callback;
        return this;
    }

    public com.smart.tablet.tasks.StoreMedia beforeExecuting(AsyncResultBag.Before callback) {
        _beforeCallback = callback;
        return this;
    }

    public com.smart.tablet.tasks.StoreMedia onSuccess(AsyncResultBag.Success callback) {
        _successCallback = callback;
        return this;
    }

    public class LongThread implements Runnable {
        private int index;

        public LongThread(int index) {
            this.index = index;
        }

        @Override
        public void run() {

            if (_media[index] != null) {

                String path = null;
                //Log.d("StoreMedia", "downloading: " + _media[index].getUrl());

                try {
                    path = downloadImage(_media[index].getUrl());
                } catch (IOException | OutOfMemoryError e) {
                    path = "";

                    //e.printStackTrace();
                }

                _files.add(_media[index].getId());
                _media[index].setPath(path);

                Log.d("StoreMedia", "downloaded : " + _media[index].getPath());

            } else {
                Log.d("StoreMedia", "media null");
            }
        }

        private String downloadImage(String object_url) throws IOException, OutOfMemoryError {
            URL url = new URL(object_url);

            InputStream in = url.openStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String filename = _filepath + url.getFile();

            byte[] buf = new byte[4096];
            int n = in.read(buf);

            while (n != -1) {
                out.write(buf, 0, n);
                n = in.read(buf);
            }

            out.close();
            in.close();

            byte[] response = out.toByteArray();

            FileOutputStream fos = new FileOutputStream(filename);

            fos.write(response);
            fos.close();

            File imgFile = new File(_filepath);

            if (imgFile.exists()) {
                //return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            }

            return filename;
        }
    }

    public interface Progress {
        void onProgress(Object result, Object args);
    }
}