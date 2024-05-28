package com.cc.eye.event

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object Bus {

    private val events = mutableMapOf<String, Eventbus<*>>()

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T> with(key: String): Eventbus<T> {
        // Bus.with<String>("测试").post(this.lifecycleScope,"")
        // Bus.with<String>("测试").register(this) {
        //            Log.e("主界面", "测试数据--$it")
        //       }
        var eventbus = events[key]
        if (eventbus == null) {
            eventbus = Eventbus<T>(key)
            events[key] = eventbus
        }
        return eventbus as Eventbus<T>
    }

    class Eventbus<T>(private val key: String) : LifecycleObserver {

        // 私有对象用于发送消息
        private val _event: MutableSharedFlow<T> by lazy { obtain() }

        // 暴露的公有对象用于接收消息
        private val event = _event.asSharedFlow()

        // replay -> 0:无粘性;1:粘性.
        private fun obtain(): MutableSharedFlow<T> =
            MutableSharedFlow(1,1, BufferOverflow.DROP_OLDEST)

        // 主线程接收消息
        fun register(lifecycleOwner: LifecycleOwner, action: (t: T) -> Unit) {

            lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    super.onDestroy(owner)
                    val subscriptCount = _event.subscriptionCount.value
                    if (subscriptCount <= 0)
                        events.remove(key)
                }
            })

            lifecycleOwner.lifecycleScope.launch {
                event.collect {
                    try {
                        action(it)
                    } catch (e: Exception) {
                        e.stackTrace
                    }
                }
            }
        }

        // 协程发送数据
        suspend fun post(event: T) {
            _event.emit(event)
        }

        // 主线程发送数据
        fun post(scope: CoroutineScope, event: T) {
            scope.launch {
                _event.emit(event)
            }
        }

    }

}