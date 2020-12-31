# Android 基盤


## Http Network API 通信
- ベースは HttpUrlConnection
- HttpClient.send(HttpRequest, CoroutineScope) を通じて行う
- HttpRequestのサブクラスのリクエストクラスを作成
- coroutineScopeは BaseActivity or BaseFragment
- CoroutineScopeは画面から離れるとcoroutineを停止させるためバックグラウンドで持続的な実行が必要なタスクは他のやり方を使うべき

```kotlin
// Request class
open class HttpRequest<ResponseBody>(
    open val httpMethod: HttpMethod = HttpMethod.GET,
    open val baseUrl: String,
    open val path: String,
    open val header: Map<String, String>,
    open val parameters: Map<String, Any>? = null,
    open val jsonBody: JSONObject? = null
) {
    val endPoint: String get() = baseUrl + path
}

// Response class
class HttpResponse<ResponseBody>(
    var statusCode: Int = HttpsURLConnection.HTTP_BAD_REQUEST,
    var headers: Map<String, List<String>>,
    var body: ResponseBody
)

// Example
HttpClient.send(AssetLinkRequest(), coroutineScope)
    .onSuccess { response ->
        assetlinkLiveData.value = response.body
        handler?.invoke(response.body)
    }
    .onError { error ->
        logInfoD("AssetLink error : statusCode = ${error.statusCode}, message = ${error.message}")
    }

internal class AssetLinkRequest : HttpRequest<AssetLinkResponseBody>(
    baseUrl = "https://ohyaportal.winas.jp",
    path = "/.well-known/assetlinks.json",
    header = mapOf()
)

typealias AssetLinkResponseBody = List<AssetLink>
```


## ImageView - Load Network Image
- ImageView::setImageUrl(url: String, defImageResId: Int? = null, completionHandler: ((HttpResponse<Bitmap>?, HttpError?) -> Unit)? = null)
- 内部実装はHttpClient.send(HttpRequest, CoroutineScope)
- キャッシュなどは未適用、今後追加?



## Architecture : MVVM
- 以下のクラスを継承してViewModelクラスを作成
    - androidx.lifecycle.ViewModel
    - androidx.lifecycle.AndroidViewModel : Application Context 参照用

- LiveDataでデータの変更を検知し処理を行う onChanged() : observe()

- BaseActivity, BaseFragmentからViewModelableを継承および ViewModelクラスを指定

```kotlin
internal class TopFragment : BaseFragment(), ViewModelable<TopViewModel> {
    override val layout: Int = R.layout.fragment_top

    override val viewModelClass: KClass<TopViewModel> = TopViewModel::class

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel?.run {
            assetlinkLiveData.onChanged(this@TopFragment) { assetlinks ->
                logInfoD("assetlink onchange : size = ${assetlinks?.size}")
            }
            fetchAssetLink(this@TopFragment)
        }
    }
}

internal class TopViewModel : BaseViewModel() {

    var assetlinkLiveData = MutableLiveData<List<AssetLink>?>()

    fun fetchAssetLink(coroutineScope: CoroutineScope, handler: ((List<AssetLink>?) -> Unit)? = null) {
        HttpClient.send(AssetLinkRequest(), coroutineScope)
            .onSuccess { response ->
                assetlinkLiveData.value = response.body
            }
    }
    
}

```


## View Binding 1 : kotlinx synthetic
1. layoutファイルのViewにidを付与
```xml
<TextView
        android:id="@+id/textView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Some Text"/>
```

2. ktファイルにlayout synthetic packageを importした後、view idでviewを参照
```kotlin
import kotlinx.android.synthetic._source_set_name_._layout_resource_file_name_.*

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        textView.text = "Another Text"
    }
    
```

- pros
    - 実装が簡単 : layout resource fileの view idがそのままview objectになるので findViewById()不要
    
- cons
    - module間でのview参照にバグがある。cross moduleでViewのやりとりがないなら問題ではない。
    - https://www.reddit.com/r/androiddev/comments/ala9p2/why_kotlinx_synthetic_is_no_longer_a_recommended/
    - findViewById()の代替であって他の機能はない
    


## View Binding 2 : data binding
- layoutのViewとdata objectをバインドするクラスを自動生成
- https://developer.android.com/topic/libraries/data-binding

```xml
<!-- activity_main.xml -->
<layout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        xmlns:tools="http://schemas.android.com/tools"
        tools:context=".MainActivity">

    <data>
        <variable name="viewModel"
                  type="package_name.MainViewModel"/>
    </data>
    
    <!-- 既存のlayoutの root element -->
    <androidx.constraintlayout.widget.ConstraintLayout>
        <TextView
                android:id="@+id/textView"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@{viewModel.someText}"/>
    </androidx.constraintlayout.widget.ConstraintLayout>
   
</layout>
```

```kotlin
class MainActivity: AppCompatActivity() {
    private val viewModel: MainViewModel = ViewModelProviders.of(this)[MainViewModel::class.java]
    
    val binding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            
            // Bind layout with ViewModel
            binding.viewModel = viewModel
            
            // Set lifecycle owner to use LiveData
            binding.lifecycleOwner = this
    
            binding.textView.text = "Another Text"
    }
}

class MainViewModel: ViewModel() {
    var someText: String = "Some Text"
}
```

- pros
    - view - data bindingの信頼性が高い
    - layoutファイルで直接dataの参照ができる
    
- cons
    - ソースコードがやや冗長になる
    
    

## Navigation - Activity
- navigate() : 既存のstartActivity(), startActivityForResult(), overridePendingTransition()を統合した便宜メソッド

```kotlin
fun navigate(intent: Intent, requestCode: Int? = null, anim: Pair<Int, Int>? = null) {
    when (requestCode) {
        null -> startActivity(intent)
        else -> startActivityForResult(intent, requestCode)
    }
    anim?.let { overridePendingTransition(it.first, it.second) }
}
```



## Navigation - Fragment : Navigation Component
- https://developer.android.com/guide/navigation?hl=ja
- デフォルトで端末のバックキーに対してFragment pop動作

1. Navigation Graph ファイルを用意
```xml
<!-- nav_graph.xml -->
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/navGraph"
    app:startDestination="@id/mainFragment">

    <fragment
        android:id="@+id/mainFragment"
        android:name="jp.winas.android.foundation_android.scene.nav.MainFragment"
        android:label="fragment_main"
        tools:layout="@layout/fragment_main" >

        <action
            android:id="@+id/actionToLogin"
            app:destination="@+id/loginFragment" />
    </fragment>

    <fragment
        android:id="@+id/loginFragment"
        android:name="jp.winas.android.foundation_android.scene.nav.LoginFragment"
        android:label="fragment_login"
        tools:layout="@layout/fragment_login" />
</navigation>
```

2. NavHostFragment 配置 : navigation hostとして使うFragment
```xml
<!-- activityのlayoutなどに配置 -->
<fragment
        android:id="@+id/navHostFragment"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:defaultNavHost="true"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:navGraph="@navigation/nav_graph" />
```

3. Fragment内で navigate(resId: Int)メソッドを使って他のFragmentに遷移
```kotlin
    // action指定
    navigate(R.id.actionToLogin)
    // destination指定
    navigate(R.id.loginFragment)
```

4. argument(Bundleデータ)を渡す
```kotlin
navigate(
        R.id.profileFragment,
        args = bundleOf())
```

5. enter, exit animationを指定
```xml
<action
        android:id="@+id/actionToLogin"
        app:destination="@+id/loginFragment"
        app:enterAnim="@anim/fade_in"
        app:exitAnim="@anim/fade_out"
        app:popEnterAnim="@anim/fade_in"
        app:popExitAnim="@anim/fade_out" />
```

```kotlin
navigate(
        R.id.profileFragment,
        args = bundleOf(),
        navOptions = NavOptions.Builder().apply {
            setEnterAnim(R.anim.fade_in)
            setExitAnim(R.anim.fade_out)
            setPopEnterAnim(R.anim.fade_in)
            setPopExitAnim(R.anim.fade_out)
        }.build())
```


## Storage - SharedPreferences
- StoreMapクラスを継承したクラスを用意、変数宣言の末尾に by storedMap() 記述
- クラス変数の get set が SharedPreferencesの get* put*に紐づく

```kotlin
    AppSetting(this).run {
        token = "0123456789abcdef"
        version = 7
        user = User("android", 10)
    }

    // Load from local file
    AppSetting(this).run {
        logInfoD(token)
        logInfoD(version)
        logInfoD(user)
    }
        
class AppSetting(context: Context) : StoredMap(context) {

    var token: String? by storedMap()
    var version: Int? by storedMap()
    var user: User? by storedMap()

}

data class User(val name: String, val age: Int)
```


## Utility : Logger
- 簡単にinfo, error 2つのクローバル関数
```kotlin
fun logInfo(msg: Any?)
fun logError(msg: Any?)
```

- debug build用ログ
```kotlin
fun logInfoD(msg: Any?)
fun logErrorD(msg: Any?)
```