# FastAsyncWorldEdit - Legacy

## Fork depuis [ici](https://github.com/Beaness/FastAsyncWorldedit-Legacy)

This is a modified version of FAWE legacy to run in newer JVM versions.

You have to add this to your JVM arguments to make sure FAWE can modify non final fields

```
--add-opens java.base/java.lang.invoke=ALL-UNNAMED
```
