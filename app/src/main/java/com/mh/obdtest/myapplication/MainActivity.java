package com.mh.obdtest.myapplication;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private DownloadManager mDownloadManager;
    //下载ID
    private long mDownloadId;
    //下载进度弹窗
    private DownloadProgressDialog progressDialog;
    //文件名
    private String mApkName;
    private final QueryRunnable mQueryProgressRunnable = new QueryRunnable();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1001) {
                if (progressDialog != null) {
                    progressDialog.setProgress(msg.arg1);
                    progressDialog.setMax(msg.arg2);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initReceiver();
        //if (isNeedUpdate())
        DownloadApkInfo apkInfo = getDownloadApkInfo();
        //if(apkInfo!=null) {
        startUpDate(apkInfo);
        //}


    }


    //初始化广播接收者
    private void initReceiver() {
        IntentFilter downloadCompleteFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(mDownloadCompleteReceiver, downloadCompleteFilter);
        IntentFilter downloadDetailsFilter = new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        registerReceiver(mDownloadDetailsReceiver, downloadDetailsFilter);
    }

    //是否需要下载更新
    private boolean isNeedUpdate() {
        return true;
    }

    //获取安装包信息
    private DownloadApkInfo getDownloadApkInfo() {
        DownloadApkInfo downloadApkInfo = new DownloadApkInfo();
        downloadApkInfo.setDownloadUrl("http://abc.carloginfo.com/app-release.apk");
        downloadApkInfo.setDescription("修复若干不可描述bug");
        downloadApkInfo.setDownloadSize(1.91f);
        downloadApkInfo.setVersionName("2.02");
        return downloadApkInfo;
    }

    //开始更新
    private void startUpDate(final DownloadApkInfo apkInfo) {
        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
//        TipsUtils.showDialog(this, "发现新版本",
//                apkInfo.getDescription(),
//                new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        startDownloadApk(apkInfo);
//                    }
//                }, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                        stopQuery();
//                        finish();
//                    }
//                }, "立即下载", "取消", false);

        CustomDialog customDialog = new CustomDialog(MainActivity.this);
        customDialog.setTitle("发现新版本");
        customDialog.setMessage("是否更新");

        customDialog.setConfirm("确定", new CustomDialog.IOnConfirmListener() {
            @Override
            public void onConfirm(CustomDialog dialog) {
                //Toast.makeText(LogInActivity.this, "确认成功！",Toast.LENGTH_SHORT).show();
                startDownloadApk(apkInfo);

            }
        });

        customDialog.setCancel("取消", new CustomDialog.IOnCancelListener() {
            @Override
            public void onCancel(CustomDialog dialog) {
                //Toast.makeText(LogInActivity.this, "取消成功！",Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                stopQuery();
                finish();
            }
        });

        customDialog.show();
        //customDialog.setCancelVisable(false);

    }



    //开始下载apk文件
    private void startDownloadApk(DownloadApkInfo apkInfo) {
        mApkName = "DownloadManagerDemo" + "_v" + apkInfo.getVersionName() + ".apk";
        DownloadManager.Request request = new DownloadManager.Request(
                Uri.parse(apkInfo.getDownloadUrl()));
        request.setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_MOBILE
                        | DownloadManager.Request.NETWORK_WIFI)
                .setTitle("DownloadManagerDemo" + "_v" + apkInfo.getVersionName() + ".apk") // 用于信息查看
                .setDescription("新版本升级") // 用于信息查看
                .setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        mApkName);
        try {
            mDownloadId = mDownloadManager.enqueue(request); // 加入下载队列
            startQuery();
        } catch (IllegalArgumentException e) {

            Toast.makeText(this, "更新失败\", \"请在设置中开启下载管理", Toast.LENGTH_SHORT).show();
//            TipsUtils.showDialog(MainActivity.this,
//                    "更新失败", "请在设置中开启下载管理",
//                    new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {

//                        }
//                    }, null, "确定", "", false);


            CustomDialog customDialog = new CustomDialog(MainActivity.this);
            customDialog.setTitle("更新失败");
            customDialog.setMessage("请在设置中开启下载管理");
            customDialog.setCancel("", new CustomDialog.IOnCancelListener() {
                @Override
                public void onCancel(CustomDialog dialog) {
                    //Toast.makeText(LogInActivity.this, "取消成功！",Toast.LENGTH_SHORT).show();
                }
            });
            customDialog.setConfirm("立即下载", new CustomDialog.IOnConfirmListener() {
                @Override
                public void onConfirm(CustomDialog dialog) {
                    //Toast.makeText(LogInActivity.this, "确认成功！",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + "com.android.providers.downloads"));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                    finish();

                }
            });
            customDialog.show();
            customDialog.setCancelVisable(false);

        }
    }

    //更新下载进度
    private void startQuery() {
        if (mDownloadId != 0) {
            displayProgressDialog();
            mHandler.post(mQueryProgressRunnable);
        }
    }

    //查询下载进度
    private class QueryRunnable implements Runnable {
        @Override
        public void run() {
            queryState();
            mHandler.postDelayed(mQueryProgressRunnable, 100);
        }
    }

    //查询下载进度
    private void queryState() {
        // 通过ID向下载管理查询下载情况，返回一个cursor
        Cursor c = mDownloadManager.query(new DownloadManager.Query().setFilterById(mDownloadId));
        if (c == null) {
            Toast.makeText(this, "下载失败", Toast.LENGTH_SHORT).show();
            finish();
        } else { // 以下是从游标中进行信息提取
            if (!c.moveToFirst()) {
                Toast.makeText(this, "下载失败", Toast.LENGTH_SHORT).show();
                finish();
                if (!c.isClosed()) {
                    c.close();
                }
                return;
            }
            int mDownload_so_far = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            int mDownload_all = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            Message msg = Message.obtain();
            if (mDownload_all > 0) {
                msg.what = 1001;
                msg.arg1 = mDownload_so_far;
                msg.arg2 = mDownload_all;
                mHandler.sendMessage(msg);
            }
            if (!c.isClosed()) {
                c.close();
            }
        }
    }

    //停止查询下载进度
    private void stopQuery() {
        mHandler.removeCallbacks(mQueryProgressRunnable);
    }

    //下载停止同时删除下载文件
    private void removeDownload() {
        if (mDownloadManager != null) {
            mDownloadManager.remove(mDownloadId);
        }
    }

    //进度对话框
    private void displayProgressDialog() {
        if (progressDialog == null) {
            // 创建ProgressDialog对象
            progressDialog = new DownloadProgressDialog(this);
            // 设置进度条风格，风格为长形
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            // 设置ProgressDialog 标题
            progressDialog.setTitle("下载提示1");
            // 设置ProgressDialog 提示信息
            progressDialog.setMessage("当前下载进度:");
            // 设置ProgressDialog 的进度条是否不明确
            progressDialog.setIndeterminate(false);
            // 设置ProgressDialog 是否可以按退回按键取消
            progressDialog.setCancelable(false);
            progressDialog.setProgressDrawable(getResources().getDrawable(R.drawable.ic_launcher_background));
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    removeDownload();
                    dialog.dismiss();
                    finish();
                }
            });
        }
        if (!progressDialog.isShowing()) {
            // 让ProgressDialog显示
            progressDialog.show();
        }
    }

    // 下载完成监听，下载完成之后自动安装
    private final BroadcastReceiver mDownloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                // 查询
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor c = mDownloadManager.query(query);
                if (c != null && c.moveToFirst()) {
                    int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                        String uriString = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() +
                                File.separator + mApkName;
                        finish();
                        installApkByGuide(context, intent);
                    }
                }
                if (c != null && !c.isClosed()) {
                    c.close();
                }
            }
        }
    };

    private void installApkByGuide(Context context, Intent intent) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DownloadManagerDemo_v2.02.apk");

        intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        intent.setDataAndType(uri,
                "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    //安装apk
    private void installApkByGuide111(Context context, String uriString) {
        File file = new File(uriString);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //uri = FileProvider.getUriForFile(MainActivity.this, MainActivity.this.getApplication().getPackageName() + ".provider", new File(localFilePath));
            Uri apkUri = FileProvider.getUriForFile(context, "com.mh.obdtest.myapplication.provider", file);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file),
                    "application/vnd.android.package-archive");
        }

        context.startActivity(intent);
    }

    // 通知栏点击事件，点击进入下载详情
    private final BroadcastReceiver mDownloadDetailsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
                lookDownload();
            }
        }
    };

    //进入下载详情
    private void lookDownload() {
        Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDownloadCompleteReceiver != null) {
            unregisterReceiver(mDownloadCompleteReceiver);
        }
        if (mDownloadDetailsReceiver != null) {
            unregisterReceiver(mDownloadDetailsReceiver);
        }
    }
}