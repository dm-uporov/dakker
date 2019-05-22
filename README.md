# dakker

[ ![Download](https://api.bintray.com/packages/udy18rus/maven/dakker/images/download.svg) ](https://bintray.com/udy18rus/maven/dakker/_latestVersion)

Android DI-framework. Dagger's principles, koin's syntax.

```kotlin
@DakkerApplication
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startDakker(
            mainActivityNode(
                single { SomeInteractor() }
            )
        )
    }
}

@LifecycleScopeCore
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
```kotlin
@DakkerApplication
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startDakker(
            // Application module
            appNode(
                // singleton dependency (type is Application or Context)
                // here parameter of single is lambda with App as receiver
                single { this }
            ),
            // MainActivity module
            mainActivityNode(
                // single per activity lifecycle dependency
                single { SomeInteractor() },
                // here parameter of single is lambda with MainActivity as receiver
                single { MainPresenter() }
            ),
            anotherActivityNode(
                // Every time you will request this dependency you will have new instance
                factory { AnotherInteractor() }
            )
        )
    }
}

// This annotation allowed only for LifecycleOwner classes. It means that this class is core of scope.
@LifecycleScopeCore
class MainActivity : AppCompatActivity() {

    // This annotation allowed only inside of scope core (or inside @DakkerApplication)
    // If you use this annotation you have to define a provider 
    // while dakker initialization (startDakker() invocation)
    @get:Inject
    val someInteractor: SomeInteractor by injectSomeInteractor()
}

// You can define scope of dependency with this annotation. Parameter is the KClass of scope core. 
// (WIP: String qualifier)
// 1. If you have only one constructor you don't need to define special provider.
// 2. If you have constructor params you have to provide only dependencies 
// that are not provided yet in this scope or in the parent scope.
// 3. If you have more than one constructor you can annotate prefer constructor as provider
@LifecycleScope(AnotherActivity::class)
class AnotherInteractor()

@LifecycleScopeCore
class AnotherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Actually it is AnotherActivity.getAnotherInteractor()
        // So you have access to this dependency only from this class or with this class instance
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
