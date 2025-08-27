
package com.primal.runs.utils.event
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.primal.runs.utils.Resource


class SingleRequestEvent<T> : SingleActionEvent<Resource<T>>() {
    interface RequestObserver<T> {
        fun onRequestReceived(resource: Resource<T>)
    }

    fun observe(owner: LifecycleOwner, observer: RequestObserver<T>) {
        super.observe(
            owner,
            Observer { resource ->
                if (resource == null) {
                    return@Observer
                }
                observer.onRequestReceived(resource)
            })
    }
}