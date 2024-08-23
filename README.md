# CompileLib

### 个人自用安卓运行时动态编译Java字节码库

一个类达到：
Java代码字符串/Java源文件  ->  class字节码 -> dex文件 -> 可反射调用的Class对象

### 【注】：对于1.x版本，2.x是完全重构，不兼容

### 这个库不是所有字符串代码都能编译的库，很多编译输出结果达不到生产要求，需要尝试、修改、找原因，库只是提供一个思路方向，只有极少数场景能够应用，而且也不建议用这个库去加载使用Activity、Dialog这些组件，手动处理apk的Context、Resource、AssetsManger很麻烦，直接上插件化框架不香嘛？应用场景我认为仅限于编译或加载一些简单的、不涉及xml的视图和行为逻辑代码，比如点击事件等

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

- 所有字符串代码，不能含任何注解，无论类上还是方法上，还是参数上
- 所有字符串代码不能使用lambda表达式
- 加载外部apk到当前app的DexList，apk内布局文件的加载问题需要自己解决，这个库不是插件化库
- Activity字符串代码编译报错，只能外部加载含有Activity的dex，AM预插入Activity信息后使用或者预埋代理Activity做强转（还是喜欢预埋，省得强转出问题）
- Dialog字符串代码可以编译，且正常调用show显示
- 可以加载外部apk的View视图到当前App使用
- View.OnClickListener实现类字符串代码可以编译，且正常设置给控件使用
- 外部Apk实现的View.OnClickListener类可以正常获取并正常设置给控件使用，逻辑中含有第三方库，只要合并的app里有就可以用
- 有些原生视图以字符串代码编译有问题，比如Button，不能在任何方法内直接设置setOnClickListener的
  
  View.OnClickListener，这样编译会报错，必须把接口挂载到类上才能正常编译和使用
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
            
           implementation 'io.reactivex.rxjava3:rxjava:3.1.8'
           implementation 'io.reactivex.rxjava3:rxandroid:3.0.2'
           implementation 'com.github.liujingxing.rxlife:rxlife-rxjava3:2.2.2'
           implementation 'com.linkedin.dexmaker:dexmaker:2.28.3'
           implementation 'org.codehaus.janino:janino:3.1.6'
    }
```

#### 使用说明

```
      DynamicCompiler compiler = DynamicCompiler
                .builder()
                //上下文
                .context(Context context)
                //生命周期监听
                .lifecycle(LifecycleOwner owner)
                //编译dex的存放路径
                //与cachePath不能为同一个
                .compileDexPath(String compileDexPath)
                //编译class文件的缓存路径
                //与compileDexPath不能为同一个
                .cachePath(String cachePath)
                //是否输出编译class文件日志
                .hasCompileLog(true)
                .build();
      //设置日志输出监听
      compiler.setStatueObserver(androidx.lifecycle.Observer<Statue> observer);
      //编译单个字符串代码
      compiler.compileStringJavaCodeToClass(String classFileName, String javaCode);
      //编译多个字符串代码（key为classFileName，value为javaCode，key必须以.class结尾）
      compiler.compileStringJavaCodeToClass(Map<String, String> map);
      //编译单个Java文件(默认格式UTF-8)
      compiler.compileFileJavaCodeToClass(File javaFile);
      //指定编码格式编译单个Java文件
      compiler.compileFileJavaCodeToClass(File javaFile, String format);
      //指定编码格式编译多个Java文件（key为File对象，文件名必须以class结尾,value为编码格式）
      compiler.compileFileJavaCodeToClass(Map<File, String> map);
      //指定dex名，普通编译dex文件
      compiler.compileClassFileToDex(String dexName);
      //指定dex名，自定义编译dex文件（如果对dx工具有研究，可自定义编译参数）
      compiler.compileClassFileToDex(String dexName, String... param);
      //指定单个dex名或apk名，合并单个dex到app运行时pathList
      //fileName必须以.dex或.apk结尾
      compiler.mergeDexToAppByName(String fileName);
      //指定多个dex名或apk名，合并多个dex到app运行时pathList
      //fileName必须以.dex或.apk结尾
      compiler.mergeDexToAppByName(List<String> fileNameList);
      //指定单个File对象（Dex文件或Apk文件），合并单个dex到app运行时pathList
      //fileName必须以.dex或.apk结尾
      compiler.mergeDexToAppByFile(File dexFile);
      //指定多个File对象（Dex文件或Apk文件），合并多个dex到app运行时pathList
      //fileName必须以.dex或.apk结尾
      compiler.mergeDexToAppByFile(List<File> dexFileList);
      //指定单个dex名或apk名和需要调用的绝对路径类名，加载dex
      //fileName必须以.dex或.apk结尾
      //返回的map，key为绝对路径类名，value为Class对象
      compiler.loadDexToClassWithoutMergeByName(String fileName, String absoluteClsName);
      //指定多个dex名或apk名和需要调用的绝对路径类名，加载dex（key为dex名，value为绝对路径类名）
      //key必须以.dex或.apk结尾
      //返回的map，key为绝对路径类名，value为Class对象
      compiler.loadDexToClassWithoutMergeByName(List<Map<String, String>> list);
      //指定单个File对象（Dex文件或Apk文件）和需要调用的绝对路径类名，加载dex
      //文件名必须以.dex或.apk结尾
      //返回的map，key为绝对路径类名，value为Class对象
      compiler.loadDexToClassWithoutMergeByFile(File dexFile, String absoluteClsName);
      //指定多个File对象（Dex文件或Apk文件）和需要调用的绝对路径类名，加载dex
      //文件名必须以.dex或.apk结尾
      //返回的map，key为绝对路径类名，value为Class对象
      compiler.loadDexToClassWithoutMergeByFile(List<Map<File, String>> list);
      //指定单个绝对路径类名加载dex（一定是调用merge之后，才能使用，否则找不到类）
      //返回的map，key为绝对路径类名，value为Class对象
      compiler.loadDexToClassWithMergeByName(String absoluteClsName);
      //指定多个绝对路径类名加载dex（一定是调用merge之后，才能使用，否则找不到类）
      //返回的map，key为绝对路径类名，value为Class对象
      compiler.loadDexToClassWithMergeByName(List<String> absoluteClsNameList);
      //删除缓存路径下所有文件
      compiler.clearCacheFolder();
      //删除编译路径下所有文件
      compiler.clearCompileFolder()
```
