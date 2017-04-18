package job.com.searchnearbyplaces;

import android.app.Application;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import job.com.searchnearbyplaces.model.Result;

/**
 * Created by deepak on 4/18/2017.
 */

public class MyApplication extends Application {

    private static MyApplication mContext;
    public List<Result> resultList;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        resultList = new ArrayList<>();
    }

    public static MyApplication getApp(){
        return mContext;
    }
}
