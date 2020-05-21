package com.sanjay.gutenberg.pagination.datasource

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.paging.PageKeyedDataSource
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.sanjay.gutenberg.RxImmediateSchedulerRule
import com.sanjay.gutenberg.constants.State
import com.sanjay.gutenberg.data.repository.GutenbergRepository
import com.sanjay.gutenberg.data.repository.remote.model.Book
import com.sanjay.gutenberg.paging.datasource.BookListPagingDataSource

import io.reactivex.Flowable
import org.junit.*
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals

class BookListPagingDataSourceTest {

    private var pagingDataSource: BookListPagingDataSource? = null

    @Mock
    lateinit var repository: GutenbergRepository

    private var loadInitialParams = PageKeyedDataSource.LoadInitialParams<Int>(5, false)

    @Mock
    lateinit var loadInitialCallback: PageKeyedDataSource.LoadInitialCallback<Int, Book>

    private var loadParams = PageKeyedDataSource.LoadParams(1, 20)

    @Mock
    lateinit var loadCallback: PageKeyedDataSource.LoadCallback<Int, Book>

    companion object {
        @ClassRule
        @JvmField
        val schedulers = RxImmediateSchedulerRule()
    }

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    private var observerState = mock<Observer<State>>()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        pagingDataSource = BookListPagingDataSource(repository)
    }

    @After
    fun tearDown() {
        pagingDataSource = null
    }

    @Test
    fun loadInitial_Success() {
        val booksList = emptyList<Book>()
        val observable = Flowable.just(booksList)
        val category = "Fiction"
        val searchQuery = "The"
        pagingDataSource!!.category.value = category
        pagingDataSource!!.searchQuery.value = searchQuery

        whenever(repository.searchBooks(1, category, searchQuery)).thenReturn(
            observable
        )

        pagingDataSource!!.state.observeForever(observerState)

        pagingDataSource!!.loadInitial(loadInitialParams, loadInitialCallback)

        val argumentCaptor = ArgumentCaptor.forClass(State::class.java)
        val expectedLoadingState = State.LOADING
        val expectedDoneState = State.DONE

        verify(repository).searchBooks(1, category, searchQuery)
        verify(loadInitialCallback).onResult(booksList, null, 2)
        argumentCaptor.run {
            verify(observerState, times(2)).onChanged(capture())
            val (loadingState, doneState) = allValues
            assertEquals(loadingState, expectedLoadingState)
            assertEquals(doneState, expectedDoneState)
        }
    }

    @Test
    fun loadInitial_Error() {
        val errorMessage = "Error response"
        val response = Throwable(errorMessage)
        val category = "Fiction"
        val searchQuery = "The"
        pagingDataSource!!.category.value = category
        pagingDataSource!!.searchQuery.value = searchQuery

        whenever(repository.searchBooks(1, category, searchQuery)).thenReturn(
            Flowable.error(response)
        )

        pagingDataSource!!.state.observeForever(observerState)

        pagingDataSource!!.loadInitial(loadInitialParams, loadInitialCallback)

        val argumentCaptor = ArgumentCaptor.forClass(State::class.java)
        val expectedLoadingState = State.LOADING
        val expectedDoneState = State.ERROR

        verify(repository).searchBooks(1, category, searchQuery)

        argumentCaptor.run {
            verify(observerState, times(2)).onChanged(capture())
            val (loadingState, doneState) = allValues
            assertEquals(loadingState, expectedLoadingState)
            assertEquals(doneState, expectedDoneState)
        }

    }

    @Test
    fun loadInitial_Retry() {
        val errorMessage = "Error response"
        val response = Throwable(errorMessage)
        val category = "Fiction"
        val searchQuery = "The"
        pagingDataSource!!.category.value = category
        pagingDataSource!!.searchQuery.value = searchQuery

        whenever(repository.searchBooks(1, category, searchQuery)).thenReturn(
            Flowable.error(response)
        )

        pagingDataSource!!.state.observeForever(observerState)

        pagingDataSource!!.loadInitial(loadInitialParams, loadInitialCallback)

        pagingDataSource!!.retry()

        verify(repository, times(2)).searchBooks(1, category, searchQuery)
    }
}