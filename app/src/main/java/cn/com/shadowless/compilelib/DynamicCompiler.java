package cn.com.shadowless.compilelib;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;

import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.janino.SimpleCompiler;
import org.codehaus.janino.util.ClassFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import io.reactivex.rxjava3.core.Observable;

/**
 * 动态编译
 *
 * @author sHadowLess
 */
public final class DynamicCompiler {

    /**
     * The Tag.
     */
    private final String TAG = DynamicCompiler.class.getSimpleName();

    /**
     * The Context.
     */
    private final Context context;

    /**
     * The Compile dex path.
     */
    private final String compileDexPath;

    /**
     * The Has compile log.
     */
    private final boolean hasCompileLog;

    /**
     * The Owner.
     */
    private final LifecycleOwner owner;

    /**
     * The Cache path.
     */
    private final String cachePath;

    /**
     * The Op dex cache path.
     */
    private final String opDexCachePath;

    /**
     * The Statue data.
     */
    private final MutableLiveData<Statue> statueData;

    /**
     * Instantiates a new Dynamic compiler ex.
     *
     * @param context        the context
     * @param compileDexPath the compile dex path
     * @param cachePath      the cache path
     * @param hasCompileLog  the has compile log
     * @param owner          the owner
     */
    public DynamicCompiler(Context context, String compileDexPath, String cachePath, boolean hasCompileLog, LifecycleOwner owner) {
        String tempCachePath;
        this.context = context;
        this.hasCompileLog = hasCompileLog;
        this.owner = owner;
        this.compileDexPath = compileDexPath;
        tempCachePath = cachePath;
        if (TextUtils.isEmpty(tempCachePath)) {
            tempCachePath = context.getExternalCacheDir().getAbsolutePath();
        } else {
            if (!new File(tempCachePath).isDirectory()) {
                throw new RuntimeException("缓存路径必须是文件夹");
            }
        }
        if (!new File(this.compileDexPath).isDirectory()) {
            throw new RuntimeException("dex编译路径必须是文件夹");
        }
        this.cachePath = tempCachePath;
        this.opDexCachePath = context.getDir("opDex", Context.MODE_PRIVATE).getAbsolutePath();
        if (TextUtils.equals(this.compileDexPath, this.cachePath)) {
            throw new RuntimeException("缓存路径和dex编译路径不能一致");
        }
        this.statueData = new MutableLiveData<>();
    }

    /**
     * Gets error handler.
     *
     * @return the error handler
     */
    private ErrorHandler getErrorHandler() {
        return (s, location) -> {
            String builder = "错误信息：" + s + "\n" + "错误文件名：" + location.getFileName() + "\n" + "错误行：" + "第" + location.getLineNumber() + "行" + "\n" + "错误列：" + "第" + location.getColumnNumber() + "列";
            printCompileInfo(Statue.COMPILE_JAVA_ERROR, 2, builder.toString());
        };
    }

    /**
     * Gets warning handler.
     *
     * @return the warning handler
     */
    private WarningHandler getWarningHandler() {
        return (s, s1, location) -> {
            String builder = "警告信息：" + s1 + "\n" + "文件名：" + location.getFileName() + "\n" + "警告行：" + "第" + location.getLineNumber() + "行" + "\n" + "警告列：" + "第" + location.getColumnNumber() + "列";
            printCompileInfo(Statue.COMPILE_JAVA_WARNING, 1, builder.toString());
        };
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

        /**
         * The Context.
         */
        private Context context;

        /**
         * The Compile dex path.
         */
        private String compileDexPath;

        /**
         * The Cache path.
         */
        private String cachePath;

        /**
         * The Has compile log.
         */
        private boolean hasCompileLog;

        /**
         * The Owner.
         */
        private LifecycleOwner owner;

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
         * @param compileDexPath the compile dex path
         * @return the dynamic compiler builder
         */
        public DynamicCompilerBuilder compileDexPath(String compileDexPath) {
            this.compileDexPath = compileDexPath;
            return this;
        }

        /**
         * Cache path dynamic compiler builder.
         *
         * @param cachePath the cache path
         * @return the dynamic compiler builder
         */
        public DynamicCompilerBuilder cachePath(String cachePath) {
            this.cachePath = cachePath;
            return this;
        }

        /**
         * Absolute cls name dynamic compiler builder.
         *
         * @param hasCompileLog the has compile log
         * @return the dynamic compiler builder
         */
        public DynamicCompilerBuilder hasCompileLog(boolean hasCompileLog) {
            this.hasCompileLog = hasCompileLog;
            return this;
        }

        /**
         * Build net utils.
         *
         * @return the net utils
         */
        public DynamicCompiler build() {
            return new DynamicCompiler(this.context, this.compileDexPath, this.cachePath, this.hasCompileLog, this.owner);
        }
    }

    /**
     * Compile string java code to class observable.
     *
     * @param classFileName the class file name
     * @param javaCode      the java code
     * @return the observable
     */
    public Observable<Boolean> compileStringJavaCodeToClass(String classFileName, String javaCode) {
        Map<String, String> map = new HashMap<>(1);
        map.put(classFileName, javaCode);
        return compileStringJavaCodeToClass(map);
    }

    /**
     * Compile string java code to class observable.
     *
     * @param map the map
     * @return the observable
     */
    public Observable<Boolean> compileStringJavaCodeToClass(Map<String, String> map) {
        return Observable.create(emitter -> {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String originFileName = entry.getKey();
                if (!originFileName.endsWith(".class")) {
                    emitter.onError(new Throwable("传入map的key必须以.class结尾"));
                    return;
                }
                SimpleCompiler compiler = new SimpleCompiler();
                if (hasCompileLog) {
                    compiler.setDebuggingInformation(true, true, true);
                    compiler.setCompileErrorHandler(getErrorHandler());
                    compiler.setWarningHandler(getWarningHandler());
                } else {
                    compiler.setDebuggingInformation(false, false, false);
                }
                compiler.cook(originFileName, new StringReader(entry.getValue()));
                ClassFile[] classFiles = compiler.getClassFiles();
                byte[] classBytes = classFiles[0].toByteArray();
                File dirs = new File(cachePath);
                if (!dirs.exists()) {
                    dirs.mkdirs();
                }
                try (FileOutputStream fos = new FileOutputStream(new File(cachePath, originFileName))) {
                    fos.write(classBytes, 0, classBytes.length);
                    fos.flush();
                } catch (IOException e) {
                    emitter.onNext(false);
                    emitter.onComplete();
                    return;
                }
            }
            emitter.onNext(true);
            emitter.onComplete();
        });
    }

    /**
     * Compile file java code to class observable.
     *
     * @param javaFile the java file
     * @return the observable
     */
    public Observable<Boolean> compileFileJavaCodeToClass(File javaFile) {
        Map<File, String> map = new HashMap<>(1);
        map.put(javaFile, StandardCharsets.UTF_8.name());
        return compileFileJavaCodeToClass(map);
    }

    /**
     * Compile file java code to class observable.
     *
     * @param javaFile the java file
     * @param format   the format
     * @return the observable
     */
    public Observable<Boolean> compileFileJavaCodeToClass(File javaFile, String format) {
        Map<File, String> map = new HashMap<>(1);
        map.put(javaFile, format);
        return compileFileJavaCodeToClass(map);
    }

    /**
     * Compile file java code to class observable.
     *
     * @param map the map
     * @return the observable
     */
    public Observable<Boolean> compileFileJavaCodeToClass(Map<File, String> map) {
        return Observable.create(emitter -> {
            for (Map.Entry<File, String> entry : map.entrySet()) {
                File currentFile = entry.getKey();
                String fileName = currentFile.getName();
                if (!fileName.endsWith(".class")) {
                    emitter.onError(new Throwable("传入map的key的File文件名必须以.class结尾"));
                    return;
                }
                SimpleCompiler compiler = new SimpleCompiler();
                if (hasCompileLog) {
                    compiler.setDebuggingInformation(true, true, true);
                    compiler.setCompileErrorHandler(getErrorHandler());
                    compiler.setWarningHandler(getWarningHandler());
                } else {
                    compiler.setDebuggingInformation(false, false, false);
                }
                compiler.cookFile(currentFile, entry.getValue());
                ClassFile[] classFiles = compiler.getClassFiles();
                byte[] classBytes = classFiles[0].toByteArray();
                File dirs = new File(cachePath);
                if (!dirs.exists()) {
                    dirs.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(new File(cachePath, fileName));
                fos.write(classBytes, 0, classBytes.length);
                fos.flush();
                fos.close();
            }
            emitter.onNext(true);
            emitter.onComplete();
        });
    }

    /**
     * Compile class file to dex observable.
     *
     * @param dexName the dex name
     * @return the observable
     */
    public Observable<Boolean> compileClassFileToDex(String dexName) {
        return compileClassFileToDex(dexName, "--dex", "--no-strict", "--output=" + new File(compileDexPath, dexName).getAbsolutePath(), cachePath);
    }

    /**
     * Compile class file to dex observable.
     *
     * @param dexName the dex name
     * @param param   the param
     * @return the observable
     */
    public Observable<Boolean> compileClassFileToDex(String dexName, String... param) {
        return Observable.create(emitter -> {
            if (!dexName.endsWith(".dex")) {
                emitter.onError(new Throwable("传入的dexName必须以.dex结尾"));
                return;
            }
            File dexFile = new File(compileDexPath, dexName);
            if (dexFile.exists()) {
                dexFile.delete();
            }
            ClassLoader loader = getLocalClassLoader();
            Class<?> javacClazz = loader.loadClass("com.android.dx.command.Main");
            Method method = javacClazz.getMethod("main", String[].class);
            method.invoke(null, (Object) param);
            emitter.onNext(true);
            emitter.onComplete();
        });
    }

    /**
     * Merge dex to app by name observable.
     *
     * @param fileName the file name
     * @return the observable
     */
    public Observable<Boolean> mergeDexToAppByName(String fileName) {
        return mergeDexToAppByName(Arrays.asList(fileName));
    }

    /**
     * Merge dex to app by name observable.
     *
     * @param fileNameList the file name list
     * @return the observable
     */
    public Observable<Boolean> mergeDexToAppByName(List<String> fileNameList) {
        return Observable.create(emitter -> {
            ClassLoader loader = getLocalClassLoader();

            Field appPathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
            appPathListField.setAccessible(true);
            Object appPathList = appPathListField.get(loader);
            Field appDexElementsField = appPathList.getClass().getDeclaredField("dexElements");
            appDexElementsField.setAccessible(true);
            Object appDexElements = appDexElementsField.get(appPathList);

            for (String name : fileNameList) {
                if (!name.endsWith(".dex") && !name.endsWith(".apk")) {
                    emitter.onError(new Throwable("传入的dexName必须以.dex或.apk结尾"));
                    return;
                }
                File dexFile = new File(compileDexPath, name);
                if (!dexFile.exists()) {
                    emitter.onError(new Throwable("该dex文件不存在：" + dexFile.getAbsolutePath()));
                    return;
                }
                DexClassLoader dexClassLoader = new DexClassLoader(dexFile.getAbsolutePath(), opDexCachePath, null, loader);
                Field pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
                pathListField.setAccessible(true);
                Object pathList = pathListField.get(dexClassLoader);
                Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
                dexElementsField.setAccessible(true);
                Object dexElements = dexElementsField.get(pathList);
                Object newArray = combineArray(appDexElements, dexElements);
                dexElementsField.set(appPathList, newArray);
            }
            emitter.onNext(true);
            emitter.onComplete();
        });
    }

    /**
     * Merge dex to app by file observable.
     *
     * @param dexFile the dex file
     * @return the observable
     */
    public Observable<Boolean> mergeDexToAppByFile(File dexFile) {
        return mergeDexToAppByFile(Arrays.asList(dexFile));
    }

    /**
     * Merge dex to app by file observable.
     *
     * @param dexFileList the dex file list
     * @return the observable
     */
    public Observable<Boolean> mergeDexToAppByFile(List<File> dexFileList) {
        return Observable.create(emitter -> {
            ClassLoader loader = getLocalClassLoader();

            Field appPathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
            appPathListField.setAccessible(true);
            Object appPathList = appPathListField.get(loader);
            Field appDexElementsField = appPathList.getClass().getDeclaredField("dexElements");
            appDexElementsField.setAccessible(true);
            Object appDexElements = appDexElementsField.get(appPathList);

            for (File file : dexFileList) {
                String fileName = file.getName();
                if (!fileName.endsWith(".dex") || !fileName.endsWith(".apk")) {
                    emitter.onError(new Throwable("传入的File必须以.dex或.apk结尾"));
                    return;
                }
                if (!file.exists()) {
                    emitter.onError(new Throwable("该dex文件不存在：" + file.getAbsolutePath()));
                    return;
                }
                DexClassLoader dexClassLoader = new DexClassLoader(file.getAbsolutePath(), opDexCachePath, null, loader);
                Field pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
                pathListField.setAccessible(true);
                Object pathList = pathListField.get(dexClassLoader);
                Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
                dexElementsField.setAccessible(true);
                Object dexElements = dexElementsField.get(pathList);
                Object newArray = combineArray(appDexElements, dexElements);
                dexElementsField.set(appPathList, newArray);
            }
            emitter.onNext(true);
            emitter.onComplete();
        });
    }

    /**
     * Load dex to class without merge by name observable.
     *
     * @param dexName         the dex name
     * @param absoluteClsName the absolute cls name
     * @return the observable
     */
    public Observable<Map<String, Class<?>>> loadDexToClassWithoutMergeByName(String dexName, String absoluteClsName) {
        Map<String, String> map = new HashMap<>(1);
        map.put(dexName, absoluteClsName);
        return loadDexToClassWithoutMergeByName(Arrays.asList(map));
    }

    /**
     * Load dex to class without merge by name observable.
     *
     * @param list the list
     * @return the observable
     */
    public Observable<Map<String, Class<?>>> loadDexToClassWithoutMergeByName(List<Map<String, String>> list) {
        return Observable.create(emitter -> {
            Map<String, Class<?>> classMap = new HashMap<>(list.size());
            for (Map<String, String> temp : list) {
                for (Map.Entry<String, String> entry : temp.entrySet()) {
                    File file = new File(compileDexPath, entry.getKey());
                    String fileName = file.getName();
                    if (!fileName.endsWith(".dex") && !fileName.endsWith(".apk")) {
                        emitter.onError(new Throwable("传入的File必须以.dex或.apk结尾"));
                        return;
                    }
                    if (!file.exists()) {
                        emitter.onError(new Throwable("该dex文件不存在：" + file.getAbsolutePath()));
                        return;
                    }
                    DexClassLoader classLoader = new DexClassLoader(file.getAbsolutePath(), opDexCachePath, null, getLocalClassLoader());
                    Class<?> loadClass = classLoader.loadClass(entry.getValue());
                    classMap.put(entry.getValue(), loadClass);
                }
            }
            emitter.onNext(classMap);
            emitter.onComplete();
        });
    }

    /**
     * Load dex to class without merge by file observable.
     *
     * @param dexFile         the dex file
     * @param absoluteClsName the absolute cls name
     * @return the observable
     */
    public Observable<Map<String, Class<?>>> loadDexToClassWithoutMergeByFile(File dexFile, String absoluteClsName) {
        Map<File, String> map = new HashMap<>(1);
        map.put(dexFile, absoluteClsName);
        return loadDexToClassWithoutMergeByFile(Arrays.asList(map));
    }

    /**
     * Load dex to class without merge by file observable.
     *
     * @param list the list
     * @return the observable
     */
    public Observable<Map<String, Class<?>>> loadDexToClassWithoutMergeByFile(List<Map<File, String>> list) {
        return Observable.create(emitter -> {
            Map<String, Class<?>> classMap = new HashMap<>(list.size());
            for (Map<File, String> temp : list) {
                for (Map.Entry<File, String> entry : temp.entrySet()) {
                    File file = entry.getKey();
                    String fileName = file.getName();
                    if (!fileName.endsWith(".dex") || !fileName.endsWith(".apk")) {
                        emitter.onError(new Throwable("传入的File必须以.dex或.apk结尾"));
                        return;
                    }
                    if (!file.exists()) {
                        emitter.onError(new Throwable("该dex文件不存在：" + file.getAbsolutePath()));
                        return;
                    }
                    DexClassLoader classLoader = new DexClassLoader(file.getAbsolutePath(), opDexCachePath, null, getLocalClassLoader());
                    Class<?> loadClass = classLoader.loadClass(entry.getValue());
                    classMap.put(entry.getValue(), loadClass);
                }
            }
            emitter.onNext(classMap);
            emitter.onComplete();
        });
    }

    /**
     * Load dex to class with merge by name observable.
     *
     * @param absoluteClsName the absolute cls name
     * @return the observable
     */
    public Observable<Map<String, Class<?>>> loadDexToClassWithMergeByName(String absoluteClsName) {
        return loadDexToClassWithMergeByName(Arrays.asList(absoluteClsName));
    }


    /**
     * Load dex to class with merge by name observable.
     *
     * @param absoluteClsNameList the absolute cls name list
     * @return the observable
     */
    public Observable<Map<String, Class<?>>> loadDexToClassWithMergeByName(List<String> absoluteClsNameList) {
        return Observable.create(emitter -> {
            Map<String, Class<?>> classMap = new HashMap<>(absoluteClsNameList.size());
            for (String name : absoluteClsNameList) {
                ClassLoader loader = getLocalClassLoader();
                Class<?> temp = loader.loadClass(name);
                classMap.put(name, temp);
            }
            emitter.onNext(classMap);
            emitter.onComplete();
        });
    }

    /**
     * Sets statue observer.
     *
     * @param observer the observer
     */
    public void setStatueObserver(androidx.lifecycle.Observer<Statue> observer) {
        statueData.observe(owner, observer);
    }

    /**
     * Clear cache folder.
     */
    public void clearCacheFolder() {
        deleteFilesInDirectory(cachePath);
    }

    /**
     * Clear compile folder.
     */
    public void clearCompileFolder() {
        deleteFilesInDirectory(compileDexPath);
    }

    /**
     * Delete files in directory.
     *
     * @param dirPath the dir path
     */
    private void deleteFilesInDirectory(String dirPath) {
        File directory = new File(dirPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                    } else if (file.isDirectory()) {
                        deleteFilesInDirectory(file.getAbsolutePath());
                        file.delete();
                    }
                }
            }
        }
    }


    /**
     * Gets dex file.
     *
     * @param o the o
     * @return the dex file
     * @throws NoSuchFieldException   the no such field exception
     * @throws IllegalAccessException the illegal access exception
     */
    private DexFile getDexFile(Object o) throws NoSuchFieldException, IllegalAccessException {
        Field dexFileField = o.getClass().getDeclaredField("dexFile");
        dexFileField.setAccessible(true);
        return (DexFile) dexFileField.get(o);
    }


    /**
     * Print compile info.
     *
     * @param statue the statue
     * @param level  the level
     * @param info   the info
     */
    private void printCompileInfo(Statue statue, int level, String info) {
        statueData.postValue(statue);
        if (hasCompileLog) {
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

    /**
     * Gets local class loader.
     *
     * @return the local class loader
     */
    private ClassLoader getLocalClassLoader() {
        ClassLoader loader = DynamicCompiler.class.getClassLoader();
        if (loader == null) {
            loader = context.getClassLoader();
        }
        return loader;
    }

    /**
     * Combine array object.
     *
     * @param firstArray  the first array
     * @param secondArray the second array
     * @return the object
     * @throws NoSuchFieldException   the no such field exception
     * @throws IllegalAccessException the illegal access exception
     */
    private Object combineArray(Object firstArray, Object secondArray) throws NoSuchFieldException, IllegalAccessException {
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

    /**
     * The enum Statue.
     */
    public enum Statue {

        /**
         * Compile java error statue.
         */
        COMPILE_JAVA_ERROR,
        /**
         * Compile java warning statue.
         */
        COMPILE_JAVA_WARNING

    }
}
