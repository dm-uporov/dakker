# dakker

[ ![Download](https://api.bintray.com/packages/udy18rus/maven/dakker/images/download.svg) ](https://bintray.com/udy18rus/maven/dakker/_latestVersion)

Android DI-framework. Dagger's principles, koin's syntax.

```kotlin
@DakkerApplication
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startDakker(
            mainActivityModule(
                single { SomeInteractor() }
            )
        )
    }
}

@DakkerScopeCore(scopeId = Constants.MAIN_SCOPE_ID)
class MainActivity : AppCompatActivity() {

    @get:Inject
    val someInteractor: SomeInteractor by injectSomeInteractor()
}
```


## Download
Project ```build.gradle```
```groovy
buildscript {
  repositories {
    jcenter()
  }
}
```
Module ```build.gradle```
```groovy
dependencies {
  implementation "com.github.udy18rus:dakker:$dakker_version"
  kapt "com.github.udy18rus:dakker-kapt:$dakker_version"
}
```

## Usage

### Initialization - hierarchy definition

To initialize dakker you have to ..:
1. .. define root of application with annotation ```@DakkerApplication```;
2. .. invoke ```startDakker``` with modules and providers definitions (hierarchy definition).

There is two types of providers: ```single``` and ```factory```. 
If you use ```single``` your dependency will be initialized only one time per corresponding scope.
If you use ```factory``` every time you will request this dependency you will have a new instance.

Parameter of provider is lambda ```([YourScopeCore]) -> [YourDependency]```, so you can request another dependencies by scope core to provide current dependency.

WARNING! Cycled dependencies will throw StackOverflowException. Check it yourself.

```kotlin
@DakkerApplication
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startDakker(
            appModule(
                // single per application
                single { this }
            ),
            mainActivityModule(
                // single per every MainActivity scope
                single { MainPresenter() },
                factory { SomeInteractor() }
            ),
            anotherActivityModule(
                factory { AnotherInteractor() }
            )
        )
    }
}
```

### Dependencies request
Annotation ```@DakkerScopeCore``` allowed only for ```LifecycleOwner``` classes. Use it to mark cores of scopes.
```scopeId``` is ```Int``` constant. Dependencies from different java-modules will be matched by their scopeIds.

Annotation ```@Inject``` allowed only inside of scope core (or inside ```@DakkerApplication```).
All requested to inject dependencies must be provided while hierarchy definition.

```inject*``` methods will be generated while annotation processing.
 
```kotlin
@DakkerScopeCore(scopeId = Constants.MAIN_SCOPE_ID)
class MainActivity : AppCompatActivity() {

    @get:Inject
    val someInteractor: SomeInteractor by injectSomeInteractor()
}
```

You can include dependency to scope with annotation ```@DakkerScope```. Parameter ```scopeId: Int``` is scope identifier.
If you annotate class declaration you have to define specific provider while dakker initialization.
```kotlin
@DakkerScope(scopeId = Constants.SECOND_SCOPE_ID)
class AnotherInteractor()
```

Either you can annotate specific constructor of dependency. It means that you don't need to define specific provider.
If you have constructor params you will have to provide only dependencies that are not provided yet in this scope (or in the parent scope).
```isSinglePerScope: Boolean``` marks your dependency as single per scope (if ```true``` (by default)) or as factored (if ```false```).
```kotlin
class ThirdInteractor 
@DakkerScope(scopeId = Constants.SECOND_SCOPE_ID, isSinglePerScope = false) 
constructor(anotherInteractor: AnotherInteractor)

@DakkerScopeCore(scopeId = Constants.SECOND_SCOPE_ID)
class AnotherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Actually it is AnotherActivity.getAnotherInteractor()
        // So you have access to this dependency only from core or with core instance
        val anotherInteractor = getAnotherInteractor()
    }
}
```

## Licence

The MIT License

Copyright (c) 2010-2019 Google, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
