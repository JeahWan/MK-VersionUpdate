package com.makise.mk_versionupdate;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import java.io.File;


public class UpdateVersionUtil {

    /**
     * 开启服务下载apk
     *
     * @param context
     * @param appName
     * @param appIcon
     * @param downurl
     * @return
     */
    public static boolean beginToDownload(Context context, String appName, int appIcon, String downurl) {
        //8.0 处理未知应用来源权限问题
        if (Build.VERSION.SDK_INT >= 26 && !context.getPackageManager().canRequestPackageInstalls()) {
            //跳转到设置打开权限
            Toast.makeText(context, "请允许应用安装", Toast.LENGTH_SHORT).show();
            startInstallPermissionSettingActivity(context);
            return false;
        }
        //非8.0 直接调用下载
        Intent updateIntent = new Intent(context, UpdateService.class);
        updateIntent.putExtra("packageName", context.getPackageName());
        updateIntent.putExtra("app_name", appName);
        updateIntent.putExtra("app_icon", appIcon);
        updateIntent.putExtra("downurl", downurl);
        context.startService(updateIntent);
        return true;
    }

    // 下载完成后打开安装apk界面
    public static void installApk(File file, Context context) {
//        LogUtil.info("版本更新获取sd卡的安装包的路径=" + file.getAbsolutePath());
        Intent openFile = getFileIntent(context, file);
        context.startActivity(openFile);
    }

    public static Intent getFileIntent(Context context, File file) {
        Intent intent = new Intent("android.intent.action.VIEW");
        Uri uri = Uri.fromFile(file);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //判读版本是否在7.0以上
            String packageName = context.getApplicationInfo().packageName;
            uri = FileProvider.getUriForFile(context, packageName + ".fileprovider", file);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        String type = getMIMEType(file);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(uri, type);
        return intent;
    }

    private static String getMIMEType(File f) {
        String type;
        String fName = f.getName();
        // 取得扩展名
        String end = fName.substring(fName.lastIndexOf(".") + 1, fName.length());
        if (end.equals("apk")) {
            type = "application/vnd.android.package-archive";
        } else {
            // /*如果无法直接打开，就跳出软件列表给用户选择 */
            type = "*/*";
        }
        return type;
    }

    /**
     * 打开允许安装未知应用的页面
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void startInstallPermissionSettingActivity(Context context) {
        //注意这个是8.0新API
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }
}
