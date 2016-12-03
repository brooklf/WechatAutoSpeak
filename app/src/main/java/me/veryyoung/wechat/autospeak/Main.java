package me.veryyoung.wechat.autospeak;

import android.database.Cursor;
import android.text.TextUtils;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;


public class Main implements IXposedHookLoadPackage {

    public static final String WECHAT_PACKAGE_NAME = "com.tencent.mm";
    private static final String NOTIFICATION_CLASS_NAME = WECHAT_PACKAGE_NAME + ".booter.notification.b";
    private static final String STORAGE_CLASS_NAME = WECHAT_PACKAGE_NAME + ".storage.j";
    private static final String STORAGE_METHOD_NAME = WECHAT_PACKAGE_NAME + ".bh.g";
    private static final String IMAGE_CLASS_NAME = WECHAT_PACKAGE_NAME + ".ag.n";
    private static final String IMAGE_METHOD_NAME1 = "Gj";
    private static final String IMAGE_METHOD_NAME2 = "iJ";

    private WechatMainDBHelper mDb;

    private Class<?> mImgClss;


    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) {

        if (lpparam.packageName.equals(WECHAT_PACKAGE_NAME)) {
            findAndHookMethod(NOTIFICATION_CLASS_NAME, lpparam.classLoader, "a", NOTIFICATION_CLASS_NAME, String.class, String.class, int.class, int.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                            int messageType = (int) param.args[3];
                            String content = (String) param.args[2];

                            // maybe it can be used
                            // String sender = (String) param.args[1];
                            switch (messageType) {
                                case 1:
                                    log("文字消息：" + content);
                                    break;
                                case 3:
                                    // 图片消息
                                    String imagePath = getImagePath();
                                    log("图片消息:" + imagePath);
                                    break;
                                case 47:
                                    // 表情；
                                    String expressionUrl = getExpressionUrl(content);
                                    log("表情消息:" + expressionUrl);
                                    break;
                                default:
                                    //do nothing;
                                    return;
                            }

                        }
                    }

            );


            findAndHookConstructor(STORAGE_CLASS_NAME, lpparam.classLoader, STORAGE_METHOD_NAME, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (null == mDb) {
                        mDb = new WechatMainDBHelper(param.args[0]);
                    }
                    if (null == mImgClss) {
                        mImgClss = findClass(IMAGE_CLASS_NAME, lpparam.classLoader);
                    }
                }
            });


        }


    }


    private String getImagePath() {
        Cursor cursor = mDb.getLastMsg();
        if (cursor == null || !cursor.moveToFirst()) {
            return null;
        }
        String imgPath = cursor.getString(cursor.getColumnIndex("imgPath"));
        String locationInSDCard = (String) callMethod(callStaticMethod(mImgClss, IMAGE_METHOD_NAME1), IMAGE_METHOD_NAME2, imgPath);
        if (TextUtils.isEmpty(locationInSDCard)) {
            return null;
        }
        return locationInSDCard + ".jpg";
    }

    private String getExpressionUrl(String content) {
        return "http:" + content.substring(content.indexOf("//")).substring(0, content.indexOf("\""));
    }


}