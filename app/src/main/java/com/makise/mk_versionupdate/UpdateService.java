package com.makise.mk_versionupdate;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateService extends Service {
    private static final int DOWN_ERROR = 0;
    private static final int DOWN_OK = 1; // 下载完成
    private static final int PROGRESS = 2;
    private static String packageName;
    private final int notification_id = 0;
    /**
     * 更新UI
     */
    private final Handler handler = new StaticHandler(this);
    /**
     * 创建通知栏
     */
    private RemoteViews contentView;
    private String down_url;
    private int app_icon;
    private String app_name;
    private NotificationManager notificationManager;
    private Notification notification;
    private Intent updateIntent;
    private PendingIntent pendingIntent;
    private File updateFile;
    private NotificationCompat.Builder builder;

    private static File getDiskCacheFile(Context context) {
        if ((ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            String cachePath = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ? getExternalCacheDir(context).getPath() : context
                    .getCacheDir().getPath();
            File file = new File(cachePath);
            if (!file.exists()) {
                file.mkdirs();
            }
            return new File(cachePath + File.separator + "temp.apk");
        } else {
            Toast.makeText(context, "请到系统设置打开存储权限", Toast.LENGTH_SHORT).show();
            return null;
        }

    }

    private static File getExternalCacheDir(Context context) {
        if (context == null) {
            return new File(Environment.getExternalStorageDirectory().getPath() + "/Android/data/"
                    + packageName + "/cache/");
        }
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            try {
                packageName = intent.getStringExtra("packageName");
                app_name = intent.getStringExtra("app_name");
                app_icon = intent.getIntExtra("app_icon", 0);
                if (app_icon == 0) {
                    app_icon = R.mipmap.ic_vu_default;
                }
                down_url = intent.getStringExtra("downurl");
                // 创建文件
                updateFile = getDiskCacheFile(getApplicationContext());
                if (updateFile != null) {
                    if (!updateFile.exists()) {
                        updateFile.createNewFile();
                    }
                }
                // 创建通知
                createNotification();
                // 开始下载
                downloadUpdateFile();
            } catch (Exception e) {
                Log.e("update", e.toString());
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressWarnings("deprecation")
    private void createNotification() {

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //适配安卓8.0的消息渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(app_name, app_name,NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // 创建一个Notification并设置相关属性
        builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(app_icon)//设置通知的小图标
                .setContentTitle(app_name)
                //兼容8.0 设置channelId
                .setChannelId(app_name)
                .setContentText("开始下载");//设置通知的内容

        // 创建一个开启安装App界面的意图
        updateIntent = new Intent();
        updateIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        updateIntent.setAction("com.makise.mk_versionupdate.action");
        updateIntent.setPackage(getPackageName());
        //创建PendingIntent，用于点击通知栏后实现的意图操作
        pendingIntent = PendingIntent.getActivity(this, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        //在这里我们用自定的view来显示Notification
        contentView = new RemoteViews(getPackageName(), R.layout.notification_item);
        contentView.setImageViewResource(R.id.notificationImage, app_icon);
        contentView.setTextViewText(R.id.notificationTitle, "正在下载");
        contentView.setTextViewText(R.id.notificationPercent, "0%");
        contentView.setProgressBar(R.id.notificationProgress, 100, 0, false);
        builder.setContent(contentView);

        notification = builder.build();
        notificationManager.notify(notification_id, notification);
    }

    /**
     * 下载文件
     */
    private void downloadUpdateFile() {
        OkHttpClient mOkHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(down_url)
                .build();
        mOkHttpClient.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        InputStream is = null;
                        byte[] buf = new byte[2048];
                        int len;
                        FileOutputStream fos = null;
                        try {
                            is = response.body().byteStream();
                            long total = response.body().contentLength();
                            fos = new FileOutputStream(updateFile);
                            long sum = 0;
                            int curProgress = 0;
                            long lastTime = 0;
                            while ((len = is.read(buf)) != -1) {
                                fos.write(buf, 0, len);
                                sum += len;
                                int progress = (int) (sum * 1.0f / total * 100);
                                if (progress > curProgress) {
                                    curProgress = progress;
                                    if (System.currentTimeMillis() - lastTime > 1000) {
                                        lastTime = System.currentTimeMillis();
                                        //设置进度条
                                        Message msg = handler.obtainMessage();
                                        msg.what = PROGRESS;
                                        msg.arg1 = progress;
                                        handler.sendMessage(msg);
                                    }
                                }
                            }
                            fos.flush();
                            // 下载成功
                            Message message = handler.obtainMessage();
                            message.what = DOWN_OK;
                            handler.sendMessage(message);
                            UpdateVersionUtil.installApk(updateFile, UpdateService.this);
                        } catch (Exception e) {
                            Message message = handler.obtainMessage();
                            message.what = DOWN_ERROR;
                            handler.sendMessage(message);
                        } finally {
                            try {
                                if (is != null) {
                                    is.close();
                                }
                                if (fos != null) {
                                    fos.close();
                                }
                            } catch (IOException e) {
                                Log.e("update", e.toString());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call call, IOException arg1) {
                        Message message = handler.obtainMessage();
                        message.what = DOWN_ERROR;
                        handler.sendMessage(message);
                    }
                });
    }

    static class StaticHandler extends Handler {
        private final WeakReference<UpdateService> mService;

        StaticHandler(UpdateService service) {
            mService = new WeakReference(service);
        }

        @Override
        public void handleMessage(final Message msg) {

            UpdateService service = mService.get();
            if (service != null) {
                switch (msg.what) {
                    case DOWN_OK:
                        // 下载完成，点击安装
                        service.builder.setContent(null);
                        service.builder.setContentText("下载成功，点击安装");
                        Intent installApkIntent = UpdateVersionUtil.getFileIntent(service, service.updateFile);
                        service.pendingIntent = PendingIntent.getActivity(service, 0, installApkIntent,
                                0);
                        service.builder.setContentIntent(service.pendingIntent);
                        service.builder.setAutoCancel(true);
                        service.notification = service.builder.build();
                        service.notificationManager.notify(service.notification_id, service.notification);
                        service.stopService(service.updateIntent);
                        break;
                    case DOWN_ERROR:
                        service.builder.setContent(null);
                        service.builder.setContentText("下载失败");
                        service.notification = service.builder.build();
                        service.notification.flags |= Notification.FLAG_AUTO_CANCEL;
                        service.notificationManager.notify(service.notification_id, service.notification);
                        break;
                    case PROGRESS:
                        int progress = msg.arg1;
                        service.contentView.setTextViewText(R.id.notificationPercent, progress + "%");
                        service.contentView.setProgressBar(R.id.notificationProgress, 100, progress, false);
                        service.notificationManager.notify(service.notification_id, service.notification);
                        break;
                    default:
                        service.stopService(service.updateIntent);
                        break;
                }
            }
        }
    }

}