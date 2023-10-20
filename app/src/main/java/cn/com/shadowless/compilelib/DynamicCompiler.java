package cn.com.shadowless.compilelib;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;

import com.rxjava.rxlife.RxLife;

import org.codehaus.janino.SimpleCompiler;
import org.codehaus.janino.util.ClassFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 动态编译
 *
 * @author sHadowLess
 */
public class DynamicCompiler {

    private final String TAG = DynamicCompiler.class.getSimpleName();

    private final Context context;

    private final String dexFilePath;

    private final String fileName;

    private final String classFileName;

    private final String dexFileName;

    private final String absoluteClsName;

    private final ResultCallBack callBack;

    private final SimpleCompiler compiler;

    private final String cachePath;

    private final String opDexCachePath;

    private final boolean isGenerateCompileInfo;

    private final boolean isMergeDex;

    private final boolean isUpdate;

    private final LifecycleOwner owner;

    private DexClassLoader mergerClassLoader;

    /**
     * Instantiates a new Dynamic compiler.
     *
     * @param context               the context
     * @param dexFilePath           the dex file path
     * @param fileName              the file name
     * @param classFileName         the class file name
     * @param dexFileName           the dex file name
     * @param absoluteClsName       the absolute cls name
     * @param isMergeDex            the is merge dex
     * @param isGenerateCompileInfo the is generate compile info
     * @param owner                 the owner
     * @param callBack              the call back
     */
    public DynamicCompiler(Context context, String dexFilePath, String fileName, String classFileName, String dexFileName, String absoluteClsName, boolean isMergeDex, boolean isUpdate, boolean isGenerateCompileInfo, LifecycleOwner owner, ResultCallBack callBack) {
        this.context = context;
        this.dexFilePath = dexFilePath;
        this.fileName = fileName;
        this.classFileName = classFileName;
        this.dexFileName = dexFileName;
        this.absoluteClsName = absoluteClsName;
        this.callBack = callBack;
        this.owner = owner;
        this.isMergeDex = isMergeDex;
        this.isUpdate = isUpdate;
        this.compiler = new SimpleCompiler();
        this.cachePath = context.getExternalFilesDir(null).getAbsolutePath();
        this.opDexCachePath = context.getDir("opDex", Context.MODE_PRIVATE).getAbsolutePath();
        this.isGenerateCompileInfo = isGenerateCompileInfo;
        if (isGenerateCompileInfo) {
            compiler.setDebuggingInformation(true, true, true);
            compiler.setCompileErrorHandler((s, location) -> {
                String builder = "错误信息：" + s + "\n" + "错误文件名：" + location.getFileName() + "\n" + "错误行：" + "第" + location.getLineNumber() + "行" + "\n" + "错误列：" + "第" + location.getColumnNumber() + "列";
                printCompileInfo(callBack, Statue.COMPILE_JAVA_ERROR, 2, builder.toString());
            });
            compiler.setWarningHandler((s, s1, location) -> {
                String builder = "警告信息：" + s1 + "\n" + "文件名：" + location.getFileName() + "\n" + "警告行：" + "第" + location.getLineNumber() + "行" + "\n" + "警告列：" + "第" + location.getColumnNumber() + "列";
                printCompileInfo(callBack, Statue.COMPILE_JAVA_WARNING, 1, builder.toString());
            });
        } else {
            compiler.setDebuggingInformation(false, false, false);
        }
    }

    /**
     * 构造者
     *
     * @return the net utils . net utils builder
     */
    public static DynamicCompilerBuilder builder() {
        return new DynamicCompilerBuilder();
    }

    /**
     * The type Dynamic compiler builder.
     */
    public static class DynamicCompilerBuilder {

        private Context context;

        private String dexFilePath;

        private String fileName;

        private String classFileName;

        private String dexFileName;

        private String absoluteClsName;

        private boolean isMergeDex;

        private boolean isUpdate;

        private boolean isGenerateCompileInfo;

        private LifecycleOwner owner;

        private ResultCallBack callBack;

        /**
         * Context dynamic compiler builder.
         *
         * @param context the context
         * @return the dynamic compiler builder
         */
        public DynamicCompilerBuilder context(Context context) {
            this.context = context;
            return this;
        }

        /**
         * Lifecycle dynamic compiler builder.
         *
         * @param owner the owner
         * @return the dynamic compiler builder
         */
        public DynamicCompilerBuilder lifecycle(LifecycleOwner owner) {
            this.owner = owner;
            return this;
        }


        /**
         * File name dynamic compiler builder.
         *
         * @param fileName the file name
         * @return the dynamic compiler builder
         */
        public DynamicCompilerBuilder fileName(String fileName) {
            int index = fileName.lastIndexOf(".");
            if (index != -1) {
                this.fileName = fileName.substring(0, index);
                this.classFileName = this.fileName + ".class";
                this.dexFileName = fileName;
            } else {
                this.fileName = fileName;
                this.classFileName = fileName + ".class";
                this.dexFileName = fileName + ".dex";
            }
            return this;
        }

        /**
         * Absolute cls name dynamic compiler builder.
         *
         * @param absoluteClsName the absolute cls name
         * @return the dynamic compiler builder
         */
        public DynamicCompilerBuilder invokeAbsoluteClsName(String absoluteClsName) {
            this.absoluteClsName = absoluteClsName;
            return this;
        }

        /**
         * Is merge dex dynamic compiler builder.
         *
         * @param isMergeDex the is merge dex
         * @return the dynamic compiler builder
         */
        public DynamicCompilerBuilder isMergeDex(boolean isMergeDex) {
            this.isMergeDex = isMergeDex;
            return this;
        }

        public DynamicCompilerBuilder isUpdate(boolean isUpdate) {
            this.isUpdate = isUpdate;
            return this;
        }

        /**
         * Is generate compile info dynamic compiler builder.
         *
         * @param isGenerateCompileInfo the is generate compile info
         * @return the dynamic compiler builder
         */
        public DynamicCompilerBuilder isGenerateCompileInfo(boolean isGenerateCompileInfo) {
            this.isGenerateCompileInfo = isGenerateCompileInfo;
            return this;
        }

        /**
         * Dex class call back dynamic compiler builder.
         *
         * @param callBack the call back
         * @return the dynamic compiler builder
         */
        public DynamicCompilerBuilder resultCallBack(ResultCallBack callBack) {
            this.callBack = callBack;
            return this;
        }

        /**
         * Dex file path dynamic compiler builder.
         *
         * @param dexFilePath the dex file path
         * @return the dynamic compiler builder
         */
        public DynamicCompilerBuilder dexFilePath(String dexFilePath) {
            this.dexFilePath = dexFilePath;
            return this;
        }

        /**
         * Build net utils.
         *
         * @return the net utils
         */
        public DynamicCompiler build() {
            return new DynamicCompiler(this.context, this.dexFilePath, this.fileName, this.classFileName, this.dexFileName, this.absoluteClsName, this.isMergeDex, this.isUpdate, this.isGenerateCompileInfo, this.owner, this.callBack);
        }
    }

    private boolean checkDexExit() {
        File file = new File(dexFilePath, dexFileName);
        return file.exists();
    }

    private boolean checkClassExit() {
        File file = new File(dexFilePath, classFileName);
        return file.exists();
    }

    /**
     * Delete dex from name.
     *
     * @param dexFileName the dex file name
     */
    public void deleteDexFromName(String dexFileName) {
        File file = new File(dexFilePath, dexFileName);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Delete all dex.
     */
    public void deleteAllDex() {
        File[] dex = new File(dexFilePath).listFiles(pathname -> {
            if (pathname.getName().endsWith(".dex")) {
                return true;
            }
            return false;
        });
        if (dex.length != 0) {
            for (File temp : dex) {
                temp.delete();
            }
        }
    }

    /**
     * Compile java code.
     *
     * @param code the code
     */
    public void compileJavaCode(String code) {
        if (isUpdate) {
            deleteDexFromName(dexFileName);
        }
        if (checkDexExit()) {
            if (isMergeDex) {
                mergeDex(callBack);
                return;
            }
            loadDex(callBack);
            return;
        }
        Observable.create(emitter -> {
                    try {
                        compiler.cook(fileName, new StringReader(code));
                        emitter.onNext(new Object());
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .as(RxLife.as(owner))
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        printCompileInfo(callBack, Statue.COMPILE_JAVA_START, 1, "开始编译java代码");
                    }

                    @Override
                    public void onNext(Object o) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        printCompileInfo(callBack, Statue.COMPILE_JAVA_ERROR, 2, "编译错误：" + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        printCompileInfo(callBack, Statue.COMPILE_JAVA_FINISH, 1, "编译java代码完成");
                        writeClassCode();
                    }
                });
    }

    /**
     * Compile java code.
     *
     * @param codeFile the code file
     * @param format   the format
     */
    public void compileJavaCode(File codeFile, String format) {
        if (isUpdate) {
            deleteDexFromName(dexFileName);
        }
        if (checkDexExit()) {
            if (isMergeDex) {
                mergeDex(callBack);
                return;
            }
            loadDex(callBack);
            return;
        }
        Observable.create(emitter -> {
                    try {
                        compiler.cookFile(codeFile, format);
                        emitter.onNext(new Object());
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .as(RxLife.as(owner))
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        printCompileInfo(callBack, Statue.COMPILE_JAVA_START, 1, "开始编译java代码");
                    }

                    @Override
                    public void onNext(Object o) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        printCompileInfo(callBack, Statue.COMPILE_JAVA_ERROR, 2, "编译错误：" + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        printCompileInfo(callBack, Statue.COMPILE_JAVA_FINISH, 1, "编译java代码完成");
                        writeClassCode();
                    }
                });
    }

    /**
     * Write class code.
     */
    private void writeClassCode() {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
                    try {
                        ClassFile[] classFiles = compiler.getClassFiles();
                        byte[] classBytes = classFiles[0].toByteArray();
                        boolean isSuccess = writeFileToSdCard(cachePath, classFileName, classBytes, classBytes.length, false);
                        if (isSuccess) {
                            emitter.onNext(true);
                            emitter.onComplete();
                        } else {
                            emitter.onError(new RuntimeException("写入class文件失败"));
                        }
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .as(RxLife.as(owner))
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        printCompileInfo(callBack, Statue.WRITE_CLASS_START, 1, "开始写入class文件");
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        printCompileInfo(callBack, Statue.WRITE_CLASS_ERROR, 2, "写入class文件错误：" + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        printCompileInfo(callBack, Statue.WRITE_CLASS_FINISH, 1, "写入class文件完成");
                        compileDex(callBack);
                    }
                });
    }

    /**
     * Compile dex.
     *
     * @param callBack the call back
     */
    public void compileDex(ResultCallBack callBack) {
        if (!checkClassExit()) {
            printCompileInfo(callBack, Statue.COMPILE_DEX_ERROR, 2, "需要编译的class文件不存在");
            return;
        }
        if (!isUpdate) {
            if (checkDexExit()) {
                if (isMergeDex) {
                    mergeDex(callBack);
                    return;
                }
                loadDex(callBack);
                return;
            }
        }
        deleteDexFromName(dexFileName);
        File dexFile = new File(dexFilePath, dexFileName);
        File classFile = new File(dexFilePath, classFileName);
        Observable.create(emitter -> {
                    try {
                        ClassLoader loader = getLocalClassLoader();
                        Class<?> javacClazz = loader.loadClass("com.android.dx.command.Main");
                        Method method = javacClazz.getMethod("main", String[].class);
                        String[] params = new String[]{"--dex", "--no-strict", "--output=" + dexFile.getAbsolutePath(), classFile.getAbsolutePath()};
                        method.invoke(null, (Object) params);
                        emitter.onNext(new Object());
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .as(RxLife.as(owner))
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        printCompileInfo(callBack, Statue.COMPILE_DEX_START, 1, "开始编译dex文件");
                    }

                    @Override
                    public void onNext(Object o) {
                        classFile.delete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        printCompileInfo(callBack, Statue.COMPILE_DEX_ERROR, 2, "编译错误：" + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        printCompileInfo(callBack, Statue.COMPILE_DEX_FINISH, 1, "编译dex文件完成");
                        if (isMergeDex) {
                            mergeDex(callBack);
                        } else {
                            loadDex(callBack);
                        }
                    }
                });
    }

    /**
     * Merge dex.
     *
     * @param callBack the call back
     */
    public void mergeDex(ResultCallBack callBack) {
        File dexFile = new File(dexFilePath, dexFileName);
        Observable.create(new ObservableOnSubscribe<DexClassLoader>() {
                    @Override
                    public void subscribe(ObservableEmitter<DexClassLoader> emitter) throws Exception {
                        try {
                            ClassLoader loader = DynamicCompiler.this.getLocalClassLoader();
                            DexClassLoader dexClassLoader = new DexClassLoader(dexFile.getAbsolutePath(), opDexCachePath, null, loader);
                            Field pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
                            pathListField.setAccessible(true);
                            Object pathList = pathListField.get(dexClassLoader);
                            Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
                            dexElementsField.setAccessible(true);
                            Object dexElements = dexElementsField.get(pathList);
                            pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
                            pathListField.setAccessible(true);
                            Object appPathList = pathListField.get(loader);
                            dexElementsField = appPathList.getClass().getDeclaredField("dexElements");
                            dexElementsField.setAccessible(true);
                            Object appDexElements = dexElementsField.get(appPathList);
                            Object newArray = DynamicCompiler.this.combineArray(appDexElements, dexElements);
                            dexElementsField.set(appPathList, newArray);
                            emitter.onNext(dexClassLoader);
                            emitter.onComplete();
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .as(RxLife.as(owner))
                .subscribe(new Observer<DexClassLoader>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        printCompileInfo(callBack, Statue.MERGE_DEX_START, 1, "开始合并dex文件");
                    }

                    @Override
                    public void onNext(DexClassLoader dexClassLoader) {
                        mergerClassLoader = dexClassLoader;
                    }

                    @Override
                    public void onError(Throwable e) {
                        printCompileInfo(callBack, Statue.MERGE_DEX_ERROR, 2, "合并错误：" + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        printCompileInfo(callBack, Statue.MERGE_DEX_FINISH, 1, "合并dex文件完成");
                        loadDex(callBack);
                    }
                });
    }

    /**
     * Load dex class.
     *
     * @param callBack the call back
     */
    public void loadDex(ResultCallBack callBack) {
        File dexFile = new File(dexFilePath, dexFileName);
        Observable.create((ObservableOnSubscribe<Class<?>>) emitter -> {
                    try {
                        if (mergerClassLoader == null) {
                            mergerClassLoader = new DexClassLoader(dexFile.getAbsolutePath(), opDexCachePath, null, getLocalClassLoader());
                        }
                        Class<?> temp = mergerClassLoader.loadClass(absoluteClsName);
                        ClassManager.INSTANCE.addInitClass(absoluteClsName, temp);
                        emitter.onNext(temp);
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .as(RxLife.as(owner))
                .subscribe(new Observer<Class<?>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        printCompileInfo(callBack, Statue.LOAD_DEX_START, 1, "开始加载dex文件");
                    }

                    @Override
                    public void onNext(Class<?> cls) {
                        try {
                            callBack.getClass(cls);
                        } catch (NoSuchMethodException | InvocationTargetException |
                                 IllegalAccessException | InstantiationException |
                                 ClassNotFoundException e) {
                            printCompileInfo(callBack, Statue.GET_CLASS_ERROR, 2, "获取类错误：" + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        printCompileInfo(callBack, Statue.LOAD_DEX_ERROR, 2, "编译错误：" + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        printCompileInfo(callBack, Statue.LOAD_DEX_FINISH, 1, "加载dex文件完成");
                    }
                });
    }

    private DexFile getDexFile(Object o) {
        try {
            Field dexFileField = o.getClass().getDeclaredField("dexFile");
            dexFileField.setAccessible(true);
            return (DexFile) dexFileField.get(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Object combineArray(Object firstArray, Object secondArray) {
        boolean isDuplicate = false;
        Object[] parentDexList = (Object[]) firstArray;
        Object[] childDexList = (Object[]) secondArray;
        String childDexFileName = getDexFile(childDexList[0]).getName();
        for (Object o : parentDexList) {
            String parentDexFileName = getDexFile(o).getName();
            if (TextUtils.equals(childDexFileName, parentDexFileName)) {
                isDuplicate = true;
            }
        }
        if (isDuplicate) {
            return firstArray;
        }
        Class<?> componentType = firstArray.getClass().getComponentType();
        int firstArrayLength = Array.getLength(firstArray);
        int secondArrayLength = Array.getLength(secondArray);
        int newLength = firstArrayLength + secondArrayLength;
        Object newArray = Array.newInstance(componentType, newLength);

        for (int i = 0; i < newLength; i++) {
            if (i < firstArrayLength) {
                Array.set(newArray, i, Array.get(firstArray, i));
            } else {
                Array.set(newArray, i, Array.get(secondArray, i - firstArrayLength));
            }
        }
        return newArray;
    }

    private void printCompileInfo(ResultCallBack callBack, Statue statue, int level, String info) {
        callBack.getCompileStatue(statue);
        if (isGenerateCompileInfo) {
            switch (level) {
                case 1:
                    Log.i(TAG, info);
                    break;
                case 2:
                    Log.e(TAG, info);
                    break;
                default:
                    break;
            }
        }
    }

    private boolean writeFileToSdCard(String dirPath, String fileName, byte[] data, int len, boolean isContinue) {
        File dirs = new File(dirPath);
        if (!dirs.exists()) {
            dirs.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(new File(dirPath, fileName), isContinue)) {
            fos.write(data, 0, len);
            fos.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private ClassLoader getLocalClassLoader() {
        ClassLoader loader = DynamicCompiler.class.getClassLoader();
        if (loader == null) {
            loader = context.getClassLoader();
        }
        return loader;
    }

    /**
     * The interface Load dex class call back.
     */
    public interface ResultCallBack {
        /**
         * Gets class.
         *
         * @param cls the cls
         * @throws ClassNotFoundException    the class not found exception
         * @throws NoSuchMethodException     the no such method exception
         * @throws InvocationTargetException the invocation target exception
         * @throws IllegalAccessException    the illegal access exception
         * @throws InstantiationException    the instantiation exception
         */
        void getClass(Class<?> cls) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException;

        /**
         * Gets error info.
         *
         * @param statue the statue
         */
        void getCompileStatue(Statue statue);
    }

    /**
     * The enum Statue.
     */
    public enum Statue {

        /**
         * Find class error statue.
         */
        FIND_CLASS_ERROR,

        /**
         * Compile java start statue.
         */
        COMPILE_JAVA_START,
        /**
         * Compile java error statue.
         */
        COMPILE_JAVA_ERROR,
        /**
         * Compile java warning statue.
         */
        COMPILE_JAVA_WARNING,
        /**
         * Compile java finish statue.
         */
        COMPILE_JAVA_FINISH,
        /**
         * Write class start statue.
         */
        WRITE_CLASS_START,
        /**
         * Write class error statue.
         */
        WRITE_CLASS_ERROR,
        /**
         * Write class finish statue.
         */
        WRITE_CLASS_FINISH,

        /**
         * Compile dex start statue.
         */
        MERGE_DEX_START,
        /**
         * Compile dex error statue.
         */
        MERGE_DEX_ERROR,
        /**
         * Compile dex finish statue.
         */
        MERGE_DEX_FINISH,

        /**
         * Compile dex start statue.
         */
        COMPILE_DEX_START,
        /**
         * Compile dex error statue.
         */
        COMPILE_DEX_ERROR,
        /**
         * Compile dex finish statue.
         */
        COMPILE_DEX_FINISH,
        /**
         * Load dex start statue.
         */
        LOAD_DEX_START,
        /**
         * Load dex error statue.
         */
        LOAD_DEX_ERROR,
        /**
         * Load dex finish statue.
         */
        LOAD_DEX_FINISH,

        /**
         * Get class error statue.
         */
        GET_CLASS_ERROR

    }
}
