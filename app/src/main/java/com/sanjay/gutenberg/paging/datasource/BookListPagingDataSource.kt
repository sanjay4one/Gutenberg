package com.sanjay.gutenberg.paging.datasource

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.sanjay.gutenberg.constants.State
import com.sanjay.gutenberg.data.Result
import com.sanjay.gutenberg.data.repository.GutenbergRepository
import com.sanjay.gutenberg.data.repository.remote.model.Book
import com.sanjay.gutenberg.injection.module.MainCoroutineScope
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class BookListPagingDataSource @Inject constructor(
    private val repository: GutenbergRepository,
    @MainCoroutineScope private val scope: CoroutineScope
) :
    PageKeyedDataSource<Int, Book>() {
    val disposable = CompositeDisposable()

    //LiveData object for state
    var state = MutableLiveData<State>()
    var searchQuery = MutableLiveData<String>()
    var category = MutableLiveData<String>()

    //Variable required for retrying the API call which gets failed due to any error like no internet
    private var _retry: (() -> Unit)? = null

    /**
     * Creating the lambda expression for specific page to call the API
     */
    private fun setRetry(function: (() -> Unit)?) {
        this._retry = function
    }

    //Retrying the API call
    fun retry() {
        scope.launch {
            _retry?.invoke()
        }
    }

    private fun updateState(state: State) {
        this.state.postValue(state)
    }

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, Book>
    ) {
        updateState(State.LOADING)
        val currentPage = 1
        val nextPage = currentPage + 1
        //Call api
        /*repository.searchBooks(currentPage, category.value!!, searchQuery.value)
            .subscribe(
                { books ->
                    updateState(State.DONE)
                    callback.onResult(books, null, nextPage)

                },
                {
                    updateState(State.ERROR)
                    setRetry(Action { loadInitial(params, callback) })
                }
            ).addToCompositeDisposable(disposable)*/
        scope.launch {
            repository.searchBooks(currentPage, category.value!!, searchQuery.value).let {
                when (it) {
                    is Result.Success -> {
                        updateState(State.DONE)
                        callback.onResult(it.data, null, nextPage)
                    }
                    is Result.Error -> {
                        updateState(State.ERROR)
                        setRetry { loadInitial(params, callback) }
                    }
                }
            }
        }

    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, Book>) {
        updateState(State.LOADING)
        val currentPage = params.key
        val nextPage = currentPage + 1
        //Call api
        /*repository.searchBooks(currentPage, category.value!!, searchQuery.value)
            .subscribe(
                { books ->
                    updateState(State.DONE)
                    callback.onResult(books, nextPage)

                },
                {
                    updateState(State.ERROR)
                    setRetry(Action { loadAfter(params, callback) })
                }
            ).addToCompositeDisposable(disposable)*/

        scope.launch {
            repository.searchBooks(currentPage, category.value!!, searchQuery.value).let {
                when (it) {
                    is Result.Success -> {
                        updateState(State.DONE)
                        callback.onResult(it.data, nextPage)
                    }
                    is Result.Error -> {
                        updateState(State.ERROR)
                        setRetry { loadAfter(params, callback) }
                    }
                }
            }
        }
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, Book>) {
    }
}