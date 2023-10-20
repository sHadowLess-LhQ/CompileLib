# CompileLib

#### 软件架构

个人自用安卓运行时动态编译Java字节码库

### 缘由

以前有这么个需求是要动态变化按钮的点击事件

一想这不就一热更新的事情嘛，结果说尽量不能有文件，最好就一段代码字符串就能用

然后想起上学时用的某个可以在安卓设备上写安卓代码的APP后，掏了下，发现它内部是用的Sun和Eclipse的代码编译库。

### 卧槽？

不是JDK的JavaCompile不能在Dalvik虚拟机上用吗？

于是一知半解的掏了Eclipse的编译库来试了下。

不错，能用且好使，但是编译的速度稍微有点不大行。

因此打开了新世界，并找到了janino这个更轻量的库；

不错，能用且更好使，编译比JDT快不少，还不需要android.jar

至此，封装处理了下，这个库就完成了。

### 注意

- Activity字符串代码编译报错，只能外部加载含有Activity的dex，AM预插入Activity信息后使用或者预埋代理Activity做强转（还是喜欢预埋，省得强转出问题）
- Dialog字符串代码可以编译，且正常调用show显示
- View.OnClickListener实现类字符串代码可以编译，且正常设置给控件使用
- 可直接加载apk文件，并调用apk中dex的类，但是apk不易过于复杂、体积过大，DexClassLoader加载慢，阿里的hook库只能用于28以下的设备，就懒得弄了

#### **其他还有什么代码编译不出暂时未知......**

### 安装教程

Step 1. 添加仓库地址和配置

```
     //旧AndroidStudio版本
     //build.gradle
     allprojects {
         repositories {
            ...
              maven { url 'https://jitpack.io' }
         }
     }
     
     //新AndroidStudio版本
     //settings.gradle
     dependencyResolutionManagement {
          repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
          repositories {
            ...
             maven { url 'https://jitpack.io' }
          }
      }
```

Step 2. 添加依赖

a、克隆引入

直接下载源码引入model

b、远程仓库引入

[![](https://jitpack.io/v/com.gitee.shadowless_lhq/compile-lib.svg)](https://jitpack.io/#com.gitee.shadowless_lhq/compile-lib)

```
     dependencies {
           implementation 'com.gitee.shadowless_lhq:compile-lib:Tag'
            
           implementation 'io.reactivex.rxjava2:rxjava:2.2.21'
           implementation 'io.reactivex.rxjava2:rxandroid:2.1.1'
           implementation 'com.github.liujingxing.rxlife:rxlife-rxjava2:2.2.2'
           implementation 'org.codehaus.janino:janino:3.1.6'
    }
```

#### 使用说明

```
      DynamicCompilerDex compilerDex = DynamicCompilerDex
                .builder()
                //上下文
                .context(Context context)
                //生命周期监听
                .lifecycle(LifecycleOwner owner)
                //是否合并Dex（仅对使用compileJavaCode时有效）
                .isMergeDex(true)
                //是否打印编译日志
                .isGenerateCompileInfo(boolean isPrint)
                //编译的文件名
                //可以填入xx.class或者xx.dex或者xx.apk
                //内部会自动处理，如果使用compileDex，class文件名和填入的要一致，其他类型类推
                .fileName(String fileName)
                //输出dex文件的路径
                .dexFilePath(String dexFilePath)
                //需要加载的完整类名
                .invokeAbsoluteClsName(String className)
                .resultCallBack(new DynamicCompilerDex.ResultCallBack() {
                    @Override
                    public void getClass(Class<?> cls) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
                         //获取到已加载的类
                    }

                    @Override
                    public void getCompileStatue(DynamicCompilerDex.Statue statue) {
                         //获取编译状态
                    }
                }).build();
      //编译字符串代码，获取dex中的class
      compilerDex.compileJavaCode(String code);
      //编译Java文件，获取dex中的class
      compilerDex.compileJavaCode(File code, String format);
      //删除输出Dex路径下的所有dex文件
      compilerDex.deleteAllDex();
      //删除输出Dex路径下的指定文件名的dex文件
      compilerDex.deleteDexFromName();
      //已有class文件，编译dex文件，并在已设置的结果回调接口获取dex中的class
      //classFile可以设置文件夹，编译多个class文件为一个dex
      compilerDex.compileDex(ResultCallBack callBack);
      //已有dex文件，加载dex文件并获取dex中的class
      compilerDex.loadDex(ResultCallBack callBack);
      //已有dex文件，合并且自动加载dex文件并获取dex中的class
      compilerDex.mergeDex(ResultCallBack callBack);
      //根据传入invokeAbsoluteClsName的类名获取已创建的dex的class
      ClassManager.INSTANCE.getInitCls(String className);
```
