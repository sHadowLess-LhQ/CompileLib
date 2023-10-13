package cn.com.shadowless.compilelib;

import android.content.Context;

import androidx.lifecycle.LifecycleOwner;

import com.rxjava.rxlife.RxLife;

import org.codehaus.janino.SimpleCompiler;
import org.codehaus.janino.util.ClassFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * The type Dynamic compiler.
 *
 * @author sHadowLess
 */
public class DynamicCompiler {
    private final Context context;

    private final String dexFilePath;

    private final String javaFileName;

    private final String classFileName;

    private final String dexFileName;

    private final String absoluteClsName;

    private final ResultCallBack callBack;

    private final SimpleCompiler compiler;

    private final String cachePath;

    private final String opDexCachePath;

    private final boolean isGenerateCompileInfo;

    private final LifecycleOwner owner;

    /**
     * Instantiates a new Dynamic compiler.
     *
     * @param context               the context
     * @param dexFilePath           the dex file path
     * @param javaFileName          the java file name
     * @param classFileName         the class file name
     * @param dexFileName           the dex file name
     * @param absoluteClsName       the absolute cls name
     * @param isGenerateCompileInfo the is generate compile info
     * @param owner                 the owner
     * @param callBack              the call back
     */
    public DynamicCompiler(Context context, String dexFilePath, String javaFileName, String classFileName, String dexFileName, String absoluteClsName, boolean isGenerateCompileInfo, LifecycleOwner owner, ResultCallBack callBack) {
        this.context = context;
        this.dexFilePath = dexFilePath;
        this.javaFileName = javaFileName;
        this.classFileName = classFileName;
        this.dexFileName = dexFileName;
        this.absoluteClsName = absoluteClsName;
        this.callBack = callBack;
        this.owner = owner;
        this.compiler = new SimpleCompiler();
        this.cachePath = context.getExternalFilesDir(null).getAbsolutePath();
        this.opDexCachePath = context.getDir("opDex", Context.MODE_PRIVATE).getAbsolutePath();
        this.isGenerateCompileInfo = isGenerateCompileInfo;
        if (isGenerateCompileInfo) {
            compiler.setDebuggingInformation(true, true, true);
            compiler.setCompileErrorHandler((s, location) -> {
                String builder = "错误信息：" +
                        s +
                        "\n" +
                        "错误文件名：" +
                        location.getFileName() +
                        "\n" +
                        "错误行：" +
                        "第" +
                        location.getLineNumber() +
                        "行" +
                        "\n" +
                        "错误列：" +
                        "第" +
                        location.getColumnNumber() +
                        "列";
                callBack.getCompileInfo(builder);
            });
            compiler.setWarningHandler((s, s1, location) -> {
                String builder = "警告信息：" +
                        s1 +
                        "\n" +
                        "文件名：" +
                        location.getFileName() +
                        "\n" +
                        "警告行：" +
                        "第" +
                        location.getLineNumber() +
                        "行" +
                        "\n" +
                        "警告列：" +
                        "第" +
                        location.getColumnNumber() +
                        "列";
                callBack.getCompileInfo(builder);
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

        private String javaFileName;

        private String classFileName;

        private String dexFileName;

        private String absoluteClsName;

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
         * Java file name dynamic compiler builder.
         *
         * @param javaFileName the java file name
         * @return the dynamic compiler builder
         */
        public DynamicCompilerBuilder javaFileName(String javaFileName) {
            this.javaFileName = javaFileName;
            this.classFileName = javaFileName + ".class";
            this.dexFileName = javaFileName + ".dex";
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
            return new DynamicCompiler(this.context, this.dexFilePath, this.javaFileName, this.classFileName, this.dexFileName, this.absoluteClsName, this.isGenerateCompileInfo, this.owner, this.callBack);
        }
    }

    private boolean checkDexExit() {
        File file = new File(dexFilePath, dexFileName);
        return file.exists();
    }

    /**
     * Compile java code.
     *
     * @param code the code
     */
    public void compileJavaCode(String code) {
        if (checkDexExit()) {
            loadDex(new File(dexFilePath, dexFileName), new File(opDexCachePath), absoluteClsName);
            return;
        }
        Observable.create(emitter -> {
                    try {
                        compiler.cook(javaFileName, new StringReader(code));
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
                        printCompileInfo("开始编译java代码");
                    }

                    @Override
                    public void onNext(Object o) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        printCompileInfo("编译错误：" + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        printCompileInfo("编译java代码完成");
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
        if (checkDexExit()) {
            loadDex(new File(dexFilePath, dexFileName), new File(opDexCachePath), absoluteClsName);
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
                        printCompileInfo("开始编译java代码");
                    }

                    @Override
                    public void onNext(Object o) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        printCompileInfo("编译错误：" + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        printCompileInfo("编译java代码完成");
                        writeClassCode();
                    }
                });
    }

    /**
     * Write class code.
     */
    public void writeClassCode() {
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
                        printCompileInfo("开始写入class文件");
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        printCompileInfo("写入class文件错误：" + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        printCompileInfo("写入class文件完成");
                        compileDex(
                                new File(dexFilePath, dexFileName),
                                new File(context.getExternalFilesDir(null).getAbsolutePath(), classFileName));
                    }
                });
    }

    /**
     * Compile dex.
     *
     * @param dexFile   the dex file
     * @param classFile the class file
     */
    public void compileDex(File dexFile, File classFile) {
        Observable.create((ObservableOnSubscribe<Object>) emitter -> {
                    try {
                        ClassLoader loader = DynamicCompiler.class.getClassLoader();
                        if (loader == null) {
                            loader = context.getClassLoader();
                        }
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
                        printCompileInfo("开始编译dex文件");
                    }

                    @Override
                    public void onNext(Object o) {
                        classFile.delete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        printCompileInfo("编译错误：" + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        printCompileInfo("编译dex文件完成");
                        loadDex(dexFile, new File(opDexCachePath), absoluteClsName);
                    }
                });
    }

    /**
     * Load dex class.
     *
     * @param dexFile         the dex file
     * @param optimizeDexPath the optimize dex path
     * @param absoluteClsName the absolute cls name
     */
    public void loadDex(File dexFile, File optimizeDexPath, String absoluteClsName) {
        Observable.create((ObservableOnSubscribe<Class<?>>) emitter -> {
                    try {
                        DexClassLoader cls = new DexClassLoader(dexFile.getAbsolutePath(), optimizeDexPath.getAbsolutePath(), null, DynamicCompiler.class.getClassLoader());
                        Class<?> temp = cls.loadClass(absoluteClsName);
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
                        printCompileInfo("开始加载dex文件");
                    }

                    @Override
                    public void onNext(Class<?> cls) {
                        try {
                            callBack.getClass(cls);
                        } catch (NoSuchMethodException | InvocationTargetException |
                                 IllegalAccessException | InstantiationException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        printCompileInfo("编译错误：" + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        printCompileInfo("加载dex文件成功");
                    }
                });
    }

    private void printCompileInfo(String info) {
        if (isGenerateCompileInfo) {
            callBack.getCompileInfo(info);
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

    /**
     * The interface Load dex class call back.
     */
    public interface ResultCallBack {
        /**
         * Gets class.
         *
         * @param cls the cls
         * @throws NoSuchMethodException the no such method exception
         */
        void getClass(Class<?> cls) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException;

        /**
         * Gets error info.
         *
         * @param info the info
         */
        void getCompileInfo(String info);
    }
}
