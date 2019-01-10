## 版本升级库

	为app提供下载apk，自动弹出安装界面的功能

#### AndroidManifest.xml中配置

> 首页activity增加intent-filter：

	<intent-filter>
		<action android:name="com.makise.mk_versionupdate.action" />
		<category android:name="android.intent.category.DEFAULT" />
	</intent-filter>

#### 使用

> 调用UpdateVersionUtil.beginToDownload(Context context, String appName, String icon, String downurl)方法开始下载APK
> 由于本库依赖okhttp进行apk下载，所以依赖本library的必要前提是项目中加入okhttp的依赖

#### 提示
> appName 可以使用getResources().getString(R.string.app_name)从string.xml文件中拿到

> appIcon为0时，默认显示安卓机器人图标