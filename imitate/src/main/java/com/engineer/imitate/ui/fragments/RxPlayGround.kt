package com.engineer.imitate.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.alibaba.android.arouter.facade.annotation.Route
import com.engineer.imitate.R
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_rx_play.*
import java.util.concurrent.TimeUnit

@SuppressLint("LogNotTimber")
@Route(path = "/anim/rx_play")
class RxPlayGroundFragment : Fragment() {

    val subject = BehaviorSubject.create<Any>()

    private val TAG = "RxPlayGround"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rx_play, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        start1.setOnClickListener {
            customLifecycle()
        }

        fun providerValue(): Int = 0


        start2.setOnClickListener {
//            Observable.fromCallable { providerValue() }
//                .filter { value -> value != 0 }
////                .onErrorResumeNext(Observable.empty())
//                .switchIfEmpty(Observable.create {
//                    it.onNext(1)
//                })
//                .subscribe({
//                    Log.e(TAG, " it==$it")
//                }, {
//                    Log.e(TAG, " error=$it")
//                })

            Observable.create<Int> {
                it.onNext(0)
                it.onComplete()
            }
                .filter { value -> value != 0 }
//                .onErrorResumeNext(Observable.empty())
                .switchIfEmpty(Observable.create {
                    it.onNext(1)
                })
                .subscribe({
                    Log.e(TAG, " it==$it")
                }, {
                    Log.e(TAG, " error=$it")
                })
        }

        stop_all.setOnClickListener {
            subject.onNext("onDestroyView")
        }

    }

    private fun customLifecycle() {
        Observable.interval(0, 1, TimeUnit.SECONDS)
            .compose(ThreadExTransform())
            .compose(bindUntilOnDestroyView())
//            .takeUntil(subject.filter {
//                it == "onDestroyView"
//            })
//            .compose(OneTransform(subject.filter {
//                it == "onDestroyView"
//            }))
            .doOnDispose {
                Log.e(TAG, "doOnDispose: ")
            }.doOnNext {
                Log.e(TAG, "doOnNext: it == ${it}")
                content.text = it.toString()
            }
            .doOnComplete {
                Log.e(TAG, "doOnComplete: ")
            }
            .doOnError {
                Log.e(TAG, "doOnError: ")
            }
            .subscribe()
    }

    private fun bindUntilOnDestroyView(): ObservableTransformer<Any, Any> {
        return ObservableTransformer {
            it.takeUntil(subject.filter { it == "onDestroyView" })
        }
    }

    override fun onDestroyView() {
        subject.onNext("onDestroyView")
        super.onDestroyView()
    }

    override fun onDestroy() {
        subject.onNext("onDestroy")
        super.onDestroy()
    }
}

class ThreadExTransform<T> : ObservableTransformer<T, T> {

    override fun apply(upstream: Observable<T>): ObservableSource<T> {
        return upstream
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap {
                Observable.just(it)
            }
    }
}

class OneTransform<T>(private val observer: Observable<Any>) : ObservableTransformer<T, T> {
    override fun apply(upstream: Observable<T>): ObservableSource<T> {
        return upstream.takeUntil(observer)
    }
}