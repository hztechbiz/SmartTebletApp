package com.smart.tablet.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.smart.tablet.dao.MediaDao;
import com.smart.tablet.entities.Media;
import com.smart.tablet.helpers.DatabaseHelper;
import com.smart.tablet.listeners.AsyncResultBag;

import java.io.File;
import java.util.List;

public class DeleteMedia extends AsyncTask<Void, Void, Boolean> {
    private DatabaseHelper _db;
    private AsyncResultBag.Error _errorCallback;
    private AsyncResultBag.Before _beforeCallback;
    private AsyncResultBag.Success _successCallback;
    private Media[] _media;
    private Object error;
    private List<Integer> _ids;

    public DeleteMedia(Context context) {
        _db = DatabaseHelper.getInstance(context);
    }

    public DeleteMedia(Context context, List<Integer> ids) {
        _db = DatabaseHelper.getInstance(context);
        _ids = ids;
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
            MediaDao mediaDao = _db.getAppDatabase().mediaDao();
            Media[] medias;

            if (_ids != null) {
                int[] ids = new int[_ids.size()];

                for (int i = 0; i < _ids.size(); i++) {
                    ids[i] = _ids.get(i);
                }

                medias = mediaDao.getAll(ids);
            } else {
                medias = mediaDao.getAll();
            }

            for (Media media : medias) {
                if (media != null) {
                    try {
                        File file = new File(media.getPath());
                        file.delete();

                        mediaDao.delete(media);
                    } catch (NullPointerException ex) {
                        // nothing
                    }
                }
            }
        } catch (Exception e) {
            // nothing
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

    public com.smart.tablet.tasks.DeleteMedia onError(AsyncResultBag.Error callback) {
        _errorCallback = callback;
        return this;
    }

    public com.smart.tablet.tasks.DeleteMedia beforeExecuting(AsyncResultBag.Before callback) {
        _beforeCallback = callback;
        return this;
    }

    public com.smart.tablet.tasks.DeleteMedia onSuccess(AsyncResultBag.Success callback) {
        _successCallback = callback;
        return this;
    }
}
