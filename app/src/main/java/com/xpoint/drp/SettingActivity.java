package com.xpoint.drp;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用设置界面
 */
public class SettingActivity extends AppCompatActivity {
    private Switch xSwitch;
    private AlertDialog alertDialog_DrawOverlay, alertDialog_AccessibilityService, alertDialog_UsageAccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        initView();
    }

    public void initView() {
        xSwitch = (Switch) findViewById(R.id.switch0);
        if (isServiceRunning(this, "com.xpoint.drp.PointService")) {
            xSwitch.setChecked(true);
        }
        xSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startService(new Intent(getApplicationContext(), PointService.class));
                } else {
                    stopService(new Intent(getApplicationContext(), PointService.class));
                }
            }
        });
    }

    /**
     * 判断某一个服务是否正在运行
     * ServiceName:包名全路径，例如：com.xpoint.drp.PointService
     */
    public static boolean isServiceRunning(Context context, String ServiceName) {
        if (("").equals(ServiceName) || ServiceName == null)
            return false;
        ActivityManager myManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningServices = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(50);
        for (int i = 0; i < runningServices.size(); i++) {
            if (runningServices.get(i).service.getClassName().toString().equals(ServiceName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkDrawOverlyAndAccessibilityServicePermission();//检查悬浮窗、Usage_Access及AccessibilityService权限
    }

    /**
     * 检查悬浮窗、Usage_Access 及AccessibilityService权限,并引导开启
     */
    public void checkDrawOverlyAndAccessibilityServicePermission() {
        //API >=23，需要在manifest中申请权限，并在每次需要用到权限的时候检查是否已有该权限，因为用户随时可以取消掉。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(getApplicationContext())) {
                showAlertDialogDrawOverly();//引导开启悬浮窗权限对话框
            } else if (!isAccessibilityServiceGranted()) {
                showAlertDialogAccessibilityService();//引导开启无障碍服务对话框
            } else if (!isUsageGranted()) {
                showAlertDialogUsageAccess();//引导开启“查看使用情况”权限对话框
            }
        } else if (!isAccessibilityServiceGranted()) {
            showAlertDialogAccessibilityService();//引导开启无障碍服务对话框
        } else if (!isUsageGranted()) {
            showAlertDialogUsageAccess();//引导开启“查看使用情况”权限对话框
        }
    }


    //判断本应用的无障碍服务是否开启
    public boolean isAccessibilityServiceGranted() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> serviceInfos = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : serviceInfos) {
            String id = info.getId();
            if (id.contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }

    //检查：查看使用情况的权限是否授予
    private boolean isUsageGranted() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = -1;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            mode = appOps.checkOpNoThrow("android:get_usage_stats",
                    android.os.Process.myUid(), getPackageName());
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }


    //弹出：引导开启悬浮窗权限的提示框
    public void showAlertDialogDrawOverly() {
        if (null != alertDialog_DrawOverlay) {
            alertDialog_DrawOverlay.dismiss();
            alertDialog_DrawOverlay = null;
        }

        alertDialog_DrawOverlay = new AlertDialog.Builder(SettingActivity.this)
                .setCancelable(false)
                .setTitle("请求悬浮窗权限").setMessage("请在稍后弹出的界面中，允许XPoint在其他应用的上层显示")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).create();
        alertDialog_DrawOverlay.show();
    }

    //弹出：引导开启无障碍服务的提示框
    public void showAlertDialogAccessibilityService() {
        if (null != alertDialog_AccessibilityService) {
            alertDialog_AccessibilityService.dismiss();
            alertDialog_AccessibilityService = null;
        }
        alertDialog_AccessibilityService = new AlertDialog.Builder(SettingActivity.this)
                .setCancelable(false)
                .setTitle("请求开启辅助服务").setMessage("请在稍后弹出的界面中，开启XPoint悬浮球服务")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    }
                })
                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).create();
        alertDialog_AccessibilityService.show();
    }

    //弹出：引导开启“查看使用情况”权限的提示框
    public void showAlertDialogUsageAccess() {
        if (null != alertDialog_UsageAccess) {
            alertDialog_UsageAccess.dismiss();
            alertDialog_UsageAccess = null;
        }
        alertDialog_UsageAccess = new AlertDialog.Builder(SettingActivity.this)
                .setCancelable(false)
                .setTitle("请求开启查看使用情况权限").setMessage("请在稍后弹出的界面中，授予XPoint该权限")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //开启应用授权界面
                        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    }
                })
                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).create();
        alertDialog_UsageAccess.show();
    }


}
